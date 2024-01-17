package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.example.ui_list_annotation_common.Entry
import com.example.ui_list_annotation_common.Event
import com.example.ui_list_annotation_common.EventEntry
import com.example.ui_list_annotation_common.EventMap
import com.example.ui_list_annotation_common.Holder
import com.example.ui_list_annotation_common.ItemHolderFullName
import com.example.ui_list_annotation_common.JavaGenerator.Companion.CLASS_NAME
import com.example.ui_list_annotation_common.UIListHolderZoom
import com.example.ui_list_annotation_common.UiAdapterGenerator
import com.example.ui_list_annotation_common.ViewName
import com.example.ui_list_annotation_common.doubleLayerGroupBy
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSClassifierReference
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.slim_ktx.insertCode
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.trimInsertCode
import com.storyteller_f.slim_ktx.yes
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

data class Identity(val fullName: String, val name: String)

private fun BufferedWriter.writeLine(line: String = "") {
    write("$line\n")
}

class Processing(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var count: Int = 0
    private val zoom = UIListHolderZoom<KSAnnotated>()
    private val logger = environment.logger

    @Suppress("LongMethod")
    override fun process(resolver: Resolver): List<KSAnnotated> {
        count++
        val viewHolders =
            resolver.getSymbolsWithAnnotation(BindItemHolder::class.java.canonicalName)
        val clickEvents =
            resolver.getSymbolsWithAnnotation(BindClickEvent::class.java.canonicalName)
        val longClickEvents =
            resolver.getSymbolsWithAnnotation(BindLongClickEvent::class.java.canonicalName)
        val viewHolderMap = viewHolders.groupBy {
            it.validate()
        }
        val clickEventMap = clickEvents.groupBy {
            it.validate()
        }
        val longClickEventMap = longClickEvents.groupBy {
            it.validate()
        }
        val viewHolderCount = viewHolders.count()
        val clickEventCount = clickEvents.count()
        val longClickEventCount = longClickEvents.count()

        logger.warn("ui-list round $count $viewHolderCount $clickEventCount $longClickEventCount")
        logger.warn("ui-list round $count holder ${viewHolderMap[true]?.size} ${viewHolderMap[false]?.size}")
        logger.warn("ui-list round $count click: ${clickEventMap[true]?.size} ${clickEventMap[false]?.size}")
        logger.warn("ui-list round $count long ${longClickEventMap[true]?.size} ${longClickEventMap[false]?.size}")
        val invalidate =
            viewHolderMap[false].orEmpty() + clickEventMap[false].orEmpty() + longClickEventMap[false].orEmpty()
        invalidate.forEach {
            logger.warn("ui-list invalidate $it")
        }
        if (viewHolderCount == 0 && clickEventCount == 0 && longClickEventCount == 0) {
            logger.warn("ui-list round $count exit")
            return emptyList()
        }

        logger.logging("ui-list round $count ${zoom.debugState()}")
        zoom.addHolderEntry(processEntry(viewHolders).toList())
        zoom.addClickEvent(processEvent(clickEvents, isLong = false))
        zoom.addLongClick(processEvent(longClickEvents, isLong = true))

        zoom.grouped().forEach { (packageName, entries) ->
            writeFile(packageName, entries)
        }

        return emptyList()
    }

    private fun writeFile(
        packageName: String,
        entries: List<Entry<KSAnnotated>>,
    ) {
        val allItemHolderName = entries.map {
            it.itemHolderFullName
        }
        val uiListPackageName = "$packageName.ui_list"
        logger.warn("ui-list package $uiListPackageName")
        val maps = zoom.extractEventMap(allItemHolderName)
        val clickEventsMap = maps.first
        val longClickEventsMap = maps.second
        val dependencyFiles =
            getDependencies(
                entries,
                allItemHolderName,
                clickEventsMap,
                longClickEventsMap
            ).mapNotNull {
                it.containingFile
            }.distinctBy {
                it.filePath
            }
        val dependencies =
            Dependencies(aggregating = false, *dependencyFiles.toList().toTypedArray())
        val bindingClass = zoom.importHolders(entries)
        val receiverClass = zoom.importReceiverClass(clickEventsMap, longClickEventsMap)
        val composeLibrary = zoom.importComposeLibrary(entries)

        val generator = KotlinGenerator()
        BufferedWriter(
            OutputStreamWriter(
                environment.codeGenerator.createNewFile(
                    dependencies,
                    uiListPackageName,
                    CLASS_NAME
                )
            )
        ).use { writer ->
            writer.writeLine("package $uiListPackageName")
            writer.writeLine("import com.storyteller_f.ui_list.event.ViewJava")
            writer.write(composeLibrary)
            writer.write(bindingClass)
            writer.write(receiverClass)
            writer.writeLine(UiAdapterGenerator.commonImports.joinToString("\n"))
            writer.writeLine(
                """import com.storyteller_f.ui_list.core.list
                    |import com.storyteller_f.ui_list.core.registerCenter""".trimMargin()
            )
            writer.writeLine()
            writer.writeLine("object $CLASS_NAME {")
            writer.writeLine(
                buildViewHolders(
                    entries,
                    clickEventsMap,
                    longClickEventsMap
                ).prependIndent()
            )
            writer.writeLine(generator.buildAddFunction(entries).prependIndent())
            writer.writeLine("}")
            writer.writeLine()
        }
    }

    private fun <T> getDependencies(
        entries: List<Entry<T>>,
        allItemHolderName: List<ItemHolderFullName>,
        clickEventsMap: EventMap<T>,
        longClickEventsMap: EventMap<T>,
    ): List<T> {
        val extractEventOrigin: (EventEntry<T>) -> List<T> = {
            it.value.entries.flatMap { listEntry ->
                listEntry.value.map(Event<T>::origin)
            }
        }
        return (entries.filter {
            allItemHolderName.any { entry ->
                entry == it.itemHolderFullName
            }
        }.flatMap { entry ->
            entry.viewHolders.map {
                it.value.origin
            }
        } + clickEventsMap.flatMap(
            extractEventOrigin
        ) + longClickEventsMap.flatMap(
            extractEventOrigin
        ))
    }

    private fun buildViewHolders(
        entries: List<Entry<KSAnnotated>>,
        clickEventsMap: Map<ItemHolderFullName, Map<ViewName, List<Event<KSAnnotated>>>>,
        longClickEventsMap: Map<ItemHolderFullName, Map<ViewName, List<Event<KSAnnotated>>>>,
    ): String {
        return entries.joinToString("\n\n") { entry ->
            createMultiViewHolder(
                entry,
                clickEventsMap[entry.itemHolderFullName].orEmpty(),
                longClickEventsMap[entry.itemHolderFullName].orEmpty()
            )
        }
    }

    private fun createMultiViewHolder(
        entry: Entry<KSAnnotated>,
        eventMapClick: Map<ViewName, List<Event<KSAnnotated>>>,
        eventMapLongClick: Map<ViewName, List<Event<KSAnnotated>>>,
    ): String {
        val viewHolderBuilderContent = entry.viewHolders.map {
            val viewHolderContent = if (it.value.bindingName.endsWith(
                    "Binding"
                )
            ) {
                buildViewHolder(it.value, eventMapClick, eventMapLongClick)
            } else {
                buildComposeViewHolder(it.value, eventMapClick, eventMapLongClick)
            }
            """
                    if (type.equals("${it.key}")) {
                        $1
                    }//type if end
            """.trimIndent().insertCode(viewHolderContent.yes())
        }.joinToString("\n")
        return """
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                fun buildFor${entry.itemHolderName}(view: ViewGroup, type: String) : AbstractViewHolder<*> {
                    $1
                    throw Exception("unrecognized type:[${'$'}type]")
                }
        """.trimIndent().insertCode(viewHolderBuilderContent.yes())
    }

    private fun buildComposeViewHolder(
        it: Holder<KSAnnotated>,
        eventList: Map<String, List<Event<KSAnnotated>>>,
        eventList2: Map<String, List<Event<KSAnnotated>>>,
    ): String {
        return """
            val context = view.context
            val composeView = EDComposeView(context)
            val viewHolder = ${it.viewHolderName}(composeView)
            @Suppress("UNUSED_VARIABLE") val v = viewHolder.itemView
            composeView.clickListener = { s ->
                $1
            }
            composeView.longClickListener = { s ->
                $2
            }
            return viewHolder
            """.trimInsertCode(
            buildComposeClickListener(eventList).yes(2),
            buildComposeClickListener(eventList2).yes(2)
        )
    }

    private fun buildComposeClickListener(event: Map<String, List<Event<KSAnnotated>>>) =
        event.map {
            val clickBlock = it.value.joinToString("\n") { e ->
                produceClickBlockForCompose(e)
            }
            """
            if (s == "${it.key}") {
                $1                
            }//if end
        """.trimInsertCode(clickBlock.yes())
        }.joinToString("\n")

    private fun produceClickBlockForCompose(e: Event<KSAnnotated>): String {
        val parameterList = e.parameterList
        return if (e.receiver.contains("Activity")) {
            """
            if("${e.group}".equals(viewHolder.grouped)) 
                ViewJava.doWhenIs(context, ${e.receiver}::class.java, { activity ->
                    activity.${e.functionName}($parameterList);
                    null;//activity return
                });//activity end
            """.trimIndent()
        } else {
            """
            if("${e.group}".equals(viewHolder.grouped)) 
                ViewJava.findActionReceiverOrNull(composeView.getComposeView(), ${e.receiver}::class.java, { fragment ->
                    fragment.${e.functionName}($parameterList);
                    null;//fragment return
                });//fragment end
            """.trimIndent()
        }
    }

    private fun buildViewHolder(
        entry: Holder<KSAnnotated>,
        eventMapClick: Map<String, List<Event<KSAnnotated>>>,
        eventMapLongClick: Map<String, List<Event<KSAnnotated>>>,
    ): String {
        return """
            val context = view.context
            val inflate = ${entry.bindingName}.inflate(LayoutInflater.from(context), view, false)
            
            val viewHolder = ${entry.viewHolderName}(inflate)
            $1       
            return viewHolder
            """.trimInsertCode(buildInvokeClickEvent(eventMapClick, eventMapLongClick).no())
    }

    private fun buildInvokeClickEvent(
        event: Map<String, List<Event<KSAnnotated>>>,
        event2: Map<String, List<Event<KSAnnotated>>>,
    ): String {
        val singleClickListener = event.map(::produceClickListener).joinToString("\n")
        val longClickListener = event2.map(::produceLongClickListener).joinToString("\n")
        return singleClickListener + longClickListener
    }

    private fun produceClickListener(it: Map.Entry<String, List<Event<KSAnnotated>>>) = """
            inflate.${it.key}.setOnClickListener { v ->
                $1
            }
        """.trimInsertCode(buildInvokeClickEvent(it.value).yes())

    private fun produceLongClickListener(it: Map.Entry<String, List<Event<KSAnnotated>>>) = """
            inflate.${it.key}.setOnLongClickListener { v ->
                $1
                return true;
            }
        """.trimInsertCode(buildInvokeClickEvent(it.value).yes())

    private fun buildInvokeClickEvent(events: List<Event<KSAnnotated>>): String {
        return events.joinToString("\n") { event ->
            val parameterList = event.parameterList
            if (event.receiver.contains("Activity")) {
                """
                    if("${event.group}" == viewHolder.grouped) 
                        ViewJava.doWhenIs(context, ${event.receiver}::class.java, { activity ->
                            activity.${event.functionName}($parameterList);
                            null;
                        });
                """.trimIndent()
            } else {
                """
                    if("${event.group}" == viewHolder.grouped)
                        ViewJava.findActionReceiverOrNull(v, ${event.receiver}::class.java, { fragment ->
                            fragment.${event.functionName}($parameterList);
                            null;
                        });
                """.trimIndent()
            }
        }
    }

    @OptIn(KspExperimental::class)
    private fun processEvent(
        clickEvents: Sequence<KSAnnotated>,
        isLong: Boolean,
    ): Map<String, Map<String, List<Event<KSAnnotated>>>> {
        return clickEvents.doubleLayerGroupBy({
            val (itemHolderFullName, _) = getItemHolderDetail(it)
            val viewName = if (isLong) {
                it.getAnnotationsByType(
                    BindLongClickEvent::class
                ).first().viewName
            } else {
                it.getAnnotationsByType(BindClickEvent::class).first().viewName
            }
            itemHolderFullName to viewName
        }) {
            it as KSFunctionDeclaration
            val key = if (isLong) {
                it.getAnnotationsByType(
                    BindLongClickEvent::class
                ).first().group
            } else {
                it.getAnnotationsByType(BindClickEvent::class).first().group
            }
            val parent = it.parent as KSClassDeclaration
            val r = parent.identity()
            val parameterList = it.parameters.joinToString(", ") { parameter ->
                val asString = parameter.name?.asString()
                if (asString.isNullOrEmpty()) {
                    ""
                } else if (asString == "itemHolder") {
                    "viewHolder.itemHolder"
                } else if (asString == "binding") {
                    "inflate"
                } else {
                    "v"
                }
            }
            Event(
                r.name,
                r.fullName,
                it.simpleName.asString(),
                parameterList,
                key,
                it as KSAnnotated
            )
        }
    }

    private fun KSDeclaration.identity(): Identity {
        val name = simpleName.asString()
        return Identity("${packageName.asString()}.$name", name)
    }

    /**
     * 处理viewHolder
     */
    @OptIn(KspExperimental::class)
    private fun processEntry(viewHolders: Sequence<KSAnnotated>): Sequence<Entry<KSAnnotated>> {
        return viewHolders.map { viewHolder ->
            viewHolder as KSClassDeclaration
            val type = viewHolder.getAnnotationsByType(BindItemHolder::class).first().type
            val (itemHolderFullName, itemHolderName) = getItemHolderDetail(viewHolder)
            val (bindingName, bindingFullName) = getBindingDetail(viewHolder)
            val (viewHolderFullName, viewHolderName) = viewHolder.identity()
            Entry(
                itemHolderName,
                itemHolderFullName,
                mutableMapOf(
                    type to Holder(
                        bindingName,
                        bindingFullName,
                        viewHolderName,
                        viewHolderFullName,
                        viewHolder as KSAnnotated
                    )
                ),
            )
        }
    }

    private fun getItemHolderDetail(viewHolder: KSAnnotated): Pair<String, String> {
        val ksType = viewHolder.annotations.first().arguments.first().value as KSType
        val declaration = ksType.declaration as KSClassDeclaration
        assert(
            declaration.isAnnotationPresentOrParent(ItemHolder::class)
        ) { "ItemHolder[${declaration.qualifiedName?.asString()}] 需要添加@ItemHolder 注解" }
        return declaration.qualifiedName.let { it!!.asString() to it.getShortName() }
    }

    @OptIn(KspExperimental::class)
    private fun KSClassDeclaration.isAnnotationPresentOrParent(kClass: KClass<out Annotation>): Boolean {
        val isPresent = isAnnotationPresent(kClass)
        return if (isPresent) {
            true
        } else {
            superTypes.any {
                val resolve = it.resolve()
                resolve.declaration.isAnnotationPresent(kClass)
            }
        }
    }

    private fun getBindingDetail(viewHolder: KSClassDeclaration): Pair<String, String> {
        val firstProperties = viewHolder.getAllProperties().first()
        val propertyName = firstProperties.simpleName.getShortName()
        return if (propertyName == "binding") {
            val bindingPackageName = firstProperties.packageName.asString()
            val bindingName =
                (firstProperties.type.element as KSClassifierReference).referencedName()
            val bindingFullName = "$bindingPackageName.databinding.$bindingName"
            Pair(bindingName, bindingFullName)
        } else {
            val asString = firstProperties.type.resolve().declaration.qualifiedName
            val bindingName = asString?.getShortName() ?: ""
            val bindingFullName = asString?.asString() ?: ""
            Pair(bindingName, bindingFullName)
        }
    }
}

class ProcessingProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processing(environment)
    }
}

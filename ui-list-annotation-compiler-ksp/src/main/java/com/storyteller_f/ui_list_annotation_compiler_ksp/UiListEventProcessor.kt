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
import com.example.ui_list_annotation_common.nestedGroupBy
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.annotation_defination.ItemHolder
import com.storyteller_f.slim_ktx.dup
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.replaceCode
import com.storyteller_f.slim_ktx.trimAndReplaceCode
import com.storyteller_f.slim_ktx.yes
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

data class Identity(val fullName: String, val name: String)

private fun BufferedWriter.writeLine(line: String = "") {
    write("$line\n")
}

class UiListEventProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
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

        logger.logging("ui-list round $count $viewHolderCount $clickEventCount $longClickEventCount")
        logger.logging("ui-list round $count holder ${viewHolderMap[true]?.size} ${viewHolderMap[false]?.size}")
        logger.logging("ui-list round $count click: ${clickEventMap[true]?.size} ${clickEventMap[false]?.size}")
        logger.logging("ui-list round $count long ${longClickEventMap[true]?.size} ${longClickEventMap[false]?.size}")
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
        logger.info("ui-list package $uiListPackageName")
        val (clickEventsMap, longClickEventsMap) = zoom.extractEventMap(allItemHolderName)
        val dependencyFiles =
            getDependencyFiles(entries, allItemHolderName, clickEventsMap, longClickEventsMap)

        val allImports = allImports(entries, clickEventsMap, longClickEventsMap)
        val affectInfo = getAffectFileInfo(uiListPackageName, dependencyFiles)
        val generator = KotlinGenerator()
        writer(uiListPackageName, dependencyFiles).use { writer ->
            writer.writeLine("package $uiListPackageName\n")
            writer.writeLine(affectInfo)
            writer.writeLine()
            writer.writeLine(
                allImports.joinToString("\n") {
                    "import $it"
                }
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
            writer.writeLine("}\n")
        }
    }

    private fun getAffectFileInfo(
        uiListPackageName: String,
        dependencyFiles: List<KSFile>
    ): String {
        val coverPart = getAllFilePathCoverPart(dependencyFiles)

        val distinctName = dependencyFiles.map {
            it.filePath.substring(coverPart.length)
        }
        val info = distinctName.joinToString("\n\t")
        logger.warn(
            "$uiListPackageName has changed, because processor changed or this file changed $coverPart\n\t$info"
        )
        val affectInfo = buildString {
            appendLine("//scope: $coverPart")
            distinctName.forEachIndexed { index, ksFile ->
                appendLine("//file $index: $ksFile")
            }
        }
        return affectInfo
    }

    private fun getAllFilePathCoverPart(
        dependencyFiles: List<KSFile>,
    ): String {
        return dependencyFiles.map {
            it.filePath
        }.coverPart()
    }

    private fun allImports(
        entries: List<Entry<KSAnnotated>>,
        clickEventsMap: EventMap<KSAnnotated>,
        longClickEventsMap: EventMap<KSAnnotated>
    ): List<String> {
        val bindingClass = zoom.importHolders(entries)
        val receiverClass = zoom.importReceiverClass(clickEventsMap, longClickEventsMap)

        return (bindingClass + receiverClass + UiAdapterGenerator.commonImports).distinct()
    }

    private fun writer(
        uiListPackageName: String,
        dependencyFiles: List<KSFile>
    ): BufferedWriter {
        val dependencies =
            Dependencies(aggregating = false, *dependencyFiles.toTypedArray())
        return BufferedWriter(
            OutputStreamWriter(
                environment.codeGenerator.createNewFile(
                    dependencies,
                    uiListPackageName,
                    CLASS_NAME
                )
            )
        )
    }

    private fun UiListEventProcessor.getDependencyFiles(
        entries: List<Entry<KSAnnotated>>,
        allItemHolderName: List<ItemHolderFullName>,
        clickEventsMap: EventMap<KSAnnotated>,
        longClickEventsMap: EventMap<KSAnnotated>
    ): List<KSFile> {
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
        return dependencyFiles
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
            """.trimIndent().replaceCode(viewHolderContent.yes())
        }.joinToString("\n")
        return """
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                fun buildFor${entry.itemHolderName}(view: ViewGroup, type: String): AbstractViewHolder<*> {
                    $1
                    throw Exception("unrecognized type:[${'$'}type]")
                }
                
        """.trimIndent().replaceCode(viewHolderBuilderContent.yes())
    }

    private fun buildComposeViewHolder(
        it: Holder<KSAnnotated>,
        eventList: Map<ViewName, List<Event<KSAnnotated>>>,
        eventList2: Map<ViewName, List<Event<KSAnnotated>>>,
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
            """.trimAndReplaceCode(
            buildComposeClickListener(eventList).yes(),
            buildComposeClickListener(eventList2).yes()
        )
    }

    private fun buildComposeClickListener(event: Map<ViewName, List<Event<KSAnnotated>>>) =
        event.map {
            if (it.value.map { event1 ->
                    event1.group
                }.dup()) {
                throw Exception(
                    "dup group ${
                        it.value.map { event1 ->
                            "${event1.receiver}#${event1.functionName} ${event1.group}"
                        }
                    }"
                )
            }
            val clickBlock = it.value.joinToString("\n") { e ->
                produceClickBlockForCompose(e)
            }
            """
            if (s == "${it.key}") {
                $1                
            }//if end
        """.trimAndReplaceCode(clickBlock.yes())
        }.joinToString("\n")

    private fun produceClickBlockForCompose(e: Event<KSAnnotated>): String {
        val parameterList = e.parameterList
        return if (e.receiver.contains("Activity")) {
            """
            if("${e.group}".equals(viewHolder.grouped)) 
                (context as? ${e.receiver})?.${e.functionName}($parameterList)
            """.trimIndent()
        } else {
            """
            if("${e.group}".equals(viewHolder.grouped)) 
                v.findFragmentOrNull<${e.receiver}>()?.${e.functionName}($parameterList)
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
            """.trimAndReplaceCode(buildInvokeClickEvent(eventMapClick, eventMapLongClick).no())
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
        """.trimAndReplaceCode(buildInvokeClickEvent(it.value).yes())

    private fun produceLongClickListener(it: Map.Entry<String, List<Event<KSAnnotated>>>) = """
            inflate.${it.key}.setOnLongClickListener { v ->
                $1
                return true;
            }
        """.trimAndReplaceCode(buildInvokeClickEvent(it.value).yes())

    private fun buildInvokeClickEvent(events: List<Event<KSAnnotated>>): String {
        return events.joinToString("\n") { event ->
            val parameterList = event.parameterList
            if (event.receiver.contains("Activity")) {
                """
                    if("${event.group}" == viewHolder.grouped) 
                        (context as? ${event.receiver})?.${event.functionName}($parameterList)
                """.trimIndent()
            } else {
                """
                    if("${event.group}" == viewHolder.grouped)
                        v.findFragmentOrNull<${event.receiver}>()?.${event.functionName}($parameterList)
                """.trimIndent()
            }
        }
    }

    @OptIn(KspExperimental::class)
    private fun processEvent(
        clickEvents: Sequence<KSAnnotated>,
        isLong: Boolean,
    ): Map<String, Map<String, List<Event<KSAnnotated>>>> {
        return clickEvents.nestedGroupBy({
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
            val group = if (isLong) {
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
                group,
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
        val isDataClass = declaration.modifiers.contains(Modifier.DATA)
        val hasOverride = declaration.getDeclaredFunctions().any {
            val asString = it.simpleName.asString()
            asString == "equals" && it.findOverridee() != null || asString == "areContentsTheSame"
        }
        assert(isDataClass || hasOverride) {
            "ItemHolder[${declaration.qualifiedName?.asString()}] 需要添加data 修饰符或者重载equals/areContentsTheSame"
        }
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
        val firstProperty = viewHolder.getAllProperties().first()
        val ksName = firstProperty.type.resolve().declaration.qualifiedName
        val bindingName = ksName?.getShortName() ?: ""
        val bindingFullName = ksName?.asString() ?: ""
        return Pair(bindingName, bindingFullName)
    }
}

private fun List<String>.coverPart(): String {
    val c = minOfOrNull {
        it.length
    } ?: return ""
    val baseString = get(0)
    for (i in 0 until c) {
        val base = baseString[i]
        if (any {
                it[i] != base
            }) {
            return baseString.substring(0, i)
        }
    }
    return baseString.substring(0, c)
}

class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return UiListEventProcessor(environment)
    }
}

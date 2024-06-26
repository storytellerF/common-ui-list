package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.replaceCode
import com.storyteller_f.slim_ktx.trimAndReplaceCode
import com.storyteller_f.slim_ktx.yes
import com.storyteller_f.ui_list_annotation_common.Entry
import com.storyteller_f.ui_list_annotation_common.Event
import com.storyteller_f.ui_list_annotation_common.EventEntry
import com.storyteller_f.ui_list_annotation_common.EventMap
import com.storyteller_f.ui_list_annotation_common.Holder
import com.storyteller_f.ui_list_annotation_common.ItemHolderFullName
import com.storyteller_f.ui_list_annotation_common.UIListHolderZoom
import com.storyteller_f.ui_list_annotation_common.UiAdapterGenerator
import com.storyteller_f.ui_list_annotation_common.ViewName
import java.io.BufferedWriter

class KotlinGenerator(
    private val entries: List<Entry<KSAnnotated>>,
    private val zoom: UIListHolderZoom<KSAnnotated>,
    private val logger: KSPLogger,
    private val packageName: String,
    private val writer: (List<KSFile>) -> BufferedWriter
) : UiAdapterGenerator<KSAnnotated>() {
    private val allItemHolderName = entries.map {
        it.itemHolderFullName
    }
    private val mapPair = zoom.extractEventMap(allItemHolderName)
    private val dependencyFiles =
        getDependencyFiles(entries, allItemHolderName, mapPair.first, mapPair.second)
    private val allImports = allImports(entries, mapPair.first, mapPair.second)
    private val affectInfo = getAffectFileInfo(packageName, dependencyFiles)
    override fun buildAddFunction(entry: List<Entry<KSAnnotated>>): String {
        val addFunctions = entry.joinToString("\n") {
            buildRegisterBlock(it)
        }
        return """
            fun registerAll(map: MutableMap<KClass<out DataItemHolder>, BuildBatch>) {
                $1
            }
            """.trimAndReplaceCode(addFunctions.yes())
    }

    private fun buildRegisterBlock(it: Entry<KSAnnotated>) = """
                register${it.itemHolderName}(map)
    """.trimIndent()

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

        return (bindingClass + receiverClass + commonImports).distinct()
    }

    private fun getDependencyFiles(
        entries: List<Entry<KSAnnotated>>,
        allItemHolderName: List<ItemHolderFullName>,
        clickEventsMap: EventMap<KSAnnotated>,
        longClickEventsMap: EventMap<KSAnnotated>
    ): List<KSFile> {
        val dependencyFiles = getDependencies(
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

    fun write() {
        writer(dependencyFiles).use { writer ->
            writer.writeLine("package $packageName\n")
            writer.writeLine(affectInfo)
            writer.writeLine()
            writer.writeLine(allImports.joinToString("\n") {
                "import $it"
            })
            writer.writeLine()
            writer.writeLine(
                buildViewHolders(
                    entries,
                    mapPair.first,
                    mapPair.second
                )
            )
            // registerAll 为了能够使用:: 语法必须放到一个类中
            writer.writeLine("object $CLASS_NAME {")
            writer.writeLine(buildAddFunction(entries).prependIndent())
            writer.writeLine("}\n")
        }
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
        clickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
        longClickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
    ): String {
        val viewHolderBuilderContent = buildViewHolderContent(entry, clickEventMap, longClickEventMap)
        val itemHolderExtraParameter = if (entry.viewHolders.any {
                it.value.constructorExtraParams.isNotEmpty()
            }) {
            ", key: String"
        } else {
            ""
        }
        return """
                @Suppress("UNUSED_ANONYMOUS_PARAMETER")
                fun build${entry.itemHolderName}(parent: ViewGroup, type: String$itemHolderExtraParameter): AbstractViewHolder<*> {
                    $1
                    throw Exception("unrecognized type:[${'$'}type]")
                }
                
                fun register${entry.itemHolderName}(map: MutableMap<KClass<out DataItemHolder>, BuildBatch>) {
                    map.put(${entry.itemHolderName}::class, BuildBatch(${if (itemHolderExtraParameter.isEmpty()) "b2 =" else "b3 ="} ::build${entry.itemHolderName}));
                }
                
        """.trimIndent().replaceCode(viewHolderBuilderContent.yes())
    }

    private fun buildViewHolderContent(
        entry: Entry<KSAnnotated>,
        clickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
        longClickEventMap: Map<ViewName, List<Event<KSAnnotated>>>
    ): String {
        val viewHolderBuilderContent = entry.viewHolders.map { viewHolder ->
            if ((clickEventMap + longClickEventMap).filter {
                    it.value.size > 1
                }.onEach {
                    logger.error("${entry.itemHolderFullName}针对${it.key} 设置了多个点击事件")
                }.isNotEmpty()) {
                throw Exception("具体错误查看日志")
            }
            val viewHolderContent = if (viewHolder.value.bindingName.endsWith(
                    "Binding"
                )
            ) {
                buildViewHolder(viewHolder.value, clickEventMap, longClickEventMap)
            } else {
                buildComposeViewHolder(viewHolder.value, clickEventMap, longClickEventMap)
            }
            """
                if (type.equals("${viewHolder.key}")) {
                    $1
                }//type if end
            """.trimIndent().replaceCode(viewHolderContent.yes())
        }.joinToString("\n")
        return viewHolderBuilderContent
    }

    private fun buildComposeViewHolder(
        it: Holder<KSAnnotated>,
        clickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
        longClickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
    ): String {
        return """
            val context = parent.context
            val view = EDComposeView(context)
            val viewHolder = ${it.viewHolderName}(view)
            @Suppress("UNUSED_VARIABLE") val v = viewHolder.itemView
            view.clickListener = { s ->
                $1
            }
            view.longClickListener = { s ->
                $2
            }
            return viewHolder
            """.trimAndReplaceCode(
            buildComposeClickListener(clickEventMap).yes(),
            buildComposeClickListener(longClickEventMap).yes()
        )
    }

    private fun buildComposeClickListener(event: Map<ViewName, List<Event<KSAnnotated>>>) =
        event.map {
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
                (context as? ${e.receiver})?.${e.functionName}($parameterList)
            """.trimIndent()
        } else {
            """
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
            val context = parent.context
            val binding = ${entry.bindingName}.inflate(LayoutInflater.from(context), parent, false)
            
            val viewHolder = ${entry.viewHolderName}(binding${entry.constructorExtraParams})
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
            binding.${it.key}.setOnClickListener { v ->
                $1
            }
        """.trimAndReplaceCode(buildInvokeClickEvent(it.value).yes())

    private fun produceLongClickListener(it: Map.Entry<String, List<Event<KSAnnotated>>>) = """
            binding.${it.key}.setOnLongClickListener { v ->
                $1
                return true;
            }
        """.trimAndReplaceCode(buildInvokeClickEvent(it.value).yes())

    private fun buildInvokeClickEvent(events: List<Event<KSAnnotated>>): String {
        return events.joinToString("\n") { event ->
            val parameterList = event.parameterList
            if (event.receiver.contains("Activity")) {
                """
                    (context as? ${event.receiver})?.${event.functionName}($parameterList)
                """.trimIndent()
            } else {
                """
                    v.findFragmentOrNull<${event.receiver}>()?.${event.functionName}($parameterList)
                """.trimIndent()
            }
        }
    }
}

fun List<String>.coverPart(): String {
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

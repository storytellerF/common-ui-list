package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.example.ui_list_annotation_common.Entry
import com.example.ui_list_annotation_common.Event
import com.example.ui_list_annotation_common.EventEntry
import com.example.ui_list_annotation_common.EventMap
import com.example.ui_list_annotation_common.Holder
import com.example.ui_list_annotation_common.ItemHolderFullName
import com.example.ui_list_annotation_common.JavaGenerator
import com.example.ui_list_annotation_common.UIListHolderZoom
import com.example.ui_list_annotation_common.UiAdapterGenerator
import com.example.ui_list_annotation_common.ViewName
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.storyteller_f.slim_ktx.dup
import com.storyteller_f.slim_ktx.no
import com.storyteller_f.slim_ktx.replaceCode
import com.storyteller_f.slim_ktx.trimAndReplaceCode
import com.storyteller_f.slim_ktx.yes
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
    private val t = zoom.extractEventMap(allItemHolderName)
    private val dependencyFiles = getDependencyFiles(entries, allItemHolderName, t.first, t.second)
    private val allImports = allImports(entries, t.first, t.second)
    private val affectInfo = getAffectFileInfo(packageName, dependencyFiles)
    override fun buildAddFunction(entry: List<Entry<KSAnnotated>>): String {
        var index = 0
        val addFunctions = entry.joinToString("\n") {
            buildRegisterBlock(it, index++)
        }
        return """
            fun add(offset: Int): Int {
                $1
                return $index;
            }
            """.trimAndReplaceCode(addFunctions.yes())
    }

    private fun buildRegisterBlock(it: Entry<KSAnnotated>, index: Int) = """
                registerCenter.put(${it.itemHolderName}::class.java, $index + offset);
                list.add(::buildFor${it.itemHolderName});
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
            writer.writeLine("object ${JavaGenerator.CLASS_NAME} {")
            writer.writeLine(
                buildViewHolders(
                    entries,
                    t.first,
                    t.second
                ).prependIndent()
            )
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
        val viewHolderBuilderContent = entry.viewHolders.map {
            val viewHolderContent = if (it.value.bindingName.endsWith(
                    "Binding"
                )
            ) {
                buildViewHolder(it.value, clickEventMap, longClickEventMap)
            } else {
                buildComposeViewHolder(it.value, clickEventMap, longClickEventMap)
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
        clickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
        longClickEventMap: Map<ViewName, List<Event<KSAnnotated>>>,
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
            buildComposeClickListener(clickEventMap).yes(),
            buildComposeClickListener(longClickEventMap).yes()
        )
    }

    private fun buildComposeClickListener(event: Map<ViewName, List<Event<KSAnnotated>>>) =
        event.map {
            if (it.value.map { event1 ->
                    event1.group
                }.dup()) {
                throw Exception("dup group ${
                    it.value.map { event1 ->
                        "${event1.receiver}#${event1.functionName} ${event1.group}"
                    }
                }")
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

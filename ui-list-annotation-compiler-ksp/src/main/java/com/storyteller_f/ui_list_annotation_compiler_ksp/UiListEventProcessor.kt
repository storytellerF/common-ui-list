package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.google.devtools.ksp.KspExperimental
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
import com.google.devtools.ksp.symbol.KSClassifierReference
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
import com.storyteller_f.ui_list_annotation_common.Entry
import com.storyteller_f.ui_list_annotation_common.Event
import com.storyteller_f.ui_list_annotation_common.Holder
import com.storyteller_f.ui_list_annotation_common.JavaGenerator.Companion.CLASS_NAME
import com.storyteller_f.ui_list_annotation_common.UIListHolderZoom
import com.storyteller_f.ui_list_annotation_common.nestedGroupBy
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

data class Identity(val fullName: String, val name: String)

fun BufferedWriter.writeLine(line: String = "") {
    write("$line\n")
}

class UiListEventProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var count: Int = 0
    private val zoom = UIListHolderZoom<KSAnnotated>()
    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        count++
        val viewHolders =
            resolver.getSymbolsWithAnnotation(BindItemHolder::class.java.canonicalName)
        val clickEvents =
            resolver.getSymbolsWithAnnotation(BindClickEvent::class.java.canonicalName)
        val longClickEvents =
            resolver.getSymbolsWithAnnotation(BindLongClickEvent::class.java.canonicalName)

        if (checkRound(viewHolders, clickEvents, longClickEvents)) return emptyList()

        process(viewHolders, clickEvents, longClickEvents)

        return emptyList()
    }

    private fun process(
        viewHolders: Sequence<KSAnnotated>,
        clickEvents: Sequence<KSAnnotated>,
        longClickEvents: Sequence<KSAnnotated>
    ) {
        logger.logging("ui-list round $count ${zoom.debugState()}")
        zoom.addHolderEntry(processEntry(viewHolders).toList())
        zoom.addClickEvent(processEvent(clickEvents, isLong = false))
        zoom.addLongClick(processEvent(longClickEvents, isLong = true))

        zoom.grouped().forEach { (packageName, entries) ->
            writeFile(packageName, entries)
        }
    }

    private fun checkRound(
        viewHolders: Sequence<KSAnnotated>,
        clickEvents: Sequence<KSAnnotated>,
        longClickEvents: Sequence<KSAnnotated>
    ): Boolean {
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

        logger.warn("ui-list round $count vh:$viewHolderCount click:$clickEventCount long-click:$longClickEventCount")
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
            return true
        }
        return false
    }

    private fun writeFile(
        packageName: String,
        entries: List<Entry<KSAnnotated>>,
    ) {
        val uiListPackageName = "$packageName.ui_list"
        logger.info("ui-list package $uiListPackageName")

        KotlinGenerator(entries, zoom, logger, uiListPackageName) {
            writer(uiListPackageName, it)
        }.write()
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
            val parent = it.parent as KSClassDeclaration
            val identity = parent.identity()
            val parameterList = argumentList(it)
            Event(
                identity.name,
                identity.fullName,
                it.simpleName.asString(),
                parameterList,
                it
            )
        }
    }

    private fun argumentList(it: KSFunctionDeclaration): String {
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
        return parameterList
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
        val firstProperty = viewHolder.getAllProperties().first {
            val shortName = it.simpleName.getShortName()
            shortName == "binding" || shortName == "edComposeView"
        }
        val ksName = firstProperty.type.resolve().declaration.qualifiedName
        if (ksName != null) {
            val bindingName = ksName.getShortName()
            val bindingFullName = ksName.asString()
            return Pair(bindingName, bindingFullName)
        } else {
            val propertyName = firstProperty.simpleName.getShortName()
            return if (propertyName == "binding") {
                val bindingPackageName = firstProperty.packageName.asString()
                val bindingName =
                    (firstProperty.type.element as KSClassifierReference).referencedName()
                val bindingFullName = "$bindingPackageName.databinding.$bindingName"
                Pair(bindingName, bindingFullName)
            } else {
                val asString = firstProperty.type.resolve().declaration.qualifiedName
                val bindingName = asString?.getShortName() ?: ""
                val bindingFullName = asString?.asString() ?: ""
                Pair(bindingName, bindingFullName)
            }
        }
    }
}

class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return UiListEventProcessor(environment)
    }
}

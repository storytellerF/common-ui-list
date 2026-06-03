package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
class UiListEventProcessorTest {
    @Test
    fun `generates builders for view binding and compose holders`() {
        val result = compile(
            ProcessorProvider(),
            *uiListRuntimeStubs,
            SourceFile.kotlin(
                "sample/Sample.kt",
                """
                package sample

                import android.view.LayoutInflater
                import android.view.View
                import android.view.ViewGroup
                import com.storyteller_f.annotation_defination.BindClickEvent
                import com.storyteller_f.annotation_defination.BindItemHolder
                import com.storyteller_f.annotation_defination.BindLongClickEvent
                import com.storyteller_f.annotation_defination.ItemHolder
                import com.storyteller_f.ui_list.core.AbstractViewHolder
                import com.storyteller_f.ui_list.core.BindingViewHolder
                import com.storyteller_f.ui_list.core.DataItemHolder
                import com.storyteller_f.view_holder_compose.ComposeViewHolder
                import com.storyteller_f.view_holder_compose.EDComposeView

                @ItemHolder("repo")
                data class RepoItemHolder(val name: String) : DataItemHolder()

                class RepoViewItemBinding(val root: View) {
                    companion object {
                        fun inflate(inflater: LayoutInflater, parent: ViewGroup, attachToParent: Boolean) =
                            RepoViewItemBinding(View(parent.context))
                    }
                }

                @BindItemHolder(RepoItemHolder::class)
                class RepoViewHolder(private val binding: RepoViewItemBinding, key: String) :
                    BindingViewHolder<RepoItemHolder>(binding, key)

                @ItemHolder("separator")
                data class SeparatorItemHolder(val title: String) : DataItemHolder()

                @BindItemHolder(SeparatorItemHolder::class)
                class SeparatorViewHolder(edComposeView: EDComposeView) :
                    ComposeViewHolder<SeparatorItemHolder>(edComposeView)

                class ClickReceiver {
                    @BindClickEvent(RepoItemHolder::class)
                    fun clickRepo(itemHolder: RepoItemHolder) = Unit

                    @BindClickEvent(SeparatorItemHolder::class, "card")
                    fun clickSeparator(view: View, itemHolder: SeparatorItemHolder) = Unit

                    @BindLongClickEvent(SeparatorItemHolder::class, "card")
                    fun longClickSeparator(binding: Any, itemHolder: SeparatorItemHolder) = Unit
                }
                """.trimIndent()
            )
        )

        val generated = result.sourcesGeneratedBySymbolProcessor
            .filter { it.name.endsWith("Builder.kt") }
            .sortedBy { it.name }
            .joinToString("\n\n// --- file ---\n\n") { it.readText() }
            .normalizeGenerated()

        assertGolden("builders.kt", generated)
    }

    private fun compile(
        provider: SymbolProcessorProvider,
        vararg sources: SourceFile,
    ) = KotlinCompilation().apply {
        inheritClassPath = true
        configureKsp {
            symbolProcessorProviders.add(provider)
        }
        kspWithCompilation = false
        this.sources = sources.toList()
        jvmTarget = "21"
    }.compile()

    private fun assertGolden(name: String, actual: String) {
        val expected = javaClass.classLoader
            .getResource("golden/ui-list-annotation-compiler-ksp/$name")!!
            .readText()
            .normalizeGenerated()
        assertEquals(expected, actual)
    }

    private fun String.normalizeGenerated(): String =
        replace(Regex("/tmp/Kotlin-Compilation[^/]+/sources/"), "<sources>/")
            .replace("\r\n", "\n")
            .trim()
}

private val uiListRuntimeStubs = arrayOf(
    SourceFile.kotlin(
        "android/content/Context.kt",
        """
        package android.content
        open class Context
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "android/view/View.kt",
        """
        package android.view
        import android.content.Context
        open class View(val context: Context)
        open class ViewGroup(context: Context = Context()) : View(context)
        class LayoutInflater {
            companion object {
                fun from(context: Context): LayoutInflater = LayoutInflater()
            }
        }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "com/storyteller_f/ui_list/core/Core.kt",
        """
        package com.storyteller_f.ui_list.core
        import android.view.View
        open class DataItemHolder
        abstract class AbstractViewHolder<IH : DataItemHolder>(val itemView: View) {
            lateinit var itemHolder: IH
        }
        open class BindingViewHolder<IH : DataItemHolder>(binding: Any, key: String = "") :
            AbstractViewHolder<IH>((binding as sample.RepoViewItemBinding).root)
        class BuildBatch(
            val b2: ((android.view.ViewGroup, String) -> AbstractViewHolder<*>)? = null,
            val b3: ((android.view.ViewGroup, String, String) -> AbstractViewHolder<*>)? = null,
        )
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "com/storyteller_f/ui_list/event/View.kt",
        """
        package com.storyteller_f.ui_list.event
        fun Any.findFragmentOrNull(): Any? = null
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "com/storyteller_f/view_holder_compose/Compose.kt",
        """
        package com.storyteller_f.view_holder_compose
        import android.content.Context
        import android.view.View
        import com.storyteller_f.ui_list.core.AbstractViewHolder
        import com.storyteller_f.ui_list.core.DataItemHolder
        class EDComposeView(context: Context) {
            val composeView: View = View(context)
            var clickListener: ((String) -> Unit)? = null
            var longClickListener: ((String) -> Unit)? = null
        }
        open class ComposeViewHolder<IH : DataItemHolder>(val edComposeView: EDComposeView) :
            AbstractViewHolder<IH>(edComposeView.composeView)
        """.trimIndent()
    ),
)

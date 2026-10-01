package com.storyteller_f.ui_list_annotation_compiler_ksp

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class UiListEventProcessorTest {
    @Test
    fun `generates builders for view binding and compose holders`() {
        val result = compile(
            ProcessorProvider(),
            *uiListRuntimeStubs,
            sampleSource,
            callbackProbeSource
        )

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = result.sourcesGeneratedBySymbolProcessor
            .filter { it.name.endsWith("Builder.kt") }
            .sortedBy { it.name }
            .joinToString("\n\n// --- file ---\n\n") { it.readText() }
            .normalizeGenerated()

        assertGolden("builders.kt", generated)
        result.classLoader.loadClass("sample.CallbackProbe").getMethod("verify").invoke(null)
    }

    @Test
    fun `unknown Int parameter names produce a diagnostic`() {
        val source = SourceFile.kotlin(
            "sample/Sample.kt",
            sampleSourceCode.replace("position: Int", "offset: Int")
        )
        val result = compile(ProcessorProvider(), *uiListRuntimeStubs, source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Unsupported event parameter 'offset'"))
    }

    @Test
    fun `position parameters must have type Int`() {
        val source = SourceFile.kotlin(
            "sample/Sample.kt",
            sampleSourceCode.replace("position: Int", "position: String")
        )
        val result = compile(ProcessorProvider(), *uiListRuntimeStubs, source)

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Event position parameter 'position' must have type Int"))
    }

    private fun compile(
        provider: SymbolProcessorProvider,
        vararg sources: SourceFile,
    ) = KotlinCompilation().apply {
        inheritClassPath = true
        configureKsp {
            symbolProcessorProviders.add(provider)
        }
        kspWithCompilation = true
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
        replace(
            Regex(
                "(?:[A-Za-z]:[/\\\\][^\\r\\n]*?Kotlin-Compilation[^/\\\\]+[/\\\\]sources[/\\\\]|" +
                    "/tmp/Kotlin-Compilation[^/]+/sources/)"
            ),
            "<sources>/"
        )
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
        open class View(val context: Context) {
            private var clickListener: ((View) -> Unit)? = null
            private var longClickListener: ((View) -> Boolean)? = null
            fun setOnClickListener(listener: (View) -> Unit) { clickListener = listener }
            fun setOnLongClickListener(listener: (View) -> Boolean) { longClickListener = listener }
            fun performClick() { clickListener?.invoke(this) }
            fun performLongClick() = longClickListener?.invoke(this) ?: false
        }
        open class ViewGroup(context: Context = Context()) : View(context)
        class LayoutInflater {
            companion object {
                fun from(context: Context): LayoutInflater = LayoutInflater()
            }
        }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "androidx/recyclerview/widget/RecyclerView.kt",
        """
        package androidx.recyclerview.widget
        class RecyclerView {
            companion object {
                const val NO_POSITION = -1
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
            val context get() = itemView.context
            var bindingAdapterPosition: Int = -1
            var absoluteAdapterPosition: Int = -1
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
        @Suppress("UNCHECKED_CAST")
        fun <T> Any.findFragmentOrNull(): T? = sample.receiver as T
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

private val sampleSourceCode = """
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

    val receiver = ClickReceiver()

    class ClickReceiver {
        val calls = mutableListOf<List<Any>>()
        @BindClickEvent(RepoItemHolder::class)
        fun clickRepo(
            view: View,
            absoluteAdapterPosition: Int,
            position: Int,
            viewholder: RepoViewHolder,
            bindingAdapterPosition: Int
        ) {
            calls += listOf("repo", view, absoluteAdapterPosition, position, viewholder, bindingAdapterPosition)
        }

        @BindLongClickEvent(RepoItemHolder::class)
        fun longClickRepo(viewholder: AbstractViewHolder<*>, absoluteAdapterPosition: Int) {
            calls += listOf("longRepo", viewholder, absoluteAdapterPosition)
        }

        @BindClickEvent(SeparatorItemHolder::class, "card")
        fun clickSeparator(
            absoluteAdapterPosition: Int,
            viewholder: SeparatorViewHolder,
            view: View,
            bindingAdapterPosition: Int
        ) {
            calls += listOf("separator", absoluteAdapterPosition, viewholder, view, bindingAdapterPosition)
        }

        @BindLongClickEvent(SeparatorItemHolder::class, "card")
        fun longClickSeparator(view: View, position: Int) {
            calls += listOf("longSeparator", view, position)
        }
    }
""".trimIndent()

private val sampleSource = SourceFile.kotlin("sample/Sample.kt", sampleSourceCode)

private val callbackProbeSource = SourceFile.kotlin(
    "sample/CallbackProbe.kt",
    """
    package sample
    import android.view.ViewGroup
    import sample.ui_list.buildRepoItemHolder
    import sample.ui_list.buildSeparatorItemHolder

    object CallbackProbe {
        @JvmStatic
        fun verify() {
            val holder = buildRepoItemHolder(ViewGroup(), "", "")
            val view = holder.itemView
            holder.bindingAdapterPosition = 2
            holder.absoluteAdapterPosition = 7
            view.performClick()
            check(receiver.calls.last() == listOf("repo", view, 7, 2, holder, 2))
            holder.bindingAdapterPosition = 3
            holder.absoluteAdapterPosition = 8
            view.performClick()
            check(receiver.calls.last() == listOf("repo", view, 8, 3, holder, 3))
            check(view.performLongClick())
            check(receiver.calls.last() == listOf("longRepo", holder, 8))
            val count = receiver.calls.size
            holder.bindingAdapterPosition = -1
            view.performClick()
            check(!view.performLongClick())
            check(receiver.calls.size == count)
            holder.bindingAdapterPosition = 3
            holder.absoluteAdapterPosition = -1
            view.performClick()
            check(receiver.calls.size == count)
            val compose = buildSeparatorItemHolder(ViewGroup(), "") as SeparatorViewHolder
            compose.bindingAdapterPosition = 5
            compose.absoluteAdapterPosition = 9
            compose.edComposeView.clickListener?.invoke("card")
            check(receiver.calls.last() == listOf("separator", 9, compose, compose.itemView, 5))
            compose.edComposeView.longClickListener?.invoke("card")
            check(receiver.calls.last() == listOf("longSeparator", compose.itemView, 5))
        }
    }
    """.trimIndent()
)

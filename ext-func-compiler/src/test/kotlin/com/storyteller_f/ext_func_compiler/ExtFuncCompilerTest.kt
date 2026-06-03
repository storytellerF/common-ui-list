package com.storyteller_f.ext_func_compiler

import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class ExtFuncCompilerTest {
    @Test
    fun `generates ext builder for ksp backed annotation variants`() {
        val result = compile(
            ExtFuncProvider(),
            SourceFile.kotlin(
                "sample/Sample.kt",
                """
                package sample

                import android.content.Context
                import androidx.annotation.MainThread
                import androidx.fragment.app.Fragment
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.ViewModelStore
                import androidx.lifecycle.ViewModelStoreOwner
                import androidx.lifecycle.viewmodel.CreationExtras
                import androidx.lifecycle.viewmodel.HasDefaultViewModelProviderFactory
                import com.storyteller_f.ext_func_definition.ExtFuncFlat
                import com.storyteller_f.ext_func_definition.ExtFuncFlatType

                @ExtFuncFlat(ExtFuncFlatType.V2)
                val Context.service get() = "service"

                @ExtFuncFlat(ExtFuncFlatType.V3)
                val Context.bindingService get() = "binding"

                @ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
                val Float.dp get() = this

                @ExtFuncFlat(ExtFuncFlatType.V5)
                @MainThread
                inline fun <reified VM : ViewModel, T, ARG> T.vm(
                    crossinline arg: () -> ARG,
                    noinline storeProducer: () -> ViewModelStore = { viewModelStore },
                    noinline ownerProducer: () -> HasDefaultViewModelProviderFactory = { this },
                    crossinline vmProducer: (ARG) -> VM,
                ) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner =
                    ViewModelLazy(VM::class, storeProducer, defaultFactory(arg, vmProducer)) {
                        ownerProducer().defaultViewModelCreationExtras
                    }

                abstract class DelegateTarget {
                    abstract fun sayTest(): String
                }

                open class DelegateOwner {
                    @ExtFuncFlat(ExtFuncFlatType.V8)
                    val delegate: DelegateTarget = object : DelegateTarget() {
                        override fun sayTest() = "ok"
                    }
                }
                """.trimIndent()
            ),
            *androidStubs,
            *lifecycleStubs,
        )

        val generated = result.sourcesGeneratedBySymbolProcessor.single {
            it.name == "ExtBuilder.kt"
        }.readText().normalizeGenerated()

        assertGolden("ext_builder_all_variants.kt", generated)
    }

    @Test
    fun `pure generators keep stable output`() {
        val output = listOf(
            generatePropertyV2("service"),
            generatePropertyV3("bindingService"),
        ).joinToString("\n---\n").normalizeGenerated()

        assertGolden("pure_generators.kt", output)
        val v6 = generateForV6()
        val v7 = generateForV7()
        assertTrue(v6.contains("fun<T1, T2> combineDao"))
        assertTrue(v6.contains("fun<T1, T2, T3, T4, T5, T6, T7, T8, T9> combineDao"))
        assertTrue(v7.contains("data class Dao2<out D1, out D2>"))
        assertTrue(v7.contains("data class Dao9<out D1, out D2, out D3, out D4, out D5, out D6, out D7, out D8, out D9>"))
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
        kotlincArguments = listOf("-Xcontext-parameters")
    }.compile()

    private fun assertGolden(name: String, actual: String) {
        val expected = javaClass.classLoader
            .getResource("golden/ext-func-compiler/$name")!!
            .readText()
            .normalizeGenerated()
        assertEquals(expected, actual)
    }

    private fun String.normalizeGenerated(): String =
        replace(Regex("(/[A-Za-z0-9_.-]+)+/common-ui-list/"), "<repo>/")
            .replace(Regex("KSCallableReferenceImpl@[0-9a-f]+"), "KSCallableReferenceImpl@<hash>")
            .replace("\r\n", "\n")
            .trim()
}

private val androidStubs = arrayOf(
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
        """.trimIndent()
    ),
)

private val lifecycleStubs = arrayOf(
    SourceFile.kotlin(
        "androidx/annotation/MainThread.kt",
        """
        package androidx.annotation
        annotation class MainThread
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "androidx/fragment/app/Fragment.kt",
        """
        package androidx.fragment.app
        import android.content.Context
        import androidx.lifecycle.ViewModelStore
        import androidx.lifecycle.viewmodel.HasDefaultViewModelProviderFactory
        open class Fragment : HasDefaultViewModelProviderFactory {
            fun requireContext(): Context = Context()
            fun requireActivity(): FragmentActivity = FragmentActivity()
            fun requireParentFragment(): Fragment = Fragment()
        }
        class FragmentActivity : Fragment() {
            override val viewModelStore: ViewModelStore = ViewModelStore()
        }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "androidx/viewbinding/ViewBinding.kt",
        """
        package androidx.viewbinding
        import android.view.View
        interface ViewBinding { val root: View }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "androidx/lifecycle/Lifecycle.kt",
        """
        package androidx.lifecycle
        open class ViewModel
        class ViewModelStore
        interface ViewModelStoreOwner { val viewModelStore: ViewModelStore }
        open class LiveData<T>(open val value: T? = null)
        class MediatorLiveData<T> : LiveData<T>() {
            fun <S> addSource(source: LiveData<S>, onChanged: (S) -> Unit) = Unit
            override var value: T? = null
        }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "androidx/lifecycle/viewmodel/ViewModel.kt",
        """
        package androidx.lifecycle.viewmodel
        import androidx.lifecycle.ViewModelStore
        interface CreationExtras
        interface HasDefaultViewModelProviderFactory {
            val viewModelStore: ViewModelStore get() = ViewModelStore()
            val defaultViewModelCreationExtras: CreationExtras get() = object : CreationExtras {}
        }
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "sample/ViewModelHelpers.kt",
        """
        package sample
        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.ViewModelStore
        import androidx.lifecycle.viewmodel.CreationExtras
        import kotlin.reflect.KClass

        class ViewModelLazy<VM : ViewModel>(
            clazz: KClass<VM>,
            storeProducer: () -> ViewModelStore,
            factoryProducer: () -> Any,
            extrasProducer: () -> CreationExtras,
        )

        inline fun <ARG, VM : ViewModel> defaultFactory(
            crossinline arg: () -> ARG,
            crossinline vmProducer: (ARG) -> VM,
        ): () -> Any = { Any() }
        """.trimIndent()
    ),
)

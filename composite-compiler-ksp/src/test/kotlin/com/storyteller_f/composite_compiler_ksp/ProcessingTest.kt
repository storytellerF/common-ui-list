package com.storyteller_f.composite_compiler_ksp

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
class ProcessingTest {
    @Test
    fun `generates composite room database adapter`() {
        val result = compile(
            ProcessingProvider(),
            *roomStubs,
            SourceFile.kotlin(
                "sample/RepoDatabase.kt",
                """
                package sample

                import androidx.room.Dao
                import androidx.room.Database
                import com.storyteller_f.composite_defination.Composite

                data class Repo(val id: String) {
                    fun remoteKeyId(): String = id
                }

                data class RepoRemoteKey(val itemId: String)

                @Dao
                interface RepoDao

                @Dao
                interface RemoteKeyDao

                @Database(entities = [Repo::class, RepoRemoteKey::class], version = 1)
                @Composite("Repo")
                abstract class RepoDatabase {
                    abstract fun reposDao(): RepoDao
                    abstract fun remoteKeyDao(): RemoteKeyDao
                }
                """.trimIndent()
            )
        )

        val generated = result.sourcesGeneratedBySymbolProcessor.single {
            it.name == "RepoComposite.kt"
        }.readText().normalizeGenerated()

        assertGolden("repo_composite.kt", generated)
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
            .getResource("golden/composite-compiler-ksp/$name")!!
            .readText()
            .normalizeGenerated()
        assertEquals(expected, actual)
    }

    private fun String.normalizeGenerated(): String =
        replace("\r\n", "\n").trim()
}

private val roomStubs = arrayOf(
    SourceFile.kotlin(
        "androidx/room/Room.kt",
        """
        package androidx.room
        import kotlin.reflect.KClass
        annotation class Dao
        annotation class Database(
            val entities: Array<KClass<*>>,
            val version: Int,
            val exportSchema: Boolean = true,
        )
        """.trimIndent()
    ),
    SourceFile.kotlin(
        "com/storyteller_f/ui_list/database/CommonRoomDatabase.kt",
        """
        package com.storyteller_f.ui_list.database
        abstract class CommonRoomDatabase<D, K, DB>(protected val database: DB) {
            abstract suspend fun clearOld()
            abstract suspend fun insertRemoteKey(remoteKeys: MutableList<K>)
            abstract suspend fun getRemoteKey(id: String): K?
            abstract suspend fun insertAllData(ds: MutableList<D>)
            abstract suspend fun deleteItemBy(d: D)
        }
        """.trimIndent()
    ),
)

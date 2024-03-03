package com.storyteller_f.file_system

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testPrefix() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(
            LocalFileSystem.CURRENT_EMULATED_PATH to LocalFileSystem.CURRENT_EMULATED_PATH,
            "/storage/self/primary" to "/storage/self/primary",
            LocalFileSystem.ROOT_USER_EMULATED_PATH to LocalFileSystem.ROOT_USER_EMULATED_PATH,
            "/storage/XX44-XX55/Downloads" to "/storage/XX44-XX55",
            "/storage/XX44-XX55" to "/storage/XX44-XX55",
            LocalFileSystem.STORAGE_PATH to "/storage"
        ).forEach { (path, expected) ->
            val prefix = getPrefix(appContext, File(path).toUri())
            assertEquals(expected, prefix?.key)
        }
    }

    @Test
    fun testList() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            listOf(
                "/storage/self" to listOf("primary"),
                "/storage/self/primary" to
                        File("/storage/self/primary").listFiles()?.toList().orEmpty(),
            ).forEach { (it, expected) ->
                val fileInstance = getLocalFileInstance(
                    appContext,
                    File(it).toUri(),
                )
                assertEquals(expected.size, fileInstance.list().count)
            }
        }

    }

}
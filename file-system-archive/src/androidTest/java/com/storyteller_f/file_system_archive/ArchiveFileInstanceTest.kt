package com.storyteller_f.file_system_archive

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.encodeByBase64
import com.storyteller_f.file_system.ensureFile
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ArchiveFileInstanceTest {

    @Test
    fun testArchiveFileInstance() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val file = File(appContext.filesDir, "test.zip").ensureFile() ?: return@runBlocking
            ZipOutputStream(file.outputStream()).use {
                val zipEntry = ZipEntry("hello.txt")
                it.putNextEntry(zipEntry)
                it.write("hello".toByteArray())
                it.closeEntry()
            }

            val archiveFileInstanceUri = Uri.Builder()
                .scheme("archive")
                .authority(Uri.fromFile(file).toString().encodeByBase64())
                .path("/")
                .build()
            val archiveFileInstance = getFileInstance(appContext, archiveFileInstanceUri)!!
            val list = archiveFileInstance.list()
            assertEquals("hello.txt", list.files.first().name)

            val helloTxtFileInstance =
                archiveFileInstance.toChild("hello.txt", FileCreatePolicy.NotCreate)!!

            assertEquals(true, helloTxtFileInstance.fileKind().isFile)
            val content = helloTxtFileInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            assertEquals("hello", content)
        }
    }

    @Test
    fun testLocalArchiveFileInstanceToChild() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val file = File(appContext.filesDir, "test.zip").ensureFile() ?: return@runBlocking
            ZipOutputStream(file.outputStream()).use {
                val zipEntry = ZipEntry("hello.txt")
                it.putNextEntry(zipEntry)
                it.write("hello".toByteArray())
                it.closeEntry()
            }

            val fileInstance = getFileInstance(appContext, Uri.fromFile(file))!!
            val instance = fileInstance.toChildEfficiently(appContext, "hello.txt")
            assertEquals("hello.txt", instance.name)
        }

    }
}
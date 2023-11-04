package com.storyteller_f.file_system

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.operate.ScopeFileMoveOpInShell
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
class MoveFileTest {
    @Test
    fun shellMoveFileInUserPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val currentUserDataPath = context.getCurrentUserDataPath()
        val userDataUri = File(currentUserDataPath).toUri()

        val userData = getLocalFileInstance(context, userDataUri)
        runBlocking {
            val pack = userData.list()
            val userPackageModel = pack.directories.first()
            val userPackage = userData.toChildEfficiently(
                context,
                userPackageModel.name,
                FileCreatePolicy.NotCreate
            )

            //在cache 下面创建一个问题
            val cacheInstance =
                userPackage.toChildEfficiently(context, "cache", FileCreatePolicy.NotCreate)
            val filesInstance = userPackage.toChildEfficiently(context, "files", FileCreatePolicy.NotCreate)
            val testFile =
                cacheInstance.toChildEfficiently(context, "test.txt", FileCreatePolicy.Create(true))
            testFile.getFileOutputStream().bufferedWriter().use {
                it.write("hello world")
            }
            ScopeFileMoveOpInShell(testFile, filesInstance, context).call()
            val readContent =
                filesInstance.toChildEfficiently(context, "test.txt", FileCreatePolicy.NotCreate)
                    .getFileInputStream().bufferedReader().use {
                    it.readText()
                }
            assertEquals("hello world", readContent)
            assertEquals(false, testFile.exists())
        }
    }
}

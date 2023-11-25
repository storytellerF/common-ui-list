package com.storyteller_f.file_system_remote

import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FtpsTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    companion object {

        @JvmStatic
        @BeforeClass
        fun setup() {
            CommonFileSystem.setup("ftps")
        }

        @JvmStatic
        @AfterClass
        fun close() {
            CommonFileSystem.close()
        }
    }

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = CommonFileSystem.ftpsSpec
        val uri = test1Spec.toUri().buildUpon().appendPath("test1").build()
        val ftpsFileInstance = FtpsFileInstance(uri)
        runBlocking {
            val list = ftpsFileInstance.list()
            Assert.assertEquals(1, list.count)
            val childInstance =
                ftpsFileInstance.toChildEfficiently(context, "hello.txt", FileCreatePolicy.NotCreate)
            val text = childInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            Assert.assertEquals("world smb", text)
        }
    }
}

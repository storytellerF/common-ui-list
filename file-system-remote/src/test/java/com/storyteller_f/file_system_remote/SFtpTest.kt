package com.storyteller_f.file_system_remote

import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@Ignore(
    """java.lang.NullPointerException: Cannot invoke "org.robolectric.internal.TestEnvironment.resetState()" 
    |because the return value of "org.robolectric.RobolectricTestRunner$
    |RobolectricFrameworkMethod.getTestEnvironment()" is null"""
)
@RunWith(RobolectricTestRunner::class)
class SFtpTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    companion object {

        @JvmStatic
        @BeforeClass
        fun setup() {
            CommonFileSystem.setup("sftp")
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

        val test1Spec = CommonFileSystem.sftpSpec
        val uri = test1Spec.toUri().buildUpon().appendPath("test1").build()
        val sFtpFileInstance = SFtpFileInstance(uri)
        runBlocking {
            val list = sFtpFileInstance.list()
            Assert.assertEquals(1, list.count)
            val childInstance =
                sFtpFileInstance.toChildEfficiently(
                    context,
                    "hello.txt",
                    FileCreatePolicy.NotCreate
                )
            val text = childInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            Assert.assertEquals("world smb", text)
        }
    }
}

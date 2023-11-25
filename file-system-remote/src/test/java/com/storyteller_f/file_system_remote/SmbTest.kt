package com.storyteller_f.file_system_remote

import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import io.mockk.MockKAnnotations
import io.mockk.junit4.MockKRule
import io.mockk.unmockkAll
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
class SmbTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    companion object {

        @JvmStatic
        @BeforeClass
        fun setup() {
            MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
            CommonFileSystem.setup()
        }

        @JvmStatic
        @AfterClass
        fun close() {
            unmockkAll()
            CommonFileSystem.close()
        }
    }

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = ShareSpec("localhost", 0, "test", "test", "smb", "test1")
        val toUri = test1Spec.toUri()
        val smbFileInstance = SmbFileInstance(toUri)
        runBlocking {
            val list = smbFileInstance.list()
            Assert.assertEquals(1, list.count)
            val childInstance =
                smbFileInstance.toChildEfficiently(context, "hello.txt", FileCreatePolicy.NotCreate)
            val text = childInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            Assert.assertEquals("world smb", text)
        }
    }
}

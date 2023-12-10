package com.storyteller_f.file_system_remote

import io.mockk.junit4.MockKRule
import org.junit.AfterClass
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
            CommonFileSystem.setup("smb")
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

        val test1Spec = CommonFileSystem.smbSpec
        val uri = test1Spec.toUri()
        val smbFileInstance = SmbFileInstance(uri)
        CommonFileSystem.commonTest(smbFileInstance, context)
    }
}

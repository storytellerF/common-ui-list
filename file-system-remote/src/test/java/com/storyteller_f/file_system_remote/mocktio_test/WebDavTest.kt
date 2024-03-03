package com.storyteller_f.file_system_remote.mocktio_test

import com.storyteller_f.file_system_remote.CommonFileSystem
import com.storyteller_f.file_system_remote.CommonFileSystemRule
import com.storyteller_f.file_system_remote.WebDavFileInstance
import io.mockk.junit4.MockKRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WebDavTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val commonRelu = CommonFileSystemRule(CommonFileSystem.webDavSpec)

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = CommonFileSystem.webDavSpec
        val uri = test1Spec.toUri().buildUpon().appendPath("test1").build()
        val webDavFileInstance = WebDavFileInstance(uri)
        CommonFileSystem.commonTest(webDavFileInstance, context)
    }
}

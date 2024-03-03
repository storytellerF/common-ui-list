package com.storyteller_f.file_system_remote.mocktio_test

import com.storyteller_f.file_system_remote.CommonFileSystem
import com.storyteller_f.file_system_remote.CommonFileSystemRule
import com.storyteller_f.file_system_remote.FtpsFileInstance
import io.mockk.junit4.MockKRule
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

    @get:Rule
    val commonRelu = CommonFileSystemRule(CommonFileSystem.ftpsSpec)

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = CommonFileSystem.ftpsSpec
        val uri = test1Spec.toUri().buildUpon().appendPath("test1").build()
        val ftpsFileInstance = FtpsFileInstance(uri)
        CommonFileSystem.commonTest(ftpsFileInstance, context)
    }
}

package com.storyteller_f.file_system

import android.net.Uri
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class TestFileInstance {
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    fun test() {
        val parse = Uri.parse("file:///storage/emulated")
        val context = RuntimeEnvironment.getApplication()
        runBlocking {
            assert(getLocalFileInstance(context, parse).list().count > 0)
        }
    }
}

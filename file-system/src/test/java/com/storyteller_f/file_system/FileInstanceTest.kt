package com.storyteller_f.file_system

import android.net.Uri
import android.os.Build
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FileInstanceTest {
    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    fun test() {
        val uri = Uri.parse("file:///storage/emulated")
        val context = RuntimeEnvironment.getApplication()
        runBlocking {
            assert(getLocalFileInstance(context, uri).list().count > 0)
        }
    }
}

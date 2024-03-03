package com.storyteller_f.file_system_remote

import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WebDavDockerTest {

    @Test
    fun test() {
        Assume.assumeTrue("未安装 ionelmc/webdav", isDockerContainerRunning())
        val remoteSpec =
            RemoteSpec("localhost", 7000, "myuser", "mypassword", RemoteAccessType.WEB_DAV)
        val toUri = remoteSpec.toUri()
        runBlocking {
            WebDavFileInstance(toUri).exists()
        }
    }
}

fun isDockerContainerRunning(): Boolean {
    return try {
        val process =
            Runtime.getRuntime().exec("docker ps --format \"{{.Names}}\" --filter name=webdav")
        process.inputStream.bufferedReader().lineSequence().contains("webdav")
    } catch (_: Exception) {
        false
    }
}

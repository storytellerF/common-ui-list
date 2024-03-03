package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.WebDavFileInstance
import com.storyteller_f.file_system_remote.checkWebDavConnection
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
        Assume.assumeTrue("未安装 ionelmc/webdav", isDockerContainerRunning("webdav"))
        val remoteSpec =
            RemoteSpec("localhost", 7000, "myuser", "mypassword", RemoteAccessType.WEB_DAV)
        remoteSpec.checkWebDavConnection()
        val toUri = remoteSpec.toUri()
        runBlocking {
            WebDavFileInstance(toUri).exists()
        }
    }
}

fun isDockerContainerRunning(dockerServerName: String): Boolean {
    return try {
        val process =
            Runtime.getRuntime().exec("docker ps --format \"{{.Names}}\" --filter name=$dockerServerName")
        process.inputStream.bufferedReader().lineSequence().contains(dockerServerName)
    } catch (_: Exception) {
        false
    }
}

package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.SFtpFileInstance
import com.storyteller_f.file_system_remote.checkFtpsConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FtpsDockerTest {

    @Test
    fun test() {
        Assume.assumeTrue("未安装 pcavezzan/ftpsdev", isDockerContainerRunning("ftps"))
        val remoteSpec =
            RemoteSpec("localhost", 2121, "myuser", "mypassword", RemoteAccessType.FTP_ES)
        remoteSpec.checkFtpsConnection()
        val toUri = remoteSpec.toUri()
        runBlocking {
            SFtpFileInstance(toUri).exists()
        }
    }
}

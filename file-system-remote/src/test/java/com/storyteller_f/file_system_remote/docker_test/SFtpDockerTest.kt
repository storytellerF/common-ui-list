package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.SFtpFileInstance
import com.storyteller_f.file_system_remote.checkSFtpConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SFtpDockerTest {

    @Test
    fun test() {
        Assume.assumeTrue("未安装 atmoz/sftp", isDockerContainerRunning("sftp"))
        val remoteSpec =
            RemoteSpec("localhost", 1022, "myuser", "mypassword", RemoteAccessType.SFTP)
        remoteSpec.checkSFtpConnection()
        val toUri = remoteSpec.toUri()
        runBlocking {
            SFtpFileInstance(toUri).exists()
        }
    }
}

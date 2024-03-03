package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.SmbFileInstance
import com.storyteller_f.file_system_remote.checkSmbConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SmbDockerTest {

    @Test
    fun test() {
        Assume.assumeTrue("未安装 dockurr/samba", isDockerContainerRunning("samba"))
        val remoteSpec =
            ShareSpec("localhost", 1445, "myuser", "mypassword", RemoteAccessType.SMB, "Data")
        remoteSpec.checkSmbConnection()
        val toUri = remoteSpec.toUri()
        runBlocking {
            SmbFileInstance(toUri).exists()
        }
    }
}

package com.storyteller_f.file_system_remote

import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockftpserver.fake.FakeFtpServer
import org.mockftpserver.fake.UserAccount
import org.mockftpserver.fake.filesystem.DirectoryEntry
import org.mockftpserver.fake.filesystem.FileEntry
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FtpTest {
    companion object {

        private var fakeFtpServer: FakeFtpServer? = null

        const val USERNAME = "user"
        const val PASSWORD = "password"
        const val SERVER = "localhost"
        const val SCHEME = RemoteAccessType.FTP

        @JvmStatic
        @BeforeClass
        fun setup() {
            val fakeFileSystem = UnixFakeFileSystem()
            fakeFileSystem.add(DirectoryEntry("/data"))
            fakeFileSystem.add(FileEntry("/data/foobar.txt", "abcdef 1234567890"))
            fakeFtpServer = FakeFtpServer().apply {
                addUserAccount(UserAccount(USERNAME, PASSWORD, "/"))
                fileSystem = fakeFileSystem
                serverControlPort = 0
                start()
            }
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            fakeFtpServer?.stop()
        }
    }

    private val toUri =
        RemoteSpec(SERVER, fakeFtpServer!!.serverControlPort, USERNAME, PASSWORD, SCHEME).toUri()

    @Test
    fun test() {
        runBlocking {
            assertEquals("data", getRemoteInstance(toUri).list().directories.first().name)
            val foobarFile = getRemoteInstance(
                toUri.buildUpon().path("/data/foobar.txt").build()
            ) as FtpFileInstance
            assertEquals(
                "abcdef 1234567890",
                foobarFile.getInputStream().bufferedReader().use {
                    it.readText()
                }
            )
            assertEquals(
                true,
                foobarFile.completePendingCommand
            )
            foobarFile.getOutputStream().bufferedWriter().use {
                it.write("hello world")
            }
            assertEquals(
                true,
                foobarFile.completePendingCommand
            )

            assertEquals(
                "hello world",
                foobarFile.getInputStream().bufferedReader().use {
                    it.readText()
                }
            )
        }
    }
}

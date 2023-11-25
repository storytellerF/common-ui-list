package com.storyteller_f.file_system_remote

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import com.storyteller_f.file_system.util.buildPath
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.FileSystem
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readAttributes
import kotlin.io.path.walk

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SmbTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    companion object {
        private val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())

        @JvmStatic
        @BeforeClass
        fun setup() {
            MockKAnnotations.init(this, relaxUnitFun = true, relaxed = true)
            fs.getPath("/test1").apply {
                createDirectory()
            }
            val helloFile = fs.getPath("/test1/hello.txt").apply {
                createFile()
            }
            helloFile.outputStream().bufferedWriter().use {
                it.write("world smb")
            }

            bindFileSystem(ShareSpec("localhost", 0, "test", "test", "smb", "test1"))
        }

        @JvmStatic
        @AfterClass
        fun close() {
            unmockkAll()
        }

        @OptIn(ExperimentalPathApi::class)
        private fun bindFileSystem(shareSpec: ShareSpec): DiskShare {
            return mockk<DiskShare> {
                every {
                    list(any())
                } answers {
                    val buildPath = buildPath(shareSpec.share, firstArg())
                    fs.getPath(buildPath).walk().filter {
                        it.name.isNotEmpty()
                    }.map { pathObject ->
                        mockk<FileIdBothDirectoryInformation> {
                            every {
                                fileName
                            } returns pathObject.name
                            every {
                                fileAttributes
                            } answers {
                                if (pathObject.isDirectory()) {
                                    FileAttributes.FILE_ATTRIBUTE_DIRECTORY.ordinal.toLong()
                                } else {
                                    0
                                }
                            }
                            val fileAttributes = pathObject.readAttributes<BasicFileAttributes>()
                            every {
                                changeTime
                            } returns FileTime(fileAttributes.lastModifiedTime().toMillis())
                            every {
                                creationTime
                            } returns FileTime(fileAttributes.creationTime().toMillis())
                            every {
                                lastAccessTime
                            } returns FileTime(fileAttributes.lastAccessTime().toMillis())
                        }
                    }.toList()
                }
                every {
                    getFileInformation(any(String::class))
                } answers {
                    mockk()
                }
                every {
                    openFile(any(), any(), any(), any(), SMB2CreateDisposition.FILE_OPEN, any())
                } answers {
                    mockk {
                        every {
                            inputStream
                        } returns fs.getPath(buildPath(shareSpec.share, firstArg())).inputStream()
                    }
                }
            }.apply {
                smbSessions[shareSpec] = this
            }
        }
    }

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = ShareSpec("localhost", 0, "test", "test", "smb", "test1")
        val toUri = test1Spec.toUri()
        val smbFileInstance = SmbFileInstance(toUri)
        runBlocking {
            val list = smbFileInstance.list()
            Assert.assertEquals(1, list.count)
            val childInstance =
                smbFileInstance.toChildEfficiently(context, "hello.txt", FileCreatePolicy.NotCreate)
            val text = childInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            Assert.assertEquals("world smb", text)
        }
    }
}

package com.storyteller_f.file_system_remote

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.util.buildPath
import io.mockk.every
import io.mockk.mockk
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

object CommonFileSystem {
    private val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())

    fun setup() {
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

    fun close() {
        smbSessions.clear()
    }

    @OptIn(ExperimentalPathApi::class)
    private fun bindFileSystem(shareSpec: ShareSpec) {
        mockk<DiskShare> {
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

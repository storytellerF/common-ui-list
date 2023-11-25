package com.storyteller_f.file_system_remote

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.common_ktx.buildMask
import com.storyteller_f.file_system.util.buildPath
import io.mockk.MockKAnswerScope
import io.mockk.every
import io.mockk.mockk
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.xfer.FilePermission
import org.apache.commons.net.ftp.FTPFile
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.Calendar
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.readAttributes
import kotlin.io.path.walk

typealias MockResponseInfo =
    MockKAnswerScope<MutableList<RemoteResourceInfo>, MutableList<RemoteResourceInfo>>

typealias MockInformation =
    MockKAnswerScope<MutableList<FileIdBothDirectoryInformation>,
        MutableList<FileIdBothDirectoryInformation>>

object CommonFileSystem {
    private val fs: FileSystem = Jimfs.newFileSystem(Configuration.unix())

    val smbSpec = ShareSpec("localhost", 0, "test", "test", "smb", "test1")

    val sftpSpec = RemoteSpec("localhost", 0, "test", "test", "sftp")

    val ftpsSpec = RemoteSpec("localhost", 0, "test", "test", "ftps")

    fun setup(type: String) {
        fs.getPath("/test1").apply {
            createDirectory()
        }
        val helloFile = fs.getPath("/test1/hello.txt").apply {
            createFile()
        }
        helloFile.outputStream().bufferedWriter().use {
            it.write("world smb")
        }

        when (type) {
            "smb" -> bindSmbSession()
            "sftp" -> bindSFtpSession()
            "ftps" -> bindFtpsSession()
        }
    }

    private fun bindFtpsSession() {
        mockk<FtpsInstance> {
            every {
                connectIfNeed()
            } returns true
            mockFtpsListFileResponse()
            every {
                inputStream(any())
            } answers {
                fs.getPath(firstArg()).inputStream()
            }
            every {
                outputStream(any())
            } answers {
                fs.getPath(firstArg()).outputStream()
            }
        }.apply {
            ftpsClients[ftpsSpec] = this
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun FtpsInstance.mockFtpsListFileResponse() {
        every {
            listFiles(any())
        } answers {
            fs.getPath(firstArg()).walk().map {
                val basicFileAttributes = it.readAttributes<BasicFileAttributes>()
                mockk<FTPFile> {
                    every {
                        hasPermission(any(), any())
                    } returns false
                    every {
                        isSymbolicLink
                    } returns basicFileAttributes.isSymbolicLink
                    every {
                        isFile
                    } returns it.isRegularFile()
                    every {
                        isDirectory
                    } returns it.isDirectory()
                    every {
                        name
                    } returns it.name
                    every {
                        timestamp
                    } returns Calendar.getInstance().apply {
                        timeInMillis = basicFileAttributes.lastModifiedTime().toMillis()
                    }
                }
            }.toList().toTypedArray()
        }
    }

    fun close() {
        smbSessions.clear()
        sftpChannels.clear()
        fs.close()
    }

    private fun bindSFtpSession() {
        mockk<SFTPClient> {
            every {
                ls(any())
            } answers {
                mockSFtpResponse()
            }
            every {
                open(any())
            } answers {
                mockSFtpRemoteFile()
            }
        }.apply {
            sftpChannels[sftpSpec] = this
        }
    }

    private fun MockKAnswerScope<RemoteFile, RemoteFile>.mockSFtpRemoteFile(): RemoteFile {
        val pathObject = fs.getPath(firstArg())
        val basicFileAttributes = pathObject.readAttributes<BasicFileAttributes>()
        return mockk {
            every {
                fetchAttributes()
            } answers {
                net.schmizz.sshj.sftp.FileAttributes.Builder().withAtimeMtime(
                    basicFileAttributes.lastAccessTime().toMillis(),
                    basicFileAttributes.lastModifiedTime().toMillis()
                ).withPermissions(buildPermission(pathObject)).build()
            }
            every {
                read(any(), any(), any(), any())
            } answers {
                pathObject.inputStream().use {
                    it.skip(firstArg())
                    it.read(secondArg(), thirdArg(), lastArg())
                }
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun MockResponseInfo.mockSFtpResponse() =
        fs.getPath(firstArg()).walk().map { pathObject ->
            val basicFileAttributes = pathObject.readAttributes<BasicFileAttributes>()
            mockk<RemoteResourceInfo> {
                mockSFtpAttributes(pathObject, basicFileAttributes)
                every {
                    isDirectory
                } returns pathObject.isDirectory()
                every {
                    name
                } returns pathObject.name
            }
        }.toList()

    private fun RemoteResourceInfo.mockSFtpAttributes(
        pathObject: Path,
        basicFileAttributes: BasicFileAttributes
    ) {
        every {
            attributes
        } answers {
            mockk {
                every {
                    mode
                } returns FileMode(buildMask {
                    if (Files.isSymbolicLink(pathObject)) FileMode.Type.SYMLINK
                })
                every {
                    atime
                } returns basicFileAttributes.lastAccessTime().toMillis()
                every {
                    mtime
                } returns basicFileAttributes.lastModifiedTime().toMillis()
                every {
                    permissions
                } returns buildPermission(pathObject)
            }
        }
    }

    private fun buildPermission(pathObject: Path?) = buildSet {
        if (Files.isReadable(pathObject)) add(FilePermission.USR_R)
        if (Files.isWritable(pathObject)) add(FilePermission.USR_W)
        if (Files.isExecutable(pathObject)) add(FilePermission.USR_X)
    }

    private fun bindSmbSession() {
        mockk<DiskShare> {
            every {
                list(any())
            } answers {
                mockSmbListResponse(smbSpec)
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
                    } returns fs.getPath(buildPath(smbSpec.share, firstArg())).inputStream()
                }
            }
        }.apply {
            smbSessions[smbSpec] = this
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun MockInformation.mockSmbListResponse(
        shareSpec: ShareSpec
    ): List<FileIdBothDirectoryInformation> {
        val buildPath = buildPath(shareSpec.share, firstArg())
        return fs.getPath(buildPath).walk().filter {
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
}

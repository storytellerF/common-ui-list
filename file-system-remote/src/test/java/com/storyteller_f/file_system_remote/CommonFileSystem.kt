package com.storyteller_f.file_system_remote

import android.content.Context
import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAccessInformation
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.msfscc.fileinformation.FileStandardInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.protocol.commons.EnumWithValue.EnumUtils
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.storyteller_f.common_ktx.buildMask
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.toChildEfficiently
import com.storyteller_f.file_system.util.buildPath
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.xfer.FilePermission
import org.apache.commons.net.ftp.FTPFile
import org.junit.Assert
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
import kotlin.io.path.isExecutable
import kotlin.io.path.isHidden
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.walk

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

    fun commonTest(fileInstance: FileInstance, context: Context) {
        runBlocking {
            val list = fileInstance.list()
            Assert.assertEquals(1, list.count)
            val childInstance =
                fileInstance.toChildEfficiently(context, "hello.txt", FileCreatePolicy.NotCreate)
            Assert.assertTrue(childInstance.fileKind().isFile)

            Assert.assertEquals(
                fs.getPath("/test1/hello.txt").readAttributes<BasicFileAttributes>()
                    .lastModifiedTime()
                    .toMillis(),
                childInstance.fileTime().lastModified
            )
            Assert.assertTrue(childInstance.filePermissions().userPermission.readable)
            val text = childInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            Assert.assertEquals("world smb", text)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun bindFtpsSession() {
        mockk<FtpsInstance> {
            every {
                connectIfNeed()
            } returns true
            every {
                listFiles(any())
            } answers {
                fs.getPath(firstArg()).walk().map {
                    mockFtpsFile(it)
                }.toList().toTypedArray<FTPFile>()
            }
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
            every {
                getFile(any())
            } answers {
                mockFtpsFile(fs.getPath(firstArg()))
            }
        }.apply {
            ftpsClients[ftpsSpec] = this
        }
    }

    private fun mockFtpsFile(p: Path): FTPFile {
        val basicFileAttributes = p.readAttributes<BasicFileAttributes>()
        return mockk<FTPFile> {
            every {
                hasPermission(any(), any())
            } answers {
                val access = firstArg<Int>()
                val permission = secondArg<Int>()
                if (access == FTPFile.USER_ACCESS) {
                    when (permission) {
                        FTPFile.READ_PERMISSION -> p.isReadable()
                        FTPFile.WRITE_PERMISSION -> p.isWritable()
                        FTPFile.EXECUTE_PERMISSION -> p.isExecutable()
                        else -> throw IllegalArgumentException()
                    }
                } else {
                    false
                }
            }
            every {
                isSymbolicLink
            } returns basicFileAttributes.isSymbolicLink
            every {
                isFile
            } returns p.isRegularFile()
            every {
                isDirectory
            } returns p.isDirectory()
            every {
                name
            } returns p.name
            every {
                timestamp
            } returns Calendar.getInstance().apply {
                timeInMillis = basicFileAttributes.lastModifiedTime().toMillis()
            }
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
                mockSFtpResponse(firstArg())
            }
            every {
                open(any())
            } answers {
                mockSFtpRemoteFile(firstArg())
            }
        }.apply {
            sftpChannels[sftpSpec] = this
        }
    }

    private fun mockSFtpRemoteFile(path: String): RemoteFile {
        val pathObject = fs.getPath(path)
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
    private fun mockSFtpResponse(path: String) =
        fs.getPath(path).walk().map { pathObject ->
            val basicFileAttributes = pathObject.readAttributes<BasicFileAttributes>()
            mockk<RemoteResourceInfo> {
                every {
                    attributes
                } answers {
                    mockSFtpAttributes(pathObject, basicFileAttributes)
                }
                every {
                    isDirectory
                } returns pathObject.isDirectory()
                every {
                    name
                } returns pathObject.name
            }
        }.toList()

    private fun mockSFtpAttributes(
        pathObject: Path,
        basicFileAttributes: BasicFileAttributes
    ): net.schmizz.sshj.sftp.FileAttributes = mockk {
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
                mockSmbListResponse(smbSpec, firstArg())
            }
            every {
                getFileInformation(any(String::class))
            } answers {
                mockSmbInformation(firstArg<String>())
            }
            every {
                openFile(any(), any(), any(), any(), SMB2CreateDisposition.FILE_OPEN, any())
            } answers {
                mockSmbInputStream(firstArg())
            }
        }.apply {
            smbSessions[smbSpec] = this
        }
    }

    private fun mockSmbInformation(path: String): FileAllInformation {
        val buildPath = buildPath(smbSpec.share, path)

        return mockk {
            every {
                standardInformation
            } answers {
                fileStandardInformation(buildPath)
            }
            every {
                basicInformation
            } answers {
                mockk {
                    every {
                        standardInformation
                    } answers {
                        fileStandardInformation(buildPath)
                    }
                    every {
                        fileAttributes
                    } answers {
                        mockSmbFileAttributes(buildPath)
                    }
                    every {
                        changeTime
                    } answers {
                        val p = fs.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().lastModifiedTime().toMillis()
                        )
                    }
                    every {
                        creationTime
                    } answers {
                        val p = fs.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().creationTime().toMillis()
                        )
                    }
                    every {
                        lastAccessTime
                    } answers {
                        val p = fs.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().lastAccessTime().toMillis()
                        )
                    }
                    every {
                        accessInformation
                    } answers {
                        mockAccessInformation(buildPath)
                    }
                }
            }
        }
    }

    private fun mockAccessInformation(buildPath: String): FileAccessInformation {
        val p = fs.getPath(buildPath)
        return mockk {
            every {
                accessFlags
            } answers {
                EnumUtils.toLong(buildSet<AccessMask> {
                    if (p.isReadable()) {
                        add(AccessMask.GENERIC_READ)
                    }
                    if (p.isWritable()) {
                        add(AccessMask.GENERIC_WRITE)
                    }
                    if (p.isExecutable()) {
                        add(AccessMask.GENERIC_EXECUTE)
                    }
                }).toInt()
            }
        }
    }

    private fun mockSmbFileAttributes(buildPath: String): Long {
        val p = fs.getPath(buildPath)
        return EnumUtils.toLong(buildSet<FileAttributes> {
            if (p.isDirectory()) {
                add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
            }
            if (p.isHidden()) {
                add(FileAttributes.FILE_ATTRIBUTE_HIDDEN)
            }
        })
    }

    private fun fileStandardInformation(buildPath: String): FileStandardInformation =
        mockk {
            every {
                isDirectory
            } returns fs.getPath(buildPath).isDirectory()
        }

    private fun mockSmbInputStream(relativePath: String): File =
        mockk {
            every {
                inputStream
            } returns fs.getPath(buildPath(smbSpec.share, relativePath)).inputStream()
        }

    @OptIn(ExperimentalPathApi::class)
    private fun mockSmbListResponse(
        shareSpec: ShareSpec,
        relativePath: String
    ): List<FileIdBothDirectoryInformation> {
        val buildPath = buildPath(shareSpec.share, relativePath)
        return fs.getPath(buildPath).walk().filter {
            it.name.isNotEmpty()
        }.map { pathObject ->
            mockSmbInformation(pathObject)
        }.toList()
    }

    private fun mockSmbInformation(pathObject: Path) =
        mockk<FileIdBothDirectoryInformation> {
            every {
                fileName
            } returns pathObject.name
            every {
                fileAttributes
            } answers {
                mockSmbFileAttributes(pathObject.pathString)
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
}
package com.storyteller_f.file_system_remote

import android.net.Uri
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import java.io.FileInputStream
import java.io.FileOutputStream

val smbClient by lazy {
    SMBClient()
}

fun ShareSpec.requireDiskShare(): DiskShare {
    val connect = smbClient.connect(server, port)
    val authenticationContext = AuthenticationContext(user, password.toCharArray(), "")
    val session = connect.authenticate(authenticationContext)
    return session.connectShare(share) as DiskShare
}

fun ShareSpec.checkSmb() {
    requireDiskShare().close()
}

val smbSessions = mutableMapOf<ShareSpec, DiskShare>()

class SmbFileInstance(private val shareSpec: ShareSpec, uri: Uri) : FileInstance(uri) {
    private var information: FileAllInformation? = null
    private var share: DiskShare? = null
    override val path: String
        get() = super.path.substring(shareSpec.share.length + 1).ifEmpty { "/" }

    override suspend fun filePermissions(): FilePermissions {
        TODO("Not yet implemented")
    }

    override suspend fun fileTime() = reconnectIfNeed().second.fileTime()
    override suspend fun fileKind() = reconnectIfNeed().let {
        FileKind.build(!it.second.standardInformation.isDirectory, false, false)
    }

    private fun initCurrentFile(): Pair<DiskShare, FileAllInformation> {
        val connectShare = getDiskShare()
        share = connectShare
        val fileInformation = connectShare.getFileInformation(path)
        information = fileInformation
        return connectShare to fileInformation
    }

    private fun getDiskShare(): DiskShare {
        val orPut = smbSessions.getOrPut(shareSpec) {
            shareSpec.requireDiskShare()
        }
        return orPut
    }

    private fun reconnectIfNeed(): Pair<DiskShare, FileAllInformation> {
        var information = information
        var share = share
        if (information == null || share == null) {
            val initCurrentFile = initCurrentFile()
            share = initCurrentFile.first
            information = initCurrentFile.second
        }
        return share to information
    }

    override suspend fun getFileLength(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileModel>,
        directoryItems: MutableList<DirectoryModel>
    ) {
        val (share, _) = reconnectIfNeed()
        share.list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val (file, child) = child(it.fileName)
            val fileInformation = share.getFileInformation(file.absolutePath)
            val fileTime = fileInformation.fileTime()
            if (fileInformation.standardInformation.isDirectory) {
                directoryItems.add(
                    DirectoryModel(
                        it.fileName,
                        child,
                        fileTime = fileTime,
                        FileKind.build(isFile = false, isSymbolicLink = false, isHidden = false)
                    )
                )
            } else {
                fileItems.add(
                    FileModel(
                        it.fileName,
                        child,
                        fileTime,
                        FileKind.build(isFile = true, isSymbolicLink = false, isHidden = false),
                        file.extension
                    )
                )
            }
        }
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

private fun FileAllInformation.fileTime(): FileTime {
    val basicInformation = basicInformation
    return FileTime(
        basicInformation.changeTime.toEpochMillis(),
        basicInformation.lastAccessTime.toEpochMillis(),
        basicInformation.creationTime.toEpochMillis()
    )
}

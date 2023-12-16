package com.storyteller_f.file_system_remote

import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.protocol.commons.EnumWithValue.EnumUtils
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
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

class SmbFileInstance(uri: Uri, private val shareSpec: ShareSpec = ShareSpec.parse(uri)) :
    FileInstance(uri) {
    private var information: FileAllInformation? = null
    private var share: DiskShare? = null
    override val path: String
        get() = super.path.substring(shareSpec.share.length + 1).ifEmpty { "/" }

    override suspend fun filePermissions(): FilePermissions {
        val second = reconnectIfNeed().second
        val accessFlags = second.accessInformation.accessFlags.toLong()
        return FilePermissions.permissions(
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ),
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ),
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ)
        )
    }

    override suspend fun fileTime() = reconnectIfNeed().second.fileTime()
    override suspend fun fileKind() = reconnectIfNeed().let {
        val second = it.second
        FileKind.build(
            !second.standardInformation.isDirectory,
            isSymbolicLink = false,
            isHidden = EnumUtils.isSet(
                second.basicInformation.fileAttributes,
                FileAttributes.FILE_ATTRIBUTE_HIDDEN
            )
        )
    }

    private fun initCurrentFile(): Pair<DiskShare, FileAllInformation> {
        val connectShare = getDiskShare()
        val fileInformation = connectShare.getFileInformation(path)
        share = connectShare
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

    override suspend fun getInputStream() = reconnectIfNeed().let {
        val openFile = it.first.openFile(
            path,
            emptySet(),
            emptySet(),
            emptySet(),
            SMB2CreateDisposition.FILE_OPEN,
            emptySet()
        )
        openFile.inputStream
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileInfo>,
        directoryItems: MutableList<FileInfo>
    ) {
        val (share, _) = reconnectIfNeed()
        share.list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val fileName = it.fileName
            val child = childUri(fileName)
            val isDirectory =
                EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
            val fileTime = it.fileTime()
            val filePermissions = FilePermissions.USER_READABLE
            if (isDirectory) {
                directoryItems.add(
                    FileInfo(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(isFile = false, isSymbolicLink = false, isHidden = false),
                        filePermissions
                    )
                )
            } else {
                fileItems.add(
                    FileInfo(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(isFile = true, isSymbolicLink = false, isHidden = false),
                        filePermissions,
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

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy) =
        SmbFileInstance(childUri(name), shareSpec)
}

private fun FileIdBothDirectoryInformation.fileTime() =
    FileTime(
        changeTime.toEpochMillis(),
        lastAccessTime.toEpochMillis(),
        creationTime.toEpochMillis()
    )

private fun FileAllInformation.fileTime(): FileTime {
    val basicInformation = basicInformation
    return FileTime(
        basicInformation.changeTime.toEpochMillis(),
        basicInformation.lastAccessTime.toEpochMillis(),
        basicInformation.creationTime.toEpochMillis()
    )
}

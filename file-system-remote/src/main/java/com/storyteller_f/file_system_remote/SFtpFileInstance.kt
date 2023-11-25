package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.common_ktx.bit
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import com.storyteller_f.file_system.util.getExtension
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.FilePermission
import java.io.FileInputStream
import java.io.FileOutputStream

val sftpChannels = mutableMapOf<RemoteSpec, SFTPClient>()

class SFtpFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {
    private var remoteFile: RemoteFile? = null
    private var attribute: FileAttributes? = null

    private fun initCurrent(): Pair<RemoteFile, FileAttributes> {
        val orPut = getInstance()
        val open = orPut.open(path)
        val fetchAttributes = open.fetchAttributes()
        remoteFile = open
        attribute = fetchAttributes
        return open to fetchAttributes
    }

    private fun getInstance() = sftpChannels.getOrPut(spec) {
        spec.sftpClient()
    }

    private fun reconnectIfNeed(): Pair<RemoteFile, FileAttributes> {
        var c = remoteFile
        var attributes = attribute
        if (c == null || attributes == null) {
            val initCurrent = initCurrent()
            c = initCurrent.first
            attributes = initCurrent.second
        }
        return c to attributes
    }

    override suspend fun filePermissions() =
        FilePermissions.fromMask(FilePermission.toMask(reconnectIfNeed().second.permissions))

    override suspend fun fileTime(): FileTime {
        val attributes = reconnectIfNeed().second
        return attributes.fileTime()
    }

    override suspend fun fileKind() = reconnectIfNeed().let {
        val type = it.second.mode.type
        val typeMask = type.toMask()
        FileKind.build(
            typeMask.bit(FileMode.Type.REGULAR.ordinal),
            typeMask.bit(FileMode.Type.SYMLINK.ordinal),
            false
        )
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
        getInstance().ls(path).forEach {
            val attributes = it.attributes
            val fileName = it.name
            val child = childUri(fileName)
            val isSymLink = attributes.mode.type.toMask().bit(FileMode.Type.SYMLINK.ordinal)
            val fileTime = attributes.fileTime()
            val filePermissions =
                FilePermissions.fromMask(FilePermission.toMask(attributes.permissions))
            if (it.isDirectory) {
                directoryItems.add(
                    DirectoryModel(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(false, isSymLink, false),
                        filePermissions
                    )
                )
            } else {
                fileItems.add(
                    FileModel(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(true, isSymLink, false),
                        filePermissions,
                        getExtension(fileName).orEmpty()
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

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

fun RemoteSpec.sftpClient(): SFTPClient {
    val sshClient = SSHClient()
    sshClient.addHostKeyVerifier(PromiscuousVerifier())
    sshClient.connect(server, port)
    sshClient.authPassword(user, password)
    return sshClient.newSFTPClient()
}

fun RemoteSpec.checkSftp() {
    sftpClient()
}

private fun FileAttributes.fileTime(): FileTime {
    return FileTime(mtime, atime)
}

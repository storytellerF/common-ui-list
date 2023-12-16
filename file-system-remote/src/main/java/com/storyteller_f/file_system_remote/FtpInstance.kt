package com.storyteller_f.file_system_remote

import android.net.Uri
import android.util.Log
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.model.FileInfo
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

val ftpClients = mutableMapOf<RemoteSpec, FtpInstance>()

class FtpFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {
    private var ftpFile: FTPFile? = null

    private fun initCurrentFile(): FTPFile? {
        return getInstance().get(path)?.apply {
            ftpFile = this
        }
    }

    private fun getInstance(): FtpInstance {
        val ftpInstance = ftpClients.getOrPut(spec) {
            FtpInstance(spec)
        }
        if (ftpInstance.connectIfNeed()) {
            return ftpInstance
        }
        throw Exception("login failed")
    }

    val completePendingCommand
        get() = getInstance().completePendingCommand

    override suspend fun filePermissions() = reconnectIfNeed()!!.permissions()

    override suspend fun fileTime() = reconnectIfNeed()!!.fileTime()
    override suspend fun fileKind() = reconnectIfNeed()!!.let {
        FileKind.build(it.isFile, it.isSymbolicLink, false)
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

    override suspend fun getInputStream() = getInstance().inputStream(path)!!

    override suspend fun getOutputStream() = getInstance().outputStream(path)!!

    override suspend fun listInternal(
        fileItems: MutableList<FileInfo>,
        directoryItems: MutableList<FileInfo>
    ) {
        val listFiles = getInstance().listFiles(path)
        listFiles?.forEach {
            val name = it.name
            val child = childUri(name)
            val permission = it.permissions()
            val fileTime = it.fileTime()
            val isSymbolicLink = it.isSymbolicLink
            if (it.isFile) {
                fileItems.add(
                    FileInfo(
                        name,
                        child,
                        fileTime,
                        FileKind.build(true, isSymbolicLink, false),
                        permission,
                    )
                )
            } else {
                directoryItems.add(
                    FileInfo(
                        name,
                        child,
                        fileTime,
                        FileKind.build(false, isSymbolicLink, false),
                        permission,
                    )
                )
            }
        }
    }

    private fun reconnectIfNeed(): FTPFile? {
        return ftpFile ?: return initCurrentFile()
    }

    override suspend fun exists(): Boolean {
        return reconnectIfNeed() != null
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

class FtpInstance(private val spec: RemoteSpec) {
    private val ftp: FTPClient = FTPClient().apply {
        addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
    }

    @Throws(IOException::class)
    fun open(): Boolean {
        ftp.connect(spec.server, spec.port)
        val reply = ftp.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect()
            throw IOException("Exception in connecting to FTP Server")
        }
        return ftp.login(spec.user, spec.password)
    }

    fun get(path: String?): FTPFile? {
        return ftp.mlistFile(path)
    }

    @Throws(IOException::class)
    fun close() {
        ftp.disconnect()
    }

    @Throws(IOException::class)
    fun listFiles(path: String?): Array<out FTPFile>? {
        return ftp.listFiles(path)
    }

    @Throws(IOException::class)
    fun downloadFile(source: String?, destination: String?) {
        val out = FileOutputStream(destination)
        ftp.retrieveFile(source, out)
    }

    @Throws(IOException::class)
    fun putFileToPath(file: File?, path: String?) {
        ftp.storeFile(path, FileInputStream(file))
    }

    fun connectIfNeed(): Boolean {
        return if (ftp.isConnected && ftp.isAvailable) {
            true
        } else {
            try {
                open()
            } catch (e: Exception) {
                Log.e(TAG, "connectIfNeed: ", e)
                false
            }
        }
    }

    fun inputStream(path: String): InputStream? {
        return ftp.retrieveFileStream(path)
    }

    fun outputStream(path: String): OutputStream? {
        return ftp.storeFileStream(path)
    }

    val completePendingCommand: Boolean
        get() {
            return ftp.completePendingCommand()
        }

    companion object {
        private const val TAG = "FtpInstance"
    }
}

fun FTPFile.permissions() = FilePermissions(
    filePermission(FTPFile.USER_ACCESS),
    filePermission(FTPFile.GROUP_ACCESS),
    filePermission(FTPFile.WORLD_ACCESS)
)

fun FTPFile.filePermission(access: Int) = FilePermission(
    hasPermission(access, FTPFile.READ_PERMISSION),
    hasPermission(access, FTPFile.WRITE_PERMISSION),
    hasPermission(access, FTPFile.EXECUTE_PERMISSION),
)

package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.util.getExtension
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClientConfig
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

val ftpsClients = mutableMapOf<RemoteSpec, FtpsInstance>()

class FtpsFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {
    private var ftpFile: FTPFile? = null

    private fun initCurrentFile(): FTPFile? {
        val ftpInstance = getInstance()
        return ftpInstance.getFile(path)?.apply {
            ftpFile = this
        }
    }

    private fun getInstance(): FtpsInstance {
        return ftpsClients.getOrPut(spec) {
            FtpsInstance(spec)
        }
    }

    override suspend fun filePermissions() = reconnectIfNeed()!!.permissions()

    override suspend fun fileTime() = reconnectIfNeed()!!.fileTime()
    override suspend fun fileKind() = reconnectIfNeed()!!.let {
        FileKind.build(it.isFile, it.isSymbolicLink, false, it.fileLength(), extension)
    }

    override suspend fun getFileLength(): Long {
        return reconnectIfNeed()!!.size
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
            val time = it.fileTime()
            val filePermissions = it.permissions()
            val isSymbolicLink = it.isSymbolicLink
            if (it.isFile) {
                fileItems.add(
                    FileInfo(
                        name,
                        child,
                        time,
                        FileKind.build(
                            true,
                            isSymbolicLink,
                            false,
                            it.fileLength(),
                            getExtension(name).orEmpty()
                        ),
                        filePermissions,
                    )
                )
            } else if (it.isDirectory) {
                directoryItems.add(
                    FileInfo(
                        name,
                        child,
                        time,
                        FileKind.build(true, isSymbolicLink, false, 0, ""),
                        filePermissions
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

    override suspend fun toChild(name: String, policy: FileCreatePolicy) =
        FtpsFileInstance(childUri(name), spec)
}

class FtpsInstance(private val spec: RemoteSpec) {
    private val ftps: FTPSClient = FTPSClient(spec.type == RemoteAccessType.FTPS).apply {
        addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
        val ftpClientConfig = FTPClientConfig()
        configure(ftpClientConfig)
    }

    fun open(): Boolean {
        connect()
        val login = ftps.login(spec.user, spec.password)
        if (login) {
            ftps.enterLocalPassiveMode()
        }
        return login
    }

    private fun connect() {
        if (ftps.isConnected) return
        ftps.connect(spec.server, spec.port)
        val reply = ftps.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftps.disconnect()
            throw IOException("Exception in connecting to FTP Server")
        }
    }

    fun getFile(path: String?): FTPFile? {
        return client {
            mlistFile(path)
        }
    }

    fun listFiles(path: String?): Array<out FTPFile>? = client {
        listFiles(path)
    }

    fun inputStream(path: String): InputStream? = client {
        retrieveFileStream(path)
    }

    fun outputStream(path: String): OutputStream? = client {
        storeFileStream(path)
    }

    /**
     * @return 返回是否可用
     */
    private fun <T : Any> client(block: FTPSClient.() -> T): T {
        if (isAvailable()) {
            if (!open()) {
                throw Exception("login failed.")
            }
        }
        return ftps.block()
    }

    internal fun isAvailable() = !ftps.isAvailable || !ftps.isConnected
}

fun FTPFile.fileTime() = FileTime(timestamp.timeInMillis)

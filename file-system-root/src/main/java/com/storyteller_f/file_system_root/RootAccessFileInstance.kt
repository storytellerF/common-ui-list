package com.storyteller_f.file_system_root

import android.net.Uri
import com.storyteller_f.file_system.buildPath
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.File

class RootAccessFileInstance(private val remote: FileSystemManager, uri: Uri) : FileInstance(uri) {

    private var extendedFile = remote.getFile(path)
    override suspend fun filePermissions() = FilePermissions(
        FilePermission(
            extendedFile.canRead(),
            extendedFile.canWrite(),
            extendedFile.canExecute()
        )
    )

    override suspend fun fileTime() = extendedFile.fileTime()
    override suspend fun fileKind() =
        FileKind.build(
            extendedFile.isFile,
            extendedFile.isSymlink,
            extendedFile.isHidden,
            extendedFile.length(),
            extension
        )

    override suspend fun getFileLength(): Long {
        return extendedFile.length()
    }

    override suspend fun getFileInputStream() = extendedFile.inputStream()

    override suspend fun getFileOutputStream() = extendedFile.outputStream()

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        val listFiles = extendedFile.listFiles()
        listFiles?.forEach {
            val permissions = it.permissions()
            val child = childUri(it.name)
            val fileTime = it.fileTime()
            if (it.isFile) {
                val fileName = it.name
                fileSystemPack.addFile(
                    FileInfo(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(
                            isFile = true,
                            isSymbolicLink = false,
                            isHidden = it.isHidden,
                            it.length(),
                            getExtension(fileName).orEmpty()
                        ),
                        permissions,
                    )
                )
            } else if (it.isDirectory) {
                fileSystemPack.addDirectory(
                    FileInfo(
                        it.name,
                        child,
                        fileTime,
                        FileKind.build(
                            isFile = false,
                            isSymbolicLink = false,
                            isHidden = it.isHidden,
                            0,
                            ""
                        ),
                        permissions
                    )
                )
            }
        }
    }

    override suspend fun exists(): Boolean = extendedFile.exists()

    override suspend fun deleteFileOrEmptyDirectory(): Boolean = extendedFile.delete()

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        val newUri = uri.buildUpon().path(extendedFile.parent!!).build()
        return RootAccessFileInstance(remote, newUri)
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
        val newUri = uri.buildUpon().path(
            buildPath(extendedFile.path, name)
        ).build()
        return RootAccessFileInstance(remote, newUri)
    }

    companion object {
        const val ROOT_FILESYSTEM_SCHEME = "root"
        private var libSuManager: FileSystemManager? = null

        @Suppress("unused")
        val isReady get() = libSuManager != null

        @Suppress("unused")
        fun registerLibSuRemote(manager: FileSystemManager) {
            libSuManager = manager
        }

        fun buildInstance(uri: Uri): RootAccessFileInstance? {
            val r = libSuManager ?: return null
            return RootAccessFileInstance(r, uri)
        }
    }
}

private fun ExtendedFile.fileTime(): FileTime = FileTime(lastModified())

fun File.permissions(): FilePermissions {
    val w = canWrite()
    val e = canExecute()
    val r = canRead()
    return FilePermissions.permissions(r, w, e)
}

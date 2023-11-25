package com.storyteller_f.file_system_root

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import com.storyteller_f.file_system.util.addDirectory
import com.storyteller_f.file_system.util.addFile
import com.storyteller_f.file_system.util.buildPath
import com.storyteller_f.file_system.util.permissions
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager

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
        FileKind.build(extendedFile.isFile, extendedFile.isSymlink, extendedFile.isHidden)

    override suspend fun getFileLength(): Long {
        return extendedFile.length()
    }

    override suspend fun getFileInputStream() = extendedFile.inputStream()

    override suspend fun getFileOutputStream() = extendedFile.outputStream()

    override suspend fun listInternal(
        fileItems: MutableList<FileModel>,
        directoryItems: MutableList<DirectoryModel>
    ) {
        val listFiles = extendedFile.listFiles()
        listFiles?.forEach {
            val permissions = it.permissions()
            val child = childUri(it.name)
            val pair = it to child
            val fileTime = it.fileTime()
            if (it.isFile) {
                fileItems.addFile(pair, permissions, fileTime)
            } else if (it.isDirectory) {
                directoryItems.addDirectory(pair, permissions, fileTime)
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
        val newUri = uri.buildUpon().path(buildPath(extendedFile.path, name)).build()
        return RootAccessFileInstance(remote, newUri)
    }

    companion object {
        const val ROOT_FILESYSTEM_SCHEME = "root"
        var remote: FileSystemManager? = null

        val isReady get() = remote != null

        fun instance(uri: Uri): RootAccessFileInstance? {
            val r = remote ?: return null
            return RootAccessFileInstance(r, uri)
        }
    }
}

private fun ExtendedFile.fileTime(): FileTime = FileTime(lastModified())

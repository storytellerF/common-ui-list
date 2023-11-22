package com.storyteller_f.file_system.instance

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.util.ObjectsCompat
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.file_system.util.getExtension
import com.storyteller_f.file_system.util.parentPath
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

// todo getChannel
// todo file descriptor
abstract class FileInstance(val uri: Uri) {

    open val path: String = uri.path!!

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    val parent: String?
        get() = parentPath(path)

    val name: String
        get() = File(path).name

    @WorkerThread
    abstract suspend fun filePermissions(): FilePermissions

    @WorkerThread
    abstract suspend fun fileTime(): FileTime

    @WorkerThread
    suspend fun getFile() =
        FileItemModel(
            name,
            uri,
            isHidden(),
            fileTime().lastModified ?: 0,
            isSymbolicLink(),
            getExtension(name).orEmpty()
        )

    @WorkerThread
    suspend fun getDirectory() =
        DirectoryItemModel(name, uri, isHidden(), fileTime().lastModified ?: 0, isSymbolicLink())

    @WorkerThread
    suspend fun getFileSystemItem(): FileSystemItemModel =
        if (isFile()) getFile() else getDirectory()

    @WorkerThread
    abstract suspend fun getFileInputStream(): FileInputStream

    @WorkerThread
    abstract suspend fun getFileOutputStream(): FileOutputStream

    @WorkerThread
    open suspend fun getInputStream(): InputStream {
        return getFileInputStream()
    }

    @WorkerThread
    open suspend fun getOutputStream(): OutputStream {
        return getFileOutputStream()
    }

    @WorkerThread
    open suspend fun isSymbolicLink(): Boolean = false

    @WorkerThread
    open suspend fun isSoftLink(): Boolean = false

    @WorkerThread
    open suspend fun isHardLink(): Boolean = false

    @WorkerThread
    abstract suspend fun isHidden(): Boolean

    /**
     * 是否是文件
     *
     * @return true 代表是文件
     */
    @WorkerThread
    abstract suspend fun isFile(): Boolean

    @WorkerThread
    abstract suspend fun isDirectory(): Boolean

    @WorkerThread
    abstract suspend fun getDirectorySize(): Long

    @WorkerThread
    abstract suspend fun getFileLength(): Long

    /**
     * 应该仅用于目录。可能会抛出异常，内部不会处理。
     */
    @WorkerThread
    protected abstract suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    )

    @WorkerThread
    suspend fun list(): FileSystemPack {
        val fileSystemPack = FileSystemPack(buildFilesContainer(), buildDirectoryContainer())
        listInternal(fileSystemPack.files, fileSystemPack.directories)
        return fileSystemPack
    }

    /**
     * 是否存在
     *
     * @return true代表存在
     */
    @WorkerThread
    abstract suspend fun exists(): Boolean

    @WorkerThread
    abstract suspend fun createFile(): Boolean

    @WorkerThread
    abstract suspend fun createDirectory(): Boolean

    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     * 不应该考虑能否转换成功
     *
     * @param name                名称
     * @return 返回子对象
     */
    @WorkerThread
    abstract suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance?

    /**
     * 移动指针，指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @return 返回他的文件夹
     */
    @WorkerThread
    abstract suspend fun toParent(): FileInstance

    /**
     * 删除当前文件
     *
     * @return 返回是否删除成功
     */
    @WorkerThread
    abstract suspend fun deleteFileOrEmptyDirectory(): Boolean

    /**
     * 重命名当前文件
     *
     * @param newName 新的文件名，不包含路径
     * @return 返回是否重命名成功
     */
    @WorkerThread
    abstract suspend fun rename(newName: String): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as FileInstance
        return ObjectsCompat.equals(uri, that.uri)
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(uri)
    }

    private fun buildFilesContainer(): MutableList<FileItemModel> {
        return mutableListOf()
    }

    private fun buildDirectoryContainer(): MutableList<DirectoryItemModel> {
        return mutableListOf()
    }

    protected fun child(it: String): Pair<File, Uri> {
        val file = File(path, it)
        val child = uri.buildUpon().path(file.absolutePath).build()
        return Pair(file, child)
    }
}

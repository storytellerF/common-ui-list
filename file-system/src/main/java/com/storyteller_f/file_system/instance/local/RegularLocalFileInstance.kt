package com.storyteller_f.file_system.instance.local

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileCreatePolicy.*
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import com.storyteller_f.file_system.util.addDirectory
import com.storyteller_f.file_system.util.addFile
import com.storyteller_f.file_system.util.fileTime
import com.storyteller_f.file_system.util.permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

@Suppress("unused")
class RegularLocalFileInstance(context: Context, uri: Uri) : LocalFileInstance(context, uri) {
    private val innerFile = File(path)

    @Throws(IOException::class)
    override suspend fun createFile(): Boolean {
        return if (innerFile.exists()) {
            true
        } else {
            withContext(Dispatchers.IO) {
                innerFile.createNewFile()
            }
        }
    }

    override suspend fun isHidden(): Boolean = innerFile.isHidden

    override suspend fun createDirectory(): Boolean {
        return if (innerFile.exists()) true else innerFile.mkdirs()
    }

    @Throws(Exception::class)
    override suspend fun toChild(name: String, policy: FileCreatePolicy): LocalFileInstance {
        val subFile = File(innerFile, name)
        val uri = getUri(subFile)
        val internalFileInstance = RegularLocalFileInstance(context, uri)
        // 检查目标文件是否存在
        checkChildExistsOtherwiseCreate(subFile, policy)
        return internalFileInstance
    }

    @Suppress("ThrowsCount")
    @Throws(IOException::class)
    private suspend fun checkChildExistsOtherwiseCreate(file: File, policy: FileCreatePolicy) {
        when {
            !exists() -> throw IOException("当前文件或者文件夹不存在。path:$path")
            fileKind().isFile -> throw IOException("当前是一个文件，无法向下操作")
            !file.exists() -> when {
                policy !is Create -> throw IOException("不存在，且不能创建")
                policy.isFile -> {
                    if (!withContext(Dispatchers.IO) {
                            file.createNewFile()
                        }) {
                        throw IOException("新建文件失败")
                    }
                }

                !file.mkdirs() -> throw IOException("新建文件失败")
            }
        }
    }

    @Throws(FileNotFoundException::class)
    override suspend fun getFileInputStream(): FileInputStream = withContext(Dispatchers.IO) {
        FileInputStream(innerFile)
    }

    @Throws(FileNotFoundException::class)
    override suspend fun getFileOutputStream(): FileOutputStream = withContext(Dispatchers.IO) {
        FileOutputStream(innerFile)
    }

    override suspend fun filePermissions() =
        FilePermissions(
            FilePermission(
                innerFile.canRead(),
                innerFile.canWrite(),
                innerFile.canExecute()
            )
        )

    override suspend fun fileTime() = innerFile.fileTime()
    override suspend fun fileKind() =
        FileKind.build(innerFile.isFile, innerFile.isSymbolicLink(), innerFile.isHidden)

    override suspend fun getFileLength(): Long = innerFile.length()

    @WorkerThread
    public override suspend fun listInternal(
        fileItems: MutableList<FileModel>,
        directoryItems: MutableList<DirectoryModel>
    ) {
        val listFiles = innerFile.listFiles() // 获取子文件
        if (listFiles != null) {
            for (childFile in listFiles) {
                val child = child(childFile.name)
                val permissions = childFile.permissions()
                val fileTime = child.first.fileTime()
                (if (childFile.isDirectory) {
                    addDirectory(directoryItems, child, permissions, fileTime)
                } else {
                    addFile(fileItems, child, permissions, fileTime)
                })
            }
        }
    }

    override suspend fun exists(): Boolean {
        return innerFile.exists()
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        return innerFile.delete()
    }

    override suspend fun rename(newName: String): Boolean {
        return innerFile.renameTo(File(newName))
    }

    override suspend fun toParent(): LocalFileInstance {
        return RegularLocalFileInstance(context, getUri(innerFile.parentFile!!))
    }

    override suspend fun getDirectorySize(): Long = getFileSize(innerFile)

    @WorkerThread
    private suspend fun getFileSize(file: File): Long {
        var size: Long = 0
        val files = file.listFiles() ?: return 0
        for (f in files) {
            yield()
            size += if (f.isFile) {
                f.length()
            } else {
                getFileSize(f)
            }
        }
        return size
    }

    companion object {
        private const val TAG = "ExternalFileInstance"
        private fun getUri(subFile: File): Uri {
            return Uri.Builder().scheme("file").path(subFile.path).build()
        }
    }
}

private fun File.isSymbolicLink() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Files.isSymbolicLink(toPath())
} else {
    try {
        absolutePath == canonicalPath
    } catch (_: IOException) {
        false
    }
}

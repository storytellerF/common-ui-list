package com.storyteller_f.file_system.util

import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import com.storyteller_f.file_system.model.FileSystemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

suspend fun File.fileTime(): FileTime {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val basicFileAttributes =
                withContext(Dispatchers.IO) {
                    Files.readAttributes(toPath(), BasicFileAttributes::class.java)
                }
            val createdTime = basicFileAttributes.creationTime().toMillis()
            val lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
            return FileTime(lastModified(), lastAccessTime, createdTime)
        } catch (_: IOException) {
        }
    }
    return FileTime(lastModified())
}

/**
 * 添加普通文件，判断过滤监听事件
 *
 * @param files            填充目的地
 * @param uri              绝对路径
 * @param name             文件名
 * @param isHidden           是否是隐藏文件
 * @param extension        文件后缀名
 * @return 返回添加的文件
 */
private fun addFile(
    files: MutableCollection<FileModel>,
    uri: Uri?,
    name: String,
    isHidden: Boolean,
    extension: String?,
    permission: String?,
    size: Long,
    fileTime: FileTime,
): FileModel? {
    val fileItemModel = FileModel(
        name,
        uri!!,
        fileTime,
        FileKind.build(
            isFile = true,
            isSymbolicLink = false,
            isHidden = isHidden
        ),
        extension.orEmpty()
    )
    fileItemModel.permissions = permission
    fileItemModel.size = size
    return if (files.add(fileItemModel)) fileItemModel else null
}

/**
 * 添加普通目录，判断过滤监听事件
 *
 * @param directories      填充目的地
 * @param uri              绝对路径
 * @param directoryName    文件夹名
 * @param isHidden     是否是隐藏文件
 * @return 如果客户端不允许添加，返回null
 */
private fun addDirectory(
    directories: MutableCollection<DirectoryModel>,
    uri: Uri?,
    directoryName: String,
    isHidden: Boolean,
    permissions: String?,
    fileTime: FileTime,
): FileSystemModel? {
    val e = DirectoryModel(
        directoryName,
        uri!!,
        fileTime,
        FileKind.build(
            isFile = false,
            isSymbolicLink = false,
            isHidden = isHidden
        )
    )
    e.permissions = permissions
    return if (directories.add(e)) e else null
}

/**
 * 添加普通目录，判断过滤监听事件
 */
fun addDirectory(
    directories: MutableCollection<DirectoryModel>,
    uriPair: Pair<File?, Uri?>?,
    permissions: String?,
    fileTime: FileTime,
): FileSystemModel? {
    val childDirectory = uriPair!!.first
    val hidden = childDirectory!!.isHidden
    val name = childDirectory.name
    return addDirectory(directories, uriPair.second, name, hidden, permissions, fileTime)
}

/**
 * 添加普通目录，判断过滤监听事件
 */
fun addFile(
    directories: MutableCollection<FileModel>,
    uriPair: Pair<File, Uri>,
    permissions: String?,
    fileTime: FileTime,
): FileSystemModel? {
    val childFile = uriPair.first
    val hidden = childFile.isHidden
    val name = childFile.name
    val extension = childFile.extension
    val length = childFile.length()
    return addFile(
        directories,
        uriPair.second,
        name,
        hidden,
        extension,
        permissions,
        length,
        fileTime
    )
}

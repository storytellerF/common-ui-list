package com.storyteller_f.file_system.util

import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
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
 * @param lastModifiedTime 上次访问时间
 * @param extension        文件后缀名
 * @return 返回添加的文件
 */
private fun addFile(
    files: MutableCollection<FileItemModel>,
    uri: Uri?,
    name: String,
    isHidden: Boolean,
    extension: String?,
    permission: String?,
    size: Long,
    fileTime: FileTime,
): FileItemModel? {
    val fileItemModel = FileItemModel(name, uri!!, isHidden, false, extension.orEmpty(), fileTime)
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
 * @param lastModifiedTime 上次访问时间
 * @return 如果客户端不允许添加，返回null
 */
private fun addDirectory(
    directories: MutableCollection<DirectoryItemModel>,
    uri: Uri?,
    directoryName: String,
    isHidden: Boolean,
    permissions: String?,
    fileTime: FileTime,
): FileSystemItemModel? {
    val e = DirectoryItemModel(directoryName, uri!!, isHidden, false, fileTime)
    e.permissions = permissions
    return if (directories.add(e)) e else null
}

/**
 * 添加普通目录，判断过滤监听事件
 */
fun addDirectory(
    directories: MutableCollection<DirectoryItemModel>,
    uriPair: Pair<File?, Uri?>?,
    permissions: String?,
    fileTime: FileTime,
): FileSystemItemModel? {
    val childDirectory = uriPair!!.first
    val hidden = childDirectory!!.isHidden
    val name = childDirectory.name
    return addDirectory(directories, uriPair.second, name, hidden, permissions, fileTime)
}

/**
 * 添加普通目录，判断过滤监听事件
 */
fun addFile(
    directories: MutableCollection<FileItemModel>,
    uriPair: Pair<File, Uri>,
    permissions: String?,
    fileTime: FileTime,
): FileSystemItemModel? {
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

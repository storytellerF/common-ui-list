package com.storyteller_f.file_system.util

import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
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
 * 添加普通目录，判断过滤监听事件
 */
fun MutableCollection<DirectoryModel>.addDirectory(
    uriPair: Pair<File, Uri>,
    permissions: FilePermissions,
    fileTime: FileTime,
): FileSystemModel? {
    val (childDirectory, uri) = uriPair

    return addDirectory(
        uri,
        childDirectory.name,
        permissions,
        fileTime,
        FileKind.build(
            isFile = false,
            isSymbolicLink = false,
            isHidden = childDirectory.isHidden
        )
    )
}

/**
 * 添加普通目录，判断过滤监听事件
 */
fun MutableCollection<FileModel>.addFile(
    uriPair: Pair<File, Uri>,
    permissions: FilePermissions,
    fileTime: FileTime,
): FileSystemModel? {
    val (childFile, uri) = uriPair
    return addFile(
        uri,
        childFile.name,
        childFile.extension,
        childFile.length(),
        permissions,
        fileTime,
        FileKind.build(
            isFile = true,
            isSymbolicLink = false,
            isHidden = childFile.isHidden
        )
    )
}

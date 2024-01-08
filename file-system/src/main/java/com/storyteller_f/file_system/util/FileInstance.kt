package com.storyteller_f.file_system.util

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo

/**
 * 添加普通文件，判断过滤监听事件
 *
 * @param uri              绝对路径
 * @param name             文件名
 * @return 返回添加的文件
 */
fun MutableCollection<FileInfo>.addFile(
    uri: Uri,
    name: String,
    permission: FilePermissions,
    fileTime: FileTime,
    fileKind: FileKind,
): FileInfo? {
    val fileItemModel = FileInfo(
        name,
        uri,
        fileTime,
        fileKind,
        permission,
    )
    return if (add(fileItemModel)) fileItemModel else null
}

/**
 * 添加普通目录，判断过滤监听事件
 *
 * @param uri              绝对路径
 * @param directoryName    文件夹名
 * @return 如果客户端不允许添加，返回null
 */
fun MutableCollection<FileInfo>.addDirectory(
    uri: Uri,
    directoryName: String,
    permissions: FilePermissions,
    fileTime: FileTime,
    fileKind: FileKind,
): FileInfo? {
    val model = FileInfo(
        directoryName,
        uri,
        fileTime,
        fileKind,
        permissions
    )
    return if (add(model)) model else null
}

package com.storyteller_f.file_system.util

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryModel
import com.storyteller_f.file_system.model.FileModel
import com.storyteller_f.file_system.model.FileSystemModel

/**
 * 添加普通文件，判断过滤监听事件
 *
 * @param uri              绝对路径
 * @param name             文件名
 * @param extension        文件后缀名
 * @return 返回添加的文件
 */
fun MutableCollection<FileModel>.addFile(
    uri: Uri,
    name: String,
    extension: String?,
    size: Long,
    permission: FilePermissions,
    fileTime: FileTime,
    fileKind: FileKind,
): FileModel? {
    val fileItemModel = FileModel(
        name,
        uri,
        fileTime,
        fileKind,
        permission,
        extension.orEmpty()
    )
    fileItemModel.size = size
    return if (add(fileItemModel)) fileItemModel else null
}

/**
 * 添加普通目录，判断过滤监听事件
 *
 * @param uri              绝对路径
 * @param directoryName    文件夹名
 * @return 如果客户端不允许添加，返回null
 */
fun MutableCollection<DirectoryModel>.addDirectory(
    uri: Uri,
    directoryName: String,
    permissions: FilePermissions,
    fileTime: FileTime,
    fileKind: FileKind,
): FileSystemModel? {
    val model = DirectoryModel(
        directoryName,
        uri,
        fileTime,
        fileKind,
        permissions
    )
    return if (add(model)) model else null
}

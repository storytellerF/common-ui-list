package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime

/**
 * @param extension A file extension without the leading '.'
 */
class FileModel(
    name: String,
    uri: Uri,
    time: FileTime,
    kind: FileKind,
    val extension: String
) : FileSystemModel(
    name,
    uri,
    time,
    kind
)

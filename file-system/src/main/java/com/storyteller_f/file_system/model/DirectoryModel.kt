package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime

class DirectoryModel(
    name: String,
    uri: Uri,
    fileTime: FileTime,
    kind: FileKind,
) : FileSystemModel(
    name,
    uri,
    fileTime,
    kind
)

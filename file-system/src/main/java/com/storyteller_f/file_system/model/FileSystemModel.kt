package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FileTime

open class FileSystemModel(
    name: String,
    uri: Uri,
    val fileTime: FileTime,
    val kind: FileKind,
) : FileSystemModelLite(name, uri) {

    var size: Long = 0
    var formattedSize: String? = null
    var permissions: String? = null
}

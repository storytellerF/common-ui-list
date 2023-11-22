package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileTime

open class FileSystemItemModel(
    name: String,
    uri: Uri,
    val isHidden: Boolean,
    val isSymLink: Boolean,
    val fileTime: FileTime,
) : FileSystemItemModelLite(name, uri) {

    var size: Long = 0
    var formattedSize: String? = null
    var permissions: String? = null
}

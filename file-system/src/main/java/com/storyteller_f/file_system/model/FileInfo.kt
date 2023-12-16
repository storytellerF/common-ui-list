package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.util.getExtension

open class FileInfo(
    name: String,
    uri: Uri,
    val time: FileTime,
    val kind: FileKind,
    val permissions: FilePermissions,
) : FileSimpleInfo(name, uri) {

    var size: Long = 0
    var formattedSize: String? = null

    val extension = getExtension(name).orEmpty()
}

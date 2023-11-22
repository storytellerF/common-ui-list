package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileTime
import java.io.File

open class FileItemModel : FileSystemItemModel {

    /**
     *  A file extension without the leading '.'
     */
    val extension: String

    constructor(
        file: File,
        uri: Uri,
        isHide: Boolean = file.isHidden,
        extension: String = file.extension,
        isSymLink: Boolean,
        fileTime: FileTime,
    ) : super(
        file.name,
        uri,
        isHide,
        isSymLink,
        fileTime
    ) {
        this.extension = extension
    }

    constructor(
        name: String,
        uri: Uri,
        isHidden: Boolean,
        isSymLink: Boolean,
        extension: String,
        time: FileTime,
    ) : super(
        name,
        uri,
        isHidden,
        isSymLink,
        time
    ) {
        this.extension = extension
    }
}

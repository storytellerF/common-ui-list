package com.storyteller_f.file_system.model

import android.net.Uri
import com.storyteller_f.file_system.instance.FileTime

class DirectoryItemModel(
    name: String,
    uri: Uri,
    isHidden: Boolean,
    isSymLink: Boolean,
    fileTime: FileTime,
) :
    FileSystemItemModel(
        name,
        uri,
        isHidden,
        isSymLink,
        fileTime,
    ) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}

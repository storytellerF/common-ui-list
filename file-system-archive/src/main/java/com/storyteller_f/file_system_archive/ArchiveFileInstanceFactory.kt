package com.storyteller_f.file_system_archive

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.FileSystemPrefix
import com.storyteller_f.file_system.decodeByBase64
import com.storyteller_f.file_system.encodeByBase64
import com.storyteller_f.file_system.instance.FileInstance

class ArchiveFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf("archive")

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance {
        return ArchiveFileInstance(context, uri)
    }

    override fun getPrefix(context: Context, uri: Uri): FileSystemPrefix {
        return FileSystemPrefix.Archive(uri.pathSegments.first().decodeByBase64())
    }

    override fun buildNestedFile(context: Context, name: String, fileInstance: FileInstance): Uri? {
        return if (fileInstance.extension == "zip") {
            val authority = fileInstance.uri.toString().encodeByBase64()
            Uri.Builder()
                .scheme("archive")
                .authority(authority)
                .path(name)
                .build()
        } else {
            null
        }
    }
}

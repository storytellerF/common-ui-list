package com.storyteller_f.file_system

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance

class LocalFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf(ContentResolver.SCHEME_FILE)

    override suspend fun buildInstance(context: Context, uri: Uri) =
        getLocalFileSystemInstance(context, uri)

    override fun getPrefix(context: Context, uri: Uri) =
        getLocalFileSystemPrefix(context, uri.path!!)
}

class DocumentFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf(ContentResolver.SCHEME_CONTENT)

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentLocalFileInstance(
                "/${uri.rawTree}",
                uri.authority!!,
                uri.tree,
                context,
                uri
            )
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }
    }
}

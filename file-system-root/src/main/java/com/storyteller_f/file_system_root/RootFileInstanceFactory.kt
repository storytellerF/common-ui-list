package com.storyteller_f.file_system_root

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory

class RootFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf(RootAccessFileInstance.ROOT_FILESYSTEM_SCHEME)

    override suspend fun buildInstance(context: Context, uri: Uri) =
        RootAccessFileInstance.buildInstance(uri)!!
}

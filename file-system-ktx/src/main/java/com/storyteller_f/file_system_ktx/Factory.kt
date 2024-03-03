package com.storyteller_f.file_system_ktx

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.getLocalFileInstance
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_remote.HttpFileInstance
import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.getRemoteInstance
import com.storyteller_f.file_system_root.RootAccessFileInstance

@Suppress("unused")
suspend fun getFileInstance(
    context: Context,
    uri: Uri,
): FileInstance {
    val scheme = uri.scheme!!
    return when {
        scheme == RootAccessFileInstance.ROOT_FILESYSTEM_SCHEME -> RootAccessFileInstance.instance(uri)!!
        RemoteAccessType.EXCLUDE_HTTP_PROTOCOL.contains(scheme) -> {
            getRemoteInstance(uri)
        }
        scheme == RemoteAccessType.HTTP || scheme == RemoteAccessType.HTTPS -> HttpFileInstance(context, uri)
        else -> getLocalFileInstance(context, uri)
    }
}

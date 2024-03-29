package com.storyteller_f.file_system_remote

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance

class RemoteFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = RemoteAccessType.ALL_PROTOCOL

    override suspend fun buildInstance(context: Context, uri: Uri): FileInstance {
        val scheme = uri.scheme!!
        return if (scheme == RemoteAccessType.HTTP || scheme == RemoteAccessType.HTTPS) {
            HttpFileInstance(context, uri)
        } else {
            getRemoteInstance(uri)
        }
    }
}

fun getRemoteInstance(uri: Uri): FileInstance {
    return when (uri.scheme) {
        RemoteAccessType.FTP -> FtpFileInstance(uri)
        RemoteAccessType.SMB -> SmbFileInstance(uri)
        RemoteAccessType.SFTP -> SFtpFileInstance(uri)
        RemoteAccessType.FTP_ES, RemoteAccessType.FTPS -> FtpsFileInstance(uri)
        RemoteAccessType.WEB_DAV -> WebDavFileInstance(uri)
        else -> throw Exception(uri.scheme)
    }
}

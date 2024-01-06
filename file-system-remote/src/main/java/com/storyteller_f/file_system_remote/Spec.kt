package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance

data class ShareSpec(
    val server: String,
    val port: Int,
    val user: String,
    val password: String,
    val type: String,
    val share: String
) {
    fun toUri(): Uri {
        return Uri.parse("$type://$user:$password@$server:$port/$share")!!
    }

    companion object {
        fun parse(uri: Uri): ShareSpec {
            val authority = uri.authority!!
            val split = authority.split("@")
            val (user, pass) = split.first().split(":")
            val (server, port) = split.last().split(":")
            return ShareSpec(
                server,
                port.toInt(),
                user,
                pass,
                uri.scheme!!,
                uri.path!!.substring(1)
            )
        }
    }
}

data class RemoteSpec(
    val server: String,
    val port: Int,
    val user: String,
    val password: String,
    val type: String
) {
    fun toUri(): Uri {
        val scheme = type
        return Uri.parse("$scheme://$user:$password@$server:$port/")!!
    }

    companion object {
        fun parse(parse: Uri): RemoteSpec {
            val scheme = parse.scheme!!
            val authority = parse.authority!!
            val (userConfig, serverConfig) = authority.split("@")
            val (user, pass) = userConfig.split(":")
            val (server, port) = serverConfig.split(":")
            return RemoteSpec(server, port.toInt(), user, pass, type = scheme)
        }
    }
}

val supportScheme = listOf(
    RemoteAccessType.FTP,
    RemoteAccessType.SMB,
    RemoteAccessType.SFTP,
    RemoteAccessType.FTP_ES,
    RemoteAccessType.FTPS,
    RemoteAccessType.WEB_DAV,
    "http",
    "https"
)

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

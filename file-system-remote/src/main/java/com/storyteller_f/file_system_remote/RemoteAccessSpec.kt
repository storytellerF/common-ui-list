package com.storyteller_f.file_system_remote

object RemoteAccessType {
    const val FTP = "ftp"
    const val SFTP = "sftp"
    const val SMB = "smb"
    const val FTP_ES = "ftp_es"
    const val FTPS = "ftps"
    const val WEB_DAV = "webdav"

    @Suppress("unused")
    val EXCLUDE_HTTP_PROTOCOL = listOf(SMB, SFTP, FTP, FTP_ES, FTPS, WEB_DAV)
}

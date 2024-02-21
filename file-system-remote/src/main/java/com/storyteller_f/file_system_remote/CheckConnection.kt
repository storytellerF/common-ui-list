package com.storyteller_f.file_system_remote

fun RemoteSpec.checkSFtpConnection() {
    SFtpInstance(this)
}

fun ShareSpec.checkSmbConnection() {
    requireDiskShare().close()
}

fun RemoteSpec.checkFtpConnection() {
    FtpInstance(this).open()
}

fun RemoteSpec.checkFtpEsConnection() {
    FtpsInstance(this).open()
}

fun RemoteSpec.checkFtpsConnection() {
    FtpsInstance(this).open()
}

fun RemoteSpec.checkWebDavConnection() {
    WebDavInstance(this).run {
        list(buildRelativePath("/"))
    }
}

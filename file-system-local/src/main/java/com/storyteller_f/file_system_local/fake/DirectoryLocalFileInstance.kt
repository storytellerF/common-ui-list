package com.storyteller_f.file_system_local.fake

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance

/**
 * 目录型，用于特殊类型
 */
abstract class DirectoryLocalFileInstance(uri: Uri) : FileInstance(uri) {
    suspend fun getFileLength(): Long = -1

    override suspend fun createFile() = false
}

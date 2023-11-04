package com.storyteller_f.file_system.model

import android.net.Uri
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*

open class FileSystemItemModel(
    name: String,
    uri: Uri,
    val isHidden: Boolean,
    val lastModifiedTime: Long,
    val isSymLink: Boolean,
) : FileSystemItemModelLite(name, uri) {
    private val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd hh:mm:ss sss", Locale.CHINA)
    val formattedLastModifiedTime: String
    var formattedLastAccessTime: String? = null
    var formattedCreatedTime: String? = null
    var lastAccessTime: Long = 0
        set(value) {
            formattedLastAccessTime = value.time
            field = value
        }
    var createdTime: Long = 0
        set(value) {
            formattedCreatedTime = value.time
            field = value
        }
    var size: Long = 0
    var formattedSize: String? = null
    var permissions: String? = null

    private val Long.time get() = simpleDateFormat.format(Date(this))

    fun editAccessTime(childFile: File) {
        val fileSystemItemModel = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val basicFileAttributes = Files.readAttributes(childFile.toPath(), BasicFileAttributes::class.java)
                fileSystemItemModel.createdTime = basicFileAttributes.creationTime().toMillis()
                fileSystemItemModel.lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
            } catch (_: IOException) {
                Log.w(TAG, "list: 获取BasicFileAttribute失败" + childFile.absolutePath)
            }
        }
    }

    init {
        formattedLastModifiedTime = lastModifiedTime.time
    }

    companion object {
        private const val TAG = "FileSystemItemModel"
    }
}

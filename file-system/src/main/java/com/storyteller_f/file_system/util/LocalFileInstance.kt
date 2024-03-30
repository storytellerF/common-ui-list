package com.storyteller_f.file_system.util

import android.os.Build
import com.storyteller_f.file_system.instance.FileTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

suspend fun File.fileTime(): FileTime {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val basicFileAttributes =
                withContext(Dispatchers.IO) {
                    Files.readAttributes(toPath(), BasicFileAttributes::class.java)
                }
            val createdTime = basicFileAttributes.creationTime().toMillis()
            val lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
            return FileTime(lastModified(), lastAccessTime, createdTime)
        } catch (_: IOException) {
        }
    }
    return FileTime(lastModified())
}

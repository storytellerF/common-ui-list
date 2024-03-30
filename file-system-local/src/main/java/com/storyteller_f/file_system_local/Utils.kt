package com.storyteller_f.file_system_local

import android.content.Context
import android.os.Build
import android.os.Process
import android.os.UserManager
import androidx.core.content.ContextCompat
import com.storyteller_f.file_system.buildPath

fun Context.getMyId() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    ContextCompat.getSystemService(this, UserManager::class.java)!!
        .getSerialNumberForUser(Process.myUserHandle())
} else {
    0L
}

@Suppress("unused")
fun Context.getCurrentUserEmulatedPath() =
    buildPath(
        LocalFileSystem.EMULATED_ROOT_PATH,
        getMyId().toString()
    )

fun Context.getCurrentUserDataPath() =
    buildPath(
        LocalFileSystem.USER_DATA,
        getMyId().toString()
    )

/**
 * /storage/XX44-XX55 或者是/storage/XX44-XX55/test。最终结果应该是/storage/XX44-XX55
 */
fun extractSdPath(path: String): String {
    var endIndex = path.indexOf("/", LocalFileSystem.STORAGE_PATH.length + 1)
    if (endIndex == -1) endIndex = path.length
    return path.substring(0, endIndex)
}

fun Context.appDataDir() = "${LocalFileSystem.DATA_SUB_DATA}/$packageName"

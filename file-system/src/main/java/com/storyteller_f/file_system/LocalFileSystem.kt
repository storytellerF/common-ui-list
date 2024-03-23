package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.LocalFileSystem.DATA
import com.storyteller_f.file_system.LocalFileSystem.DATA_SUB_DATA
import com.storyteller_f.file_system.LocalFileSystem.ROOT
import com.storyteller_f.file_system.LocalFileSystem.SDCARD
import com.storyteller_f.file_system.LocalFileSystem.SELF_PRIMARY
import com.storyteller_f.file_system.LocalFileSystem.USER_APP
import com.storyteller_f.file_system.LocalFileSystem.USER_DATA_FRONT_PATH
import com.storyteller_f.file_system.LocalFileSystem.USER_EMULATED_FRONT_PATH
import com.storyteller_f.file_system.instance.local.fake.getMyId
import com.storyteller_f.slim_ktx.substringAt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalFileSystem {
    const val ROOT = "/"

    @SuppressLint("SdCardPath")
    const val SDCARD = "/sdcard"

    const val STORAGE_PATH = "/storage"
    const val EMULATED_ROOT_PATH = "/storage/emulated"

    const val DATA = "/data"
    const val USER_DATA = "/data/user"

    @SuppressLint("SdCardPath")
    const val USER_DATA_FRONT_PATH = "/data/user/"
    const val USER_APP = "/data/app"
    const val USER_EMULATED_FRONT_PATH = "/storage/emulated/"
    const val DATA_SUB_DATA = "/data/data"

    const val ROOT_USER_EMULATED_PATH = "/storage/emulated/0"
    const val CURRENT_EMULATED_PATH = "/storage/self"
    const val SELF_PRIMARY = "/storage/self/primary"

    val publicPath = listOf("/system", "/mnt")
}

sealed class LocalFileSystemPrefix(val key: String) {

    /**
     * 公共目录，没有权限限制
     */
    data object Public : LocalFileSystemPrefix("public")

    /**
     * /sdcard 本身以及所有的子文件
     */
    data object SdCard : LocalFileSystemPrefix(SDCARD)

    /**
     * /storage/self 本身
     */
    data object Self : LocalFileSystemPrefix(LocalFileSystem.CURRENT_EMULATED_PATH)

    /**
     * /storage/self/primary 本身以及所有的子文件
     */
    data object SelfPrimary : LocalFileSystemPrefix(SELF_PRIMARY)

    /**
     * /storage/emulated/0 本身以及所有的子文件
     */
    class SelfEmulated(uid: Long) : LocalFileSystemPrefix("${USER_EMULATED_FRONT_PATH}$uid")

    /**
     * /storage/emulated 本身
     */
    data object EmulatedRoot : LocalFileSystemPrefix(LocalFileSystem.EMULATED_ROOT_PATH)

    /**
     * /storage 本身
     */
    data object Storage : LocalFileSystemPrefix(LocalFileSystem.STORAGE_PATH)

    /**
     * 外接存储设备，目录应该是/storage/emulated/XX44-XX55 类似的目录
     */
    class Mounted(key: String) : LocalFileSystemPrefix(key)

    /**
     * app 沙盒目录
     */
    class AppData(key: String) : LocalFileSystemPrefix(key)

    /**
     * 用户安装的app 路径/data/app
     */
    data object InstalledApps : LocalFileSystemPrefix(USER_APP)

    /**
     * 根目录本身
     */
    data object Root : LocalFileSystemPrefix(ROOT)

    /**
     * /data 本身
     */
    data object Data : LocalFileSystemPrefix(DATA)

    /**
     * /data/data 本身
     */
    data object Data2 : LocalFileSystemPrefix(DATA_SUB_DATA)

    /**
     * /data/user 本身
     */
    data object DataUser : LocalFileSystemPrefix(LocalFileSystem.USER_DATA)

    /**
     * /data/user/uid 本身
     */
    class SelfDataRoot(uid: Long) : LocalFileSystemPrefix("${USER_DATA_FRONT_PATH}$uid")

    class SelfPackage(uid: Long, packageName: String) :
        LocalFileSystemPrefix("${USER_DATA_FRONT_PATH}$uid/$packageName")
}

/**
 * 如果是根目录，返回空。
 */
fun getPrefix(
    context: Context,
    uri: Uri,
): LocalFileSystemPrefix? {
    val unsafePath = uri.path!!
    assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
        unsafePath
    }
    val path = simplePath(unsafePath)
    /**
     * 只有publicFileSystem 才会有prefix 的区别，其他的都不需要。
     */
    return when {
        uri.scheme!! != ContentResolver.SCHEME_FILE -> null
        else -> getPublicFileSystemPrefix(context, path)
    }
}

private fun getPublicFileSystemPrefix(context: Context, path: String): LocalFileSystemPrefix =
    when {
        LocalFileSystem.publicPath.any { path.startsWith(it) } -> LocalFileSystemPrefix.Public
        path.startsWith(LocalFileSystemPrefix.SdCard.key) -> LocalFileSystemPrefix.SdCard
        path.startsWith(context.appDataDir()) -> LocalFileSystemPrefix.AppData(context.appDataDir())
        path.startsWith(
            "${USER_DATA_FRONT_PATH}${context.getMyId()}/${context.packageName}"
        ) -> LocalFileSystemPrefix.SelfPackage(
            context.getMyId(),
            context.packageName
        )

        path.startsWith(USER_EMULATED_FRONT_PATH) -> LocalFileSystemPrefix.SelfEmulated(
            path.substring(
                USER_EMULATED_FRONT_PATH.length
            ).substringAt("/").toLong()
        )

        path == LocalFileSystem.CURRENT_EMULATED_PATH -> LocalFileSystemPrefix.Self
        path.startsWith(LocalFileSystem.CURRENT_EMULATED_PATH) -> LocalFileSystemPrefix.SelfPrimary
        path == LocalFileSystemPrefix.EmulatedRoot.key -> LocalFileSystemPrefix.EmulatedRoot
        path == LocalFileSystemPrefix.Storage.key -> LocalFileSystemPrefix.Storage
        path.startsWith(LocalFileSystem.STORAGE_PATH) -> LocalFileSystemPrefix.Mounted(
            extractSdPath(
                path
            )
        )

        path == LocalFileSystemPrefix.Root.key -> LocalFileSystemPrefix.Root
        path == LocalFileSystemPrefix.Data.key -> LocalFileSystemPrefix.Data
        path == LocalFileSystemPrefix.Data2.key -> LocalFileSystemPrefix.Data2
        path == LocalFileSystemPrefix.DataUser.key -> LocalFileSystemPrefix.DataUser
        path == "${USER_DATA_FRONT_PATH}${context.getMyId()}" -> LocalFileSystemPrefix.SelfDataRoot(context.getMyId())
        path.startsWith(LocalFileSystemPrefix.InstalledApps.key) -> LocalFileSystemPrefix.InstalledApps
        else -> throw Exception("unrecognized path $path")
    }

/**
 * /storage/XX44-XX55 或者是/storage/XX44-XX55/test。最终结果应该是/storage/XX44-XX55
 */
private fun extractSdPath(path: String): String {
    var endIndex = path.indexOf("/", LocalFileSystem.STORAGE_PATH.length + 1)
    if (endIndex == -1) endIndex = path.length
    return path.substring(0, endIndex)
}

private fun Context.appDataDir() = "${DATA_SUB_DATA}/$packageName"

suspend fun File.ensureFile(): File? {
    if (!exists()) {
        parentFile?.ensureDirs() ?: return null
        if (!withContext(Dispatchers.IO) {
                createNewFile()
            }) {
            return null
        }
    }
    return this
}

suspend fun File.ensureDirs(): File? {
    if (!exists()) {
        if (!withContext(Dispatchers.IO) {
                mkdirs()
            }) {
            return null
        }
    }
    return this
}

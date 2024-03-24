package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.LocalFileSystem.DATA
import com.storyteller_f.file_system.LocalFileSystem.DATA_SUB_DATA
import com.storyteller_f.file_system.LocalFileSystem.ROOT
import com.storyteller_f.file_system.LocalFileSystem.SDCARD
import com.storyteller_f.file_system.LocalFileSystem.SELF_PRIMARY
import com.storyteller_f.file_system.LocalFileSystem.USER_APP
import com.storyteller_f.file_system.LocalFileSystem.USER_DATA_FRONT_PATH
import com.storyteller_f.file_system.LocalFileSystem.USER_EMULATED_FRONT_PATH
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.AppLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.getMyId
import com.storyteller_f.slim_ktx.substringAt

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

sealed class FileSystemPrefix(val key: String) {

    /**
     * 公共目录，没有权限限制
     */
    data object Public : FileSystemPrefix("public")

    /**
     * /sdcard 本身以及所有的子文件
     */
    data object SdCard : FileSystemPrefix(SDCARD)

    /**
     * /storage/self 本身
     */
    data object Self : FileSystemPrefix(LocalFileSystem.CURRENT_EMULATED_PATH)

    /**
     * /storage/self/primary 本身以及所有的子文件
     */
    data object SelfPrimary : FileSystemPrefix(SELF_PRIMARY)

    /**
     * /storage/emulated/0 本身以及所有的子文件
     */
    class SelfEmulated(uid: Long) : FileSystemPrefix("${USER_EMULATED_FRONT_PATH}$uid")

    /**
     * /storage/emulated 本身
     */
    data object EmulatedRoot : FileSystemPrefix(LocalFileSystem.EMULATED_ROOT_PATH)

    /**
     * /storage 本身
     */
    data object Storage : FileSystemPrefix(LocalFileSystem.STORAGE_PATH)

    /**
     * 外接存储设备，目录应该是/storage/emulated/XX44-XX55 类似的目录
     */
    class Mounted(key: String) : FileSystemPrefix(key)

    /**
     * app 沙盒目录
     */
    class AppData(key: String) : FileSystemPrefix(key)

    /**
     * 用户安装的app 路径/data/app
     */
    data object InstalledApps : FileSystemPrefix(USER_APP)

    /**
     * 根目录本身
     */
    data object Root : FileSystemPrefix(ROOT)

    /**
     * /data 本身
     */
    data object Data : FileSystemPrefix(DATA)

    /**
     * /data/data 本身
     */
    data object Data2 : FileSystemPrefix(DATA_SUB_DATA)

    /**
     * /data/user 本身
     */
    data object DataUser : FileSystemPrefix(LocalFileSystem.USER_DATA)

    /**
     * /data/user/uid 本身
     */
    class SelfDataRoot(uid: Long) : FileSystemPrefix("${USER_DATA_FRONT_PATH}$uid")

    class SelfPackage(uid: Long, packageName: String) :
        FileSystemPrefix("${USER_DATA_FRONT_PATH}$uid/$packageName")

    /**
     * 代表压缩文件的prefix，key 是压缩文件的完整文件路径。
     * 不仅在LocalFileSystem 中有效，所有的文件系统都应该支持，只不过在其他类型的文件系统中，
     * 非压缩文件的prefix 都是null，这样在尝试获取压缩文件中的内容的时候会出现prefix 不同的情况，
     * 然后通过getFileInstance 直接根据路径获取。这样就要求所有的Factory 判断当前是不是压缩文件，如果是压缩文件，
     * 创建一个压缩文件的FileInstance，所以需要增加一个scheme 为archive。
     *
     * 比如路径为/test.zip/hello.txt，那么prefix 是Archive(/test.zip)。
     * 当前没有实际的处理
     */
    class Archive(key: String) : FileSystemPrefix(key)

    /**
     * 当前没有实际的处理
     */
    data object NoLocal : FileSystemPrefix("NoLocal")
}

fun getLocalFileSystemPrefix(context: Context, path: String): FileSystemPrefix =
    when {
        LocalFileSystem.publicPath.any { path.startsWith(it) } -> FileSystemPrefix.Public
        path.startsWith(FileSystemPrefix.SdCard.key) -> FileSystemPrefix.SdCard
        path.startsWith(context.appDataDir()) -> FileSystemPrefix.AppData(context.appDataDir())
        path.startsWith(
            "${USER_DATA_FRONT_PATH}${context.getMyId()}/${context.packageName}"
        ) -> FileSystemPrefix.SelfPackage(
            context.getMyId(),
            context.packageName
        )

        path.startsWith(USER_EMULATED_FRONT_PATH) -> FileSystemPrefix.SelfEmulated(
            path.substring(
                USER_EMULATED_FRONT_PATH.length
            ).substringAt("/").toLong()
        )

        path == LocalFileSystem.CURRENT_EMULATED_PATH -> FileSystemPrefix.Self
        path.startsWith(LocalFileSystem.CURRENT_EMULATED_PATH) -> FileSystemPrefix.SelfPrimary
        path == FileSystemPrefix.EmulatedRoot.key -> FileSystemPrefix.EmulatedRoot
        path == FileSystemPrefix.Storage.key -> FileSystemPrefix.Storage
        path.startsWith(LocalFileSystem.STORAGE_PATH) -> FileSystemPrefix.Mounted(extractSdPath(path))
        path == FileSystemPrefix.Root.key -> FileSystemPrefix.Root
        path == FileSystemPrefix.Data.key -> FileSystemPrefix.Data
        path == FileSystemPrefix.Data2.key -> FileSystemPrefix.Data2
        path == FileSystemPrefix.DataUser.key -> FileSystemPrefix.DataUser
        path == "${USER_DATA_FRONT_PATH}${context.getMyId()}" -> FileSystemPrefix.SelfDataRoot(
            context.getMyId()
        )

        path.startsWith(FileSystemPrefix.InstalledApps.key) -> FileSystemPrefix.InstalledApps
        else -> throw Exception("unrecognized path $path")
    }

suspend fun getLocalFileSystemInstance(context: Context, uri: Uri): FileInstance {
    assert(uri.scheme == ContentResolver.SCHEME_FILE) {
        "only permit local system $uri"
    }

    return when (val prefix = getFileSystemPrefix(context, uri)!!) {
        is FileSystemPrefix.AppData -> RegularLocalFileInstance(context, uri)
        FileSystemPrefix.Data -> FakeLocalFileInstance(context, uri)
        FileSystemPrefix.Data2 -> FakeLocalFileInstance(context, uri)
        is FileSystemPrefix.SelfDataRoot -> FakeLocalFileInstance(context, uri)
        is FileSystemPrefix.SelfPackage -> RegularLocalFileInstance(context, uri)
        FileSystemPrefix.DataUser -> FakeLocalFileInstance(context, uri)
        FileSystemPrefix.EmulatedRoot -> FakeLocalFileInstance(context, uri)
        FileSystemPrefix.InstalledApps -> AppLocalFileInstance(context, uri)
        is FileSystemPrefix.Mounted -> when {
            // 外接sd卡
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> RegularLocalFileInstance(
                context,
                uri
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> DocumentLocalFileInstance.getMounted(
                context,
                uri,
                prefix.key
            )

            Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> RegularLocalFileInstance(
                context,
                uri
            )

            else -> RegularLocalFileInstance(context, uri)
        }

        FileSystemPrefix.Public -> RegularLocalFileInstance(context, uri)
        FileSystemPrefix.Root -> FakeLocalFileInstance(context, uri)
        is FileSystemPrefix.SelfEmulated -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(
                context,
                uri,
                prefix.key
            )

            else -> RegularLocalFileInstance(context, uri)
        }

        FileSystemPrefix.SdCard -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        FileSystemPrefix.Self -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> FakeLocalFileInstance(context, uri)

            else -> RegularLocalFileInstance(context, uri)
        }

        FileSystemPrefix.SelfPrimary -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        FileSystemPrefix.Storage -> FakeLocalFileInstance(context, uri)
        is FileSystemPrefix.Archive -> TODO()
        is FileSystemPrefix.NoLocal -> TODO()
    }
}

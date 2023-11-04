package com.storyteller_f.file_system

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.common_ktx.substringAt
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.AppLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.getMyId
import java.io.File
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("HasPlatformType", "unused")
fun Context.getCurrentUserEmulatedPath() =
    File(LocalFileSystem.EMULATED_ROOT_PATH, getMyId().toString()).absolutePath

/**
 * 除非是根路径，否则最后一个字符不可以是/
 */
fun getLocalFileInstance(context: Context, uri: Uri): FileInstance {
    val unsafePath = uri.path!!
    assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
        "invalid path [$unsafePath]"
    }
    val path = simplePath(unsafePath)
    val scheme = uri.scheme!!
    val safeUri = uri.buildUpon().path(path).build()

    return when (scheme) {
        ContentResolver.SCHEME_CONTENT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentLocalFileInstance(
                "/${uri.rawTree}",
                uri.authority!!,
                uri.tree,
                context,
                safeUri
            )
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }

        else -> getPublicFileSystemInstance(context, safeUri)
    }
}

private fun getPublicFileSystemInstance(context: Context, uri: Uri): FileInstance {
    assert(uri.scheme == ContentResolver.SCHEME_FILE) {
        "only permit local system $uri"
    }

    return when (val prefix = getPrefix(context, uri)!!) {
        is LocalFileSystemPrefix.AppData -> RegularLocalFileInstance(context, uri)
        LocalFileSystemPrefix.Data -> FakeLocalFileInstance(context, uri)
        LocalFileSystemPrefix.Data2 -> FakeLocalFileInstance(context, uri)
        is LocalFileSystemPrefix.DataRootUser -> RegularLocalFileInstance(context, uri)
        LocalFileSystemPrefix.DataUser -> FakeLocalFileInstance(context, uri)
        LocalFileSystemPrefix.EmulatedRoot -> FakeLocalFileInstance(context, uri)
        LocalFileSystemPrefix.InstalledApps -> AppLocalFileInstance(context, uri)
        is LocalFileSystemPrefix.Mounted -> when {
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

        LocalFileSystemPrefix.Public -> RegularLocalFileInstance(context, uri)
        LocalFileSystemPrefix.Root -> FakeLocalFileInstance(context, uri)
        is LocalFileSystemPrefix.RootEmulated -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> DocumentLocalFileInstance.getEmulated(
                context,
                uri,
                prefix.key
            )

            else -> RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.SdCard -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.Self -> when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.Q -> {
                FakeLocalFileInstance(context, uri)
            }

            else -> RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.SelfPrimary -> if (Build.VERSION_CODES.Q == Build.VERSION.SDK_INT) {
            DocumentLocalFileInstance.getEmulated(context, uri, prefix.key)
        } else {
            RegularLocalFileInstance(context, uri)
        }

        LocalFileSystemPrefix.Storage -> FakeLocalFileInstance(context, uri)
    }
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

@SuppressLint("SdCardPath")
private fun getPublicFileSystemPrefix(context: Context, path: String): LocalFileSystemPrefix =
    when {
        LocalFileSystem.publicPath.any { path.startsWith(it) } -> LocalFileSystemPrefix.Public
        path.startsWith(LocalFileSystemPrefix.SdCard.key) -> LocalFileSystemPrefix.SdCard
        path.startsWith(context.appDataDir()) -> LocalFileSystemPrefix.AppData(context.appDataDir())
        path.startsWith(LocalFileSystem.USER_EMULATED_FRONT_PATH) -> LocalFileSystemPrefix.RootEmulated(
            path.substring(
                LocalFileSystem.USER_EMULATED_FRONT_PATH.length
            ).substringAt("/").toLong()
        )

        path == LocalFileSystem.CURRENT_EMULATED_PATH -> LocalFileSystemPrefix.Self
        path.startsWith(LocalFileSystem.CURRENT_EMULATED_PATH) -> LocalFileSystemPrefix.SelfPrimary
        path == LocalFileSystemPrefix.EmulatedRoot.key -> LocalFileSystemPrefix.EmulatedRoot
        path == LocalFileSystemPrefix.Storage.key -> LocalFileSystemPrefix.Storage
        path.startsWith(LocalFileSystem.STORAGE_PATH) -> LocalFileSystemPrefix.Mounted(extractSdPath(path))
        path == LocalFileSystemPrefix.Root.key -> LocalFileSystemPrefix.Root
        path == LocalFileSystemPrefix.Data.key -> LocalFileSystemPrefix.Data
        path.startsWith(LocalFileSystemPrefix.Data2.key) -> LocalFileSystemPrefix.Data2
        path == LocalFileSystemPrefix.DataUser.key -> LocalFileSystemPrefix.DataUser
        path.startsWith(LocalFileSystem.USER_DATA_FRONT_PATH) -> LocalFileSystemPrefix.DataRootUser(
            path.substring(
                LocalFileSystem.USER_DATA_FRONT_PATH.length
            ).substringAt("/").toLong()
        )

        path.startsWith(LocalFileSystemPrefix.InstalledApps.key) -> LocalFileSystemPrefix.InstalledApps
        else -> throw Exception("unrecognized path")
    }

/**
 * /storage/XX44-XX55 或者是/storage/XX44-XX55/test。最终结果应该是/storage/XX44-XX55
 */
private fun extractSdPath(path: String): String {
    var endIndex = path.indexOf("/", LocalFileSystem.STORAGE_PATH.length + 1)
    if (endIndex == -1) endIndex = path.length
    return path.substring(0, endIndex)
}

@SuppressLint("SdCardPath")
private fun Context.appDataDir() = "/data/data/$packageName"

@Throws(Exception::class)
suspend fun FileInstance.toChildEfficiently(
    context: Context,
    name: String,
    policy: FileCreatePolicy
): FileInstance {
    assert(name.last() != '/') {
        "$name is not a valid name"
    }
    if (name == ".") {
        return this
    }
    if (name == "..") {
        return this.toParentEfficiently(context)
    }
    val path = File(path, name).absolutePath
    val childUri = uri.buildUpon().path(path).build()

    val currentPrefix = getPrefix(context, uri)
    val childPrefix = getPrefix(context, childUri)
    return if (currentPrefix == childPrefix) {
        toChild(name, policy)!!
    } else {
        getLocalFileInstance(context, childUri)
    }
}

@Throws(Exception::class)
suspend fun FileInstance.toParentEfficiently(
    context: Context
): FileInstance {
    val parentPath = File(path).parent
    val parentUri = uri.buildUpon().path(parentPath).build()

    val parentPrefix = getPrefix(context, parentUri)
    val childPrefix = getPrefix(context, uri)
    return if (parentPrefix == childPrefix) {
        toParent()
    } else {
        getLocalFileInstance(context, parentUri)
    }
}

/**
 * 简化路径。
 */
fun simplePath(path: String): String {
    assert(path[0] == '/') {
        "$path is not valid"
    }
    val stack = LinkedList<String>()
    var position = 1
    stack.add("/")
    val nameStack = LinkedList<Char>()
    while (position < path.length) {
        val current = path[position++]
        checkPath(current, stack, nameStack)
    }
    val s = nameStack.joinToString("")
    if (s.isNotEmpty()) {
        if (s == "..") {
            if (stack.size > 1) {
                stack.removeLast()
                stack.removeLast()
            }
        } else if (s != ".") stack.add(s)
    }
    if (stack.size > 1 && stack.last == "/") stack.removeLast()
    return stack.joinToString("")
}

private fun checkPath(
    current: Char,
    stack: LinkedList<String>,
    nameStack: LinkedList<Char>
) {
    if (current != '/') {
        nameStack.add(current)
    } else if (stack.last != "/" || nameStack.size != 0) {
        val name = nameStack.joinToString("")
        nameStack.clear()
        when (name) {
            ".." -> {
                stack.removeLast()
                stack.removeLast() // 弹出上一个 name
            }

            "." -> {
                // 无效操作
            }

            else -> {
                stack.add(name)
                stack.add("/")
            }
        }
    }
}

val Uri.tree: String
    get() {
        return rawTree.decodeByBase64()
    }

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeByBase64() =
    Base64.decode(toByteArray()).decodeToString()

@OptIn(ExperimentalEncodingApi::class)
fun String.encodeByBase64() = Base64.encode(toByteArray())

val Uri.rawTree: String
    get() {
        assert(scheme == ContentResolver.SCHEME_CONTENT)
        return pathSegments.first()!!
    }

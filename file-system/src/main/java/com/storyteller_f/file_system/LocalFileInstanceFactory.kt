package com.storyteller_f.file_system

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.AppLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.getMyId
import com.storyteller_f.file_system.util.buildPath
import java.util.LinkedList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("unused")
fun Context.getCurrentUserEmulatedPath() =
    buildPath(LocalFileSystem.EMULATED_ROOT_PATH, getMyId().toString())

fun Context.getCurrentUserDataPath() =
    buildPath(LocalFileSystem.USER_DATA, getMyId().toString())

class LocalFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf(ContentResolver.SCHEME_FILE)

    override fun build(context: Context, uri: Uri) =
        getPublicFileSystemInstance(context, uri)
}

class DocumentFileInstanceFactory : FileInstanceFactory {
    override val scheme: List<String>
        get() = listOf(ContentResolver.SCHEME_CONTENT)

    override fun build(context: Context, uri: Uri): FileInstance {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentLocalFileInstance(
                "/${uri.rawTree}",
                uri.authority!!,
                uri.tree,
                context,
                uri
            )
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }
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
        is LocalFileSystemPrefix.SelfDataRoot -> FakeLocalFileInstance(context, uri)
        is LocalFileSystemPrefix.SelfPackage -> RegularLocalFileInstance(context, uri)
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
        is LocalFileSystemPrefix.SelfEmulated -> when (Build.VERSION.SDK_INT) {
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
            Build.VERSION_CODES.Q -> FakeLocalFileInstance(context, uri)

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

/**
 * 通过base64 解码还原原始的authority
 */
val Uri.tree: String
    get() {
        return rawTree.decodeByBase64()
    }

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeByBase64() =
    Base64.decode(toByteArray()).decodeToString()

@OptIn(ExperimentalEncodingApi::class)
fun String.encodeByBase64() = Base64.encode(toByteArray())

/**
 * 存储原始authority 的信息，使用base64 编码
 */
val Uri.rawTree: String
    get() {
        assert(scheme == ContentResolver.SCHEME_CONTENT)
        return pathSegments.first()!!
    }

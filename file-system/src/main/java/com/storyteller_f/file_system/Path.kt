package com.storyteller_f.file_system

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.local.fake.getMyId
import com.storyteller_f.file_system.util.buildPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("unused")
fun Context.getCurrentUserEmulatedPath() =
    buildPath(LocalFileSystem.EMULATED_ROOT_PATH, getMyId().toString())

fun Context.getCurrentUserDataPath() =
    buildPath(LocalFileSystem.USER_DATA, getMyId().toString())

/**
 * /storage/XX44-XX55 或者是/storage/XX44-XX55/test。最终结果应该是/storage/XX44-XX55
 */
fun extractSdPath(path: String): String {
    var endIndex = path.indexOf("/", LocalFileSystem.STORAGE_PATH.length + 1)
    if (endIndex == -1) endIndex = path.length
    return path.substring(0, endIndex)
}

fun Context.appDataDir() = "${LocalFileSystem.DATA_SUB_DATA}/$packageName"

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

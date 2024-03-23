package com.storyteller_f.file_system

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.util.buildPath
import com.storyteller_f.file_system.util.parentPath
import java.util.ServiceLoader

interface FileInstanceFactory {

    val scheme: List<String>
    fun build(context: Context, uri: Uri): FileInstance
}

@Suppress("unused")
suspend fun getFileInstance(
    context: Context,
    uri: Uri,
    policy: FileCreatePolicy = FileCreatePolicy.NotCreate
): FileInstance {
    val unsafePath = uri.path!!
    assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
        "invalid path [$unsafePath]"
    }
    val path = simplePath(unsafePath)
    val scheme = uri.scheme!!
    val safeUri = uri.buildUpon().path(path).build()
    val loader = ServiceLoader.load(FileInstanceFactory::class.java)
    return loader.first {
        it.scheme.contains(scheme)
    }!!.build(context, safeUri).apply {
        if (policy is FileCreatePolicy.Create && !exists()) {
            if (policy.isFile) {
                createFile()
            } else {
                createDirectory()
            }
        }
    }
}

/**
 * 会针对. 和.. 特殊路径进行处理。
 */
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
        return toParentEfficiently(context)
    }
    val path = buildPath(path, name)
    val childUri = uri.buildUpon().path(path).build()

    val currentPrefix = getPrefix(context, uri)
    val childPrefix = getPrefix(context, childUri)
    return if (currentPrefix == childPrefix) {
        toChild(name, policy)!!
    } else {
        getFileInstance(context, childUri, policy)
    }
}

@Throws(Exception::class)
suspend fun FileInstance.toParentEfficiently(
    context: Context
): FileInstance {
    val parentPath = parentPath(path)
    val parentUri = uri.buildUpon().path(parentPath).build()

    val parentPrefix = getPrefix(context, parentUri)
    val childPrefix = getPrefix(context, uri)
    return if (parentPrefix == childPrefix) {
        toParent()
    } else {
        getFileInstance(context, parentUri)
    }
}

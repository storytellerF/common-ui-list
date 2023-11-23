package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.FileInputStream
import java.io.FileOutputStream

val webdavInstances = mutableMapOf<ShareSpec, WebDavInstance>()

class WebDavFileInstance(private val spec: ShareSpec, uri: Uri) : FileInstance(uri) {

    private val instance = getWebDavInstance()
    override val path: String
        get() = super.path.substring(spec.share.length + 1).ifEmpty { "/" }

    override suspend fun filePermissions(): FilePermissions {
        TODO("Not yet implemented")
    }

    override suspend fun fileTime(): FileTime {
        TODO("Not yet implemented")
    }

    override suspend fun fileKind(): FileKind {
        TODO("Not yet implemented")
    }

    private fun getWebDavInstance(): WebDavInstance {
        return webdavInstances.getOrPut(spec) {
            WebDavInstance(spec)
        }
    }

    override suspend fun getFileLength(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        instance.list(path).forEach {
            val (file, child) = child(it.name)
            if (it.isDirectory) {
                directoryItems.add(
                    DirectoryItemModel(
                        it.name,
                        child,
                        isHidden = false,
                        isSymLink = false,
                        fileTime = FileTime()
                    )
                )
            } else {
                fileItems.add(
                    FileItemModel(
                        it.name,
                        child,
                        isHidden = false,
                        isSymLink = false,
                        extension = file.extension,
                        time = FileTime()
                    )
                )
            }
        }
    }

    override suspend fun isFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

class WebDavInstance(spec: ShareSpec) {
    private val baseUrl = "http://${spec.server}:${spec.port}/${spec.share}"
    private val instance = OkHttpSardine().apply {
        setCredentials(spec.user, spec.password)
    }

    fun list(path: String): MutableList<DavResource> = instance.list(baseUrl + path)
}

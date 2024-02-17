package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.thegrizzlylabs.sardineandroid.DavAcl
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

val webdavInstances = mutableMapOf<RemoteSpec, WebDavInstance>()

class WebDavFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {

    private var resources: DavResource? = null
    private var acl: DavAcl? = null

    private fun reconnectResourcesIfNeed(): DavResource {
        val r = resources
        if (r == null) {
            val resources1 = getWebDavInstance().resources(path)
            resources = resources1
            return resources1
        }
        return r
    }

    private fun reconnectAclIfNeed(): DavAcl {
        val r = acl
        if (r == null) {
            val resources1 = getWebDavInstance().acl(path)!!
            acl = resources1
            return resources1
        }
        return r
    }

    override suspend fun filePermissions() = reconnectAclIfNeed().filePermissions()

    private fun DavAcl.filePermissions(): FilePermissions {
        val granted = aces.flatMap { ace ->
            ace.granted
        }
        return FilePermissions.permissions(
            granted.contains("read"),
            granted.contains("write"),
            false
        )
    }

    override suspend fun fileTime() = reconnectResourcesIfNeed().let {
        FileTime(it.modified.time, null, it.creation.time)
    }

    override suspend fun fileKind(): FileKind {
        return reconnectResourcesIfNeed().let {
            FileKind.build(!it.isDirectory, false, isHidden = false, size = it.fileLength())
        }
    }

    private fun getWebDavInstance() = webdavInstances.getOrPut(spec) {
        WebDavInstance(spec)
    }

    override suspend fun getFileLength() = reconnectResourcesIfNeed().fileLength()

    override suspend fun getInputStream(): InputStream {
        return getWebDavInstance().getInputStream(path)!!
    }

    override suspend fun getOutputStream(): OutputStream {
        return super.getOutputStream()
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileInfo>,
        directoryItems: MutableList<FileInfo>,
    ) {
        getWebDavInstance().list(path).forEach {
            val fileName = it.name
            val child = childUri(fileName)
            val filePermissions = getWebDavInstance().acl(it.path)?.filePermissions()!!
            val fileTime = FileTime(it.modified.time, created = it.creation.time)
            if (it.isDirectory) {
                directoryItems.add(
                    FileInfo(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(isFile = false, isSymbolicLink = false, isHidden = false, 0),
                        filePermissions
                    )
                )
            } else {
                fileItems.add(
                    FileInfo(
                        fileName,
                        child,
                        fileTime,
                        FileKind.build(isFile = true, isSymbolicLink = false, isHidden = false, 0),
                        filePermissions,
                    )
                )
            }
        }
    }

    override suspend fun exists() = getWebDavInstance().exists(path)

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        getWebDavInstance().delete(path)
        return true
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

    override suspend fun createDirectory(): Boolean {
        getWebDavInstance().createDirectory(path)
        return true
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val new = childUri(name)
        return WebDavFileInstance(new, spec)
    }
}

class WebDavInstance(spec: RemoteSpec) {
    private val baseUrl = "http://${spec.server}:${spec.port}"
    private val instance = OkHttpSardine().apply {
        setCredentials(spec.user, spec.password)
    }

    fun list(path: String): MutableList<DavResource> = instance.list(buildPath(path))

    fun resources(path: String): DavResource {
        return instance.getResources(buildPath(path)).filterNotNull().first()
    }

    fun getInputStream(path: String): InputStream? {
        return instance.get(buildPath(path))
    }

    fun exists(path: String): Boolean {
        return instance.exists(buildPath(path))
    }

    fun delete(path: String) {
        instance.delete(buildPath(path))
    }

    fun createDirectory(path: String) {
        instance.createDirectory(buildPath(path))
    }

    fun acl(path: String): DavAcl? {
        return instance.getAcl(buildPath(path))
    }

    private fun buildPath(path: String) = baseUrl + path
}

fun DavResource.fileLength(): Long {
    return contentLength ?: 0
}

package com.storyteller_f.file_system_remote

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import com.storyteller_f.file_system.ensureFile
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class HttpFileInstance(context: Context, uri: Uri) : BaseContextFileInstance(context, uri) {
    /**
     * 保存到cache 目录
     */
    private lateinit var tempFile: File

    init {
        assert(uri.scheme == "http" || uri.scheme == "https")
    }

    private suspend fun createIfNeed(): File {
        if (::tempFile.isInitialized) {
            return tempFile
        } else {
            val okHttpClient = OkHttpClient()
            val execute =
                okHttpClient.newCall(Request.Builder().url(uri.toString()).build()).execute()
            if (!execute.isSuccessful) {
                throw Exception("$uri code is ${execute.code}")
            } else {
                val body = execute.body
                if (body == null) {
                    throw Exception("$uri body is empty")
                } else {
                    Log.i("TAG", "ensureFile: $body")
                    val file = createFile(execute).ensureFile()!!
                    file.write(body)
                    tempFile = file
                    return file
                }
            }
        }
    }

    private fun createFile(execute: Response): File {
        val contentDisposition = execute.header("Content-Disposition")
        val contentType = execute.header("content-type")
        val guessFileName =
            URLUtil.guessFileName(uri.toString(), contentDisposition, contentType)
        return File(
            context.cacheDir,
            "${System.currentTimeMillis()}/$guessFileName"
        )
    }

    override suspend fun filePermissions() = FilePermissions(createIfNeed().let {
        FilePermission(it.canRead(), it.canWrite(), it.canExecute())
    })

    override suspend fun fileTime(): FileTime {
        TODO("Not yet implemented")
    }

    override suspend fun getFileLength() = createIfNeed().length()

    override suspend fun getFileInputStream() = createIfNeed().inputStream()

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun isFile() = true

    override suspend fun exists() = createIfNeed().exists()

    override suspend fun isDirectory() = false

    override suspend fun deleteFileOrEmptyDirectory() = false

    override suspend fun rename(newName: String) = false

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize() = 0L

    override suspend fun createFile() = false

    override suspend fun isHidden() = false

    override suspend fun createDirectory() = false

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

private suspend fun File.write(body: ResponseBody) {
    body.source().use { int ->
        outputStream().channel.use { out ->
            val byteBuffer = ByteBuffer.allocateDirect(1024)
            while (int.read(byteBuffer) != -1) {
                yield()
                byteBuffer.flip()
                out.write(byteBuffer)
                byteBuffer.clear()
            }
        }
    }
}

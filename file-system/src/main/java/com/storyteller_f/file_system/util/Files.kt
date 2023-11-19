package com.storyteller_f.file_system.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.storyteller_f.file_system.LocalFileSystem
import com.storyteller_f.file_system.LocalFileSystemPrefix
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import java.io.File
import java.util.Locale
import java.util.Objects

fun buildFile(vararg part: String): File = File(part.fold("/") { acc, s ->
    File(acc, s).absolutePath
})

fun File.permissions(): String {
    val w = canWrite()
    val e = canExecute()
    val r = canRead()
    return permissions(r, w, e, isFile)
}

fun DocumentFile.permissions(): String {
    val w = canWrite()
    val r = canRead()
    return permissions(r, w, false, isFile)
}

@RequiresApi(api = Build.VERSION_CODES.N)
fun Context.getStorageVolume(): List<StorageVolume> {
    val storageManager = getSystemService(StorageManager::class.java)
    return storageManager.storageVolumes
}

fun Context.getStorageCompat(): List<File> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        getStorageVolume().map { storageVolume: StorageVolume ->
            val uuid = storageVolume.uuid
            File(LocalFileSystem.STORAGE_PATH, volumePathName(uuid))
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val externalFilesDirs = externalCacheDirs
        externalFilesDirs.map {
            val absolutePath = it.absolutePath
            val endIndex = absolutePath.indexOf("Android")
            val path = absolutePath.substring(0, endIndex)
            File(path)
        }
    } else {
        val file = File("/storage/")
        file.listFiles()?.toList() ?: return listOf()
    }
}

fun volumePathName(uuid: String?): String =
    Objects.requireNonNullElse(uuid, "emulated")

fun Activity.generateSAFRequestIntent(prefix: LocalFileSystemPrefix): Intent? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val sm = getSystemService(StorageManager::class.java)
        val volume = sm.getStorageVolume(File(prefix.key))
        if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return volume.createOpenDocumentTreeIntent()
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (prefix is LocalFileSystemPrefix.SelfEmulated) {
                val primary = DocumentsContract.buildRootUri(
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS_TREE
                )
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
            } else if (prefix is LocalFileSystemPrefix.Mounted) {
                val tree = DocumentLocalFileInstance.getMountedTree(prefix.key)
                val primary = DocumentsContract.buildRootUri(
                    DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                    tree
                )
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
            }
        }
        return intent
    }
    return null
}

fun getExtension(name: String): String? {
    val index = name.lastIndexOf('.')
    return if (index == -1) null else name.substring(index + 1)
}

@Suppress("DEPRECATION")
fun getSpace(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.availableBytes
    } else {
        stat.blockSize.toLong() * stat.availableBlocks.toLong()
    }
}

@Suppress("DEPRECATION")
fun getFree(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.freeBytes
    } else {
        stat.blockSize.toLong() * stat.freeBlocks.toLong()
    }
}

@Suppress("DEPRECATION")
fun getTotal(prefix: String?): Long {
    val stat = StatFs(prefix)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        stat.totalBytes
    } else {
        stat.blockSize.toLong() * stat.blockCount.toLong()
    }
}

fun permissions(r: Boolean, w: Boolean, e: Boolean, isFile: Boolean): String {
    return String.format(
        Locale.CHINA,
        "%c%c%c%c",
        if (isFile) '-' else 'd',
        if (r) 'r' else '-',
        if (w) 'w' else '-',
        if (e) 'e' else '-'
    )
}
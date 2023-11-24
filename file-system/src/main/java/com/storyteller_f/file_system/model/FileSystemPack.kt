package com.storyteller_f.file_system.model

class FileSystemPack(
    val files: MutableList<FileModel>,
    val directories: MutableList<DirectoryModel>
) {

    fun addFiles(fileItemModels: List<FileModel>?) {
        files.addAll(fileItemModels!!)
    }

    fun addDirectory(directoryItemModels: List<DirectoryModel>?) {
        directories.addAll(directoryItemModels!!)
    }

    fun destroy() {
        files.clear()
        directories.clear()
    }

    val count: Int
        get() = files.size + directories.size

    companion object {
        val EMPTY = FileSystemPack(mutableListOf(), mutableListOf())
    }
}

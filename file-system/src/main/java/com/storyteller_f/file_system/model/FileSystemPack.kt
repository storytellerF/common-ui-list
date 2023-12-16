package com.storyteller_f.file_system.model

class FileSystemPack(
    val files: MutableList<FileInfo>,
    val directories: MutableList<FileInfo>
) {

    fun addFiles(fileItemModels: List<FileInfo>) {
        files.addAll(fileItemModels)
    }

    fun addDirectory(directoryItemModels: List<FileInfo>) {
        directories.addAll(directoryItemModels)
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

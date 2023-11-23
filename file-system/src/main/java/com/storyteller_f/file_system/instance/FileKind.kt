package com.storyteller_f.file_system.instance

sealed class SymbolicLinkType(open val origin: String) {
    data class Soft(override val origin: String) : SymbolicLinkType(origin)
    data class Hard(override val origin: String) : SymbolicLinkType(origin)

    val isSoft get() = this is Soft
    val isHard get() = this is Hard
}

sealed class FileKind(open val linkType: SymbolicLinkType? = null) {
    data class File(override val linkType: SymbolicLinkType? = null) : FileKind(linkType)
    data class Directory(override val linkType: SymbolicLinkType? = null) : FileKind(linkType)

    val isFile get() = this is File
    val isDirectory get() = this is Directory

    companion object {
        fun build(isFile: Boolean, isSymbolicLink: Boolean): FileKind {
            val linkType = if (isSymbolicLink) SymbolicLinkType.Soft("") else null
            return if (isFile) FileKind.File(linkType) else FileKind.Directory(linkType)
        }
    }
}

package com.storyteller_f.file_system.instance

import com.storyteller_f.common_ktx.bit

class FilePermission(
    val readable: Boolean = false,
    val writable: Boolean = false,
    val executable: Boolean = false
) {
    companion object {
        fun fromMask(value: Int): FilePermission {
            return FilePermission(value.bit(1), value.bit(2), value.bit(4))
        }
    }
}

class FilePermissions(
    val userPermission: FilePermission,
    val groupPermission: FilePermission? = null,
    val othersPermission: FilePermission? = null
) {
    companion object {
        val USER_READABLE = FilePermissions(FilePermission(true))

        fun fromMask(value: Int): FilePermissions {
            val userPermission = FilePermission.fromMask(value.ushr(6).and(0x111))
            val groupPermission = FilePermission.fromMask(value.ushr(3).and(0x111))
            val othersPermission = FilePermission.fromMask(value.ushr(0).and(0x111))
            return FilePermissions(userPermission, groupPermission, othersPermission)
        }

        fun permissions(r: Boolean, w: Boolean, e: Boolean) =
            FilePermissions(FilePermission(r, w, e))
    }
}

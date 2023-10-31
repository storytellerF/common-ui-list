package com.storyteller_f.file_system

import android.annotation.SuppressLint

object LocalFileSystem {
    const val STORAGE_PATH = "/storage"
    const val EMULATED_ROOT_PATH = "/storage/emulated"

    @SuppressLint("SdCardPath")
    const val USER_DATA_FRONT_PATH = "/data/user/"
    const val USER_EMULATED_FRONT_PATH = "/storage/emulated/"

    const val ROOT_USER_EMULATED_PATH = "/storage/emulated/0"
    const val CURRENT_EMULATED_PATH = "/storage/self"

    val publicPath = listOf("/system", "/mnt")
}

sealed class LocalFileSystemPrefix(val key: String) {

    /**
     * 公共目录，没有权限限制
     */
    data object Public : LocalFileSystemPrefix("public")

    /**
     * /sdcard 本身以及所有的子文件
     */
    @SuppressLint("SdCardPath")
    data object SdCard : LocalFileSystemPrefix("/sdcard")

    /**
     * /storage/self 本身
     */
    data object Self : LocalFileSystemPrefix(LocalFileSystem.CURRENT_EMULATED_PATH)

    /**
     * /storage/self/primary 本身以及所有的子文件
     */
    data object SelfPrimary : LocalFileSystemPrefix("/storage/self/primary")

    /**
     * /storage/emulated/0 本身以及所有的子文件
     */
    class RootEmulated(uid: Long) : LocalFileSystemPrefix("/storage/emulated/$uid")

    /**
     * /storage/emulated 本身
     */
    data object EmulatedRoot : LocalFileSystemPrefix(LocalFileSystem.EMULATED_ROOT_PATH)

    /**
     * /storage 本身
     */
    data object Storage : LocalFileSystemPrefix(LocalFileSystem.STORAGE_PATH)

    /**
     * 外接存储设备
     */
    class Mounted(key: String) : LocalFileSystemPrefix(key)

    /**
     * app 沙盒目录
     */
    class AppData(key: String) : LocalFileSystemPrefix(key)

    /**
     * 用户安装的app
     */
    data object InstalledApps : LocalFileSystemPrefix("/data/app")

    data object Root : LocalFileSystemPrefix("/")

    /**
     * /data 本身
     */
    data object Data : LocalFileSystemPrefix("/data")

    /**
     * /data/data 本身
     */
    data object Data2 : LocalFileSystemPrefix("/data/data")

    /**
     * /data/user
     */
    data object DataUser : LocalFileSystemPrefix("/data/user")

    @SuppressLint("SdCardPath")
    class DataRootUser(uid: Long) : LocalFileSystemPrefix("/data/user/$uid")
}

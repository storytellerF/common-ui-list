package com.storyteller_f.ui_list.core

import com.storyteller_f.ui_list.database.RemoteKey

interface Datum<RK : RemoteKey> : Model {
    fun remoteKey(prevKey: Int?, nextKey: Int?): RK
    fun remoteKeyId(): String = commonId()
}

interface Model {
    fun commonId(): String

    /**
     * 用于object pool的标识
     */
    fun uniqueIdInOP() = commonId()
}

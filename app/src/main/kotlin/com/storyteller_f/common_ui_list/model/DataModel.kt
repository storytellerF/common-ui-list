package com.storyteller_f.common_ui_list.model

import com.storyteller_f.common_ui_list.db.RemoteKey

interface Model {
    fun commonId(): String

    /**
     * 用于object pool的标识
     */
    fun uniqueIdInOP() = commonId()
}

interface Datum<RK : RemoteKey> : Model {
    fun remoteKey(prevKey: Int?, nextKey: Int?): RK
    fun remoteKeyId(): String = commonId()
}

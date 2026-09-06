package com.storyteller_f.ui_list.core

internal class IndexManager<Key> {
    private var current = 0

    private val map = mutableMapOf<Key, Int>()
    private val list = mutableListOf<Key>()

    fun getIndex(key: Key): Int {
        return map.getOrPut(key) {
            list.add(key)
            current++
        }
    }

    fun getKey(index: Int): Key = list[index]
}

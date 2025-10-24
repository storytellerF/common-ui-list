package com.storyteller_f.slim_ktx

class IndexManager<Key> {
    private var current = 0

    val map = mutableMapOf<Key, Int>()
    val list = mutableListOf<Key>()

    fun getIndex(key: Key): Int {
        return map.getOrPut(key) {
            list.add(key)
            current++
        }
    }

    fun getKey(index: Int): Key {
        return list[index]
    }
}

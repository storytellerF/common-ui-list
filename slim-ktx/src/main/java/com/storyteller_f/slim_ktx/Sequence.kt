package com.storyteller_f.slim_ktx

/**
 * 生成双层map
 */
inline fun <T, K1, K2, V> Sequence<T>.nestedGroupBy(
    doubleKeySelector: (T) -> Pair<K1, K2>?,
    valueTransform: (T) -> V
): Map<K1, Map<K2, List<V>>> {
    val destination = mutableMapOf<K1, MutableMap<K2, MutableList<V>>>()
    for (element in this) {
        val key = doubleKeySelector(element)
        key?.let {
            val map = destination.getOrPut(key.first) { mutableMapOf() }
            val secondMap = map.getOrPut(key.second) {
                mutableListOf()
            }
            secondMap.add(valueTransform(element))
        }
    }
    return destination
}

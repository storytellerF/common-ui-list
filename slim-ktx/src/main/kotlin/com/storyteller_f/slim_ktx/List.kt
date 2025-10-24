package com.storyteller_f.slim_ktx

fun <T> MutableList<T>.toggle(t: T) {
    if (contains(t)) remove(t) else add(t)
}

fun <T> List<T>.same(list: List<T>): Boolean {
    if (size != list.size) return false
    return sameInternal(list)
}

private fun <T> List<T>.sameInternal(list: List<T>): Boolean {
    forEachIndexed { index, t ->
        if (list[index] != t) {
            return false
        }
    }
    return true
}

fun <E> List<E>.dup(): Boolean {
    for (i in indices) {
        val e = get(i)
        if (subList(i + 1, size).any {
                it == e
            }) {
            return true
        }
    }
    return false
}

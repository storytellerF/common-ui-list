package com.storyteller_f.common_ktx

fun String.substringAt(s: String): String {
    val indexOf = indexOf(s)
    return if (indexOf >= 0) substring(0, indexOf) else this
}

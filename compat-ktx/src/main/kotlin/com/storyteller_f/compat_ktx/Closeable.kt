package com.storyteller_f.compat_ktx

fun <T, R> T.useCompat(close: T.() -> Unit, block: (T) -> R): R {
    return try {
        block(this)
    } finally {
        close()
    }
}

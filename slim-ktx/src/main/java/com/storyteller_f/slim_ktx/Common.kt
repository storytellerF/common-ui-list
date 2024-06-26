package com.storyteller_f.slim_ktx

fun Int.bit(bit: Int) = this and bit > 0
fun Long.bit(bit: Int) = this and bit.toLong() > 0

val Throwable.exceptionMessage get() = localizedMessage ?: message ?: javaClass.toString()

fun buildMask(block: (MutableList<Int>) -> Unit) = buildList(block).fold(0) { acc, i ->
    acc or i
}

inline fun <reified T : Any> T.propertiesSame(any: Any?, vararg properties: T.() -> Any?): Boolean {
    return if (any is T) {
        properties.all { property ->
            property(this) == property(any)
        }
    } else {
        false
    }
}

inline fun and(block1: () -> Boolean, block2: () -> Boolean): Boolean {
    return block1() && block2()
}

inline fun or(block1: () -> Boolean, block2: () -> Boolean): Boolean {
    return block1() || block2()
}

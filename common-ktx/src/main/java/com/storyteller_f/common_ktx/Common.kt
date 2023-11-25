package com.storyteller_f.common_ktx

fun Int.bit(bit: Int) = this and bit > 0
fun Long.bit(bit: Int) = this and bit.toLong() > 0

val Throwable.exceptionMessage get() = localizedMessage ?: javaClass.toString()

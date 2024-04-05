package com.storyteller_f.slim_ktx

import kotlin.reflect.safeCast

inline fun <reified T : Any> T.cast(a: Any) = this::class.safeCast(a)

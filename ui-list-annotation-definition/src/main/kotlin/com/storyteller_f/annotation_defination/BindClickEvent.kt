package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

/** Int callback parameters receive the current bindingAdapterPosition; invalid positions are ignored. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

/** Int callback parameters receive the current bindingAdapterPosition; invalid positions are ignored. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

/**
 * Callback parameters are resolved by name in declaration order: bindingAdapterPosition,
 * absoluteAdapterPosition, viewholder and view. Position parameters must be Int; invalid positions are ignored.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

/**
 * Callback parameters are resolved by name in declaration order: bindingAdapterPosition,
 * absoluteAdapterPosition, viewholder and view. Position parameters must be Int; invalid positions are ignored.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

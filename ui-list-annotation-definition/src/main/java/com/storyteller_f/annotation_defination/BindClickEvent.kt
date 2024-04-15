package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
)

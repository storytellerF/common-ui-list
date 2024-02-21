package com.storyteller_f.annotation_defination

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
/**
 * @param group 设置事件时，仅允许指定adapter 中View Holder 的事件
 */
annotation class BindClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
    val group: String = "default",
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class BindLongClickEvent(
    @Suppress("unused") val kClass: KClass<out Any>,
    val viewName: String = "root",
    val group: String = "default",
)

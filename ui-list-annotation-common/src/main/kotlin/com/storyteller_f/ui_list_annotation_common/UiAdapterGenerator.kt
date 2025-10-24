package com.storyteller_f.ui_list_annotation_common

abstract class UiAdapterGenerator<T> {
    /**
     * 用于添加到列表中
     */
    abstract fun buildAddFunction(entry: List<Entry<T>>): String

    companion object {
        const val CLASS_NAME = "HolderBuilder"
        val commonImports = listOf(
            "com.storyteller_f.ui_list.core.AbstractViewHolder",
            "android.view.LayoutInflater",
            "android.view.ViewGroup",
            "com.storyteller_f.ui_list.core.BuildBatch",
            "com.storyteller_f.ui_list.core.DataItemHolder",
            "com.storyteller_f.ui_list.event.findFragmentOrNull",
            "kotlin.reflect.KClass"
        )
    }
}

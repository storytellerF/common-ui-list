package com.storyteller_f.ui_list.core

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

/** Resolves data using a position relative to this adapter. */
interface ItemHolderProvider<out IH : DataItemHolder> {
    fun getItemHolder(position: Int): IH?
}

/** Resolves a position in this adapter, including nested ConcatAdapter offsets. */
fun RecyclerView.Adapter<*>.getItemHolderAt(position: Int): DataItemHolder? {
    if (position !in 0 until itemCount) return null
    return when (this) {
        is ItemHolderProvider<*> -> getItemHolder(position)
        is ConcatAdapter -> {
            val wrapped = getWrappedAdapterAndPosition(position)
            wrapped.first.getItemHolderAt(wrapped.second)
        }
        else -> null
    }
}

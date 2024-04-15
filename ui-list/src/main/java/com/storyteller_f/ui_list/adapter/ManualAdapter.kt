package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util

@Suppress("UNCHECKED_CAST")
class ManualAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>> :
    ListAdapter<IH, VH>(common_diff_util as DiffUtil.ItemCallback<IH>) {
    private val proxy = DefaultAdapter<IH, VH>().apply {
        target = this@ManualAdapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        proxy.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = proxy.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) =
        proxy.onBindViewHolder(holder, position)

    override fun onViewAttachedToWindow(holder: VH) = proxy.onViewAttachedToWindow(holder)

    override fun onViewDetachedFromWindow(holder: VH) = proxy.onViewDetachedFromWindow(holder)

    override fun onViewRecycled(holder: VH) = proxy.onViewRecycled(holder)
}

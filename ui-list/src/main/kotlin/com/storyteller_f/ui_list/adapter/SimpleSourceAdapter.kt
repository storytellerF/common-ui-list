package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
open class SimpleSourceAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(
    localCenter: Map<KClass<out DataItemHolder>, BuildBatch>? = null
) :
    PagingDataAdapter<IH, VH>(
        common_diff_util as DiffUtil.ItemCallback<IH>
    ) {
    private val proxy = object : DefaultAdapter<IH, VH>(localCenter) {
        override fun getItemAbstract(position: Int): IH? = getItem(position)
    }.apply {
        target = this@SimpleSourceAdapter
    }

    override fun onBindViewHolder(holder: VH, position: Int) =
        proxy.onBindViewHolder(holder, position)

    override fun getItemViewType(position: Int) = proxy.getItemViewType(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        proxy.onCreateViewHolder(parent, viewType)

    override fun onViewAttachedToWindow(holder: VH) = proxy.onViewAttachedToWindow(holder)

    override fun onViewDetachedFromWindow(holder: VH) = proxy.onViewDetachedFromWindow(holder)

    override fun onViewRecycled(holder: VH) = proxy.onViewRecycled(holder)
}

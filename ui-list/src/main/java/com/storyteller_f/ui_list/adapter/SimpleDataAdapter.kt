package com.storyteller_f.ui_list.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.DefaultAdapter
import com.storyteller_f.ui_list.core.DefaultAdapter.Companion.common_diff_util
import com.storyteller_f.ui_list.source.SimpleDataViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

/**
 * 支持排序，需要搭配SimpleDataViewModel和SimpleDataRepository
 */
@Suppress("UNCHECKED_CAST")
class SimpleDataAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(
    localCenter: Map<KClass<out DataItemHolder>, BuildBatch>? = null
) :
    ListAdapter<IH, VH>(common_diff_util as DiffUtil.ItemCallback<IH>) {

    private var fatData: SimpleDataViewModel.FatData<*, IH, *>? = null
    private val proxy = DefaultAdapter<IH, VH>(localCenter).apply {
        target = this@SimpleDataAdapter
    }
    private val skipNext = AtomicBoolean(false)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        proxy.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int) = proxy.getItemViewType(position)

    override fun onBindViewHolder(holder: VH, position: Int) =
        proxy.onBindViewHolder(holder, position)

    override fun onViewAttachedToWindow(holder: VH) = proxy.onViewAttachedToWindow(holder)

    override fun onViewDetachedFromWindow(holder: VH) = proxy.onViewDetachedFromWindow(holder)

    override fun onViewRecycled(holder: VH) = proxy.onViewRecycled(holder)

    fun submitData(fatData: SimpleDataViewModel.FatData<*, IH, *>) {
        if (skipNext.compareAndSet(true, false)) return
        this.fatData = fatData
        submitList(fatData.list)
    }

    fun swap(from: Int, to: Int) {
        skipNext.set(true)
        fatData?.swap(from, to)
        notifyItemMoved(from, to)
    }
}

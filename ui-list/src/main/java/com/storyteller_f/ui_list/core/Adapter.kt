@file:Suppress("UNCHECKED_CAST")

package com.storyteller_f.ui_list.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

typealias BuildViewHolderFunction = (ViewGroup, String) -> AbstractViewHolder<out DataItemHolder>

data class VariantBuildViewHolderFunction(val variant: String, val functionPosition: Int)

/**
 * 不可以手动直接修改
 */
val list = mutableListOf<BuildViewHolderFunction>()

/**
 * 不直接存储BuildViewHolderFunction，而是存储BuildViewHolderFunction 的索引
 */
val secondList = mutableListOf<VariantBuildViewHolderFunction>()

/**
 * value 存储指向list 中元素的索引。且作为viewType
 */
val registerCenter = mutableMapOf<Class<out DataItemHolder>, Int>()

/**
 * value 作为viewType，为了确认指定的viewType 是存储在secondRegisterCenter 中，增加了偏移，偏移值是list.size
 */
val secondRegisterCenter = mutableMapOf<SecondRegisterKey, Int>()

data class SecondRegisterKey(val clazz: Class<out DataItemHolder>, val variant: String)

fun holders(vararg blocks: (Int) -> Int) {
    blocks.fold(0) { acc, block ->
        acc + block(acc)
    }
}

abstract class DataItemHolder(val variant: String = "") {

    /**
     * 可以直接进行强制类型转换，无需判断
     */
    abstract fun areItemsTheSame(other: DataItemHolder): Boolean
    open fun areContentsTheSame(other: DataItemHolder): Boolean = this == other
}

abstract class AbstractViewHolder<IH : DataItemHolder>(val view: View) :
    RecyclerView.ViewHolder(view) {
    val context: Context = view.context
    private var _holderLifecycleOwner: BindLifecycleOwner? = null
    val holderLifecycleOwner: LifecycleOwner
        get() = _holderLifecycleOwner!!

    private var _itemHolder: IH? = null

    /**
     * 所属的group
     */
    lateinit var grouped: String

    // 需要保证当前已经绑定过数据了
    val itemHolder get() = _itemHolder!!

    fun onBind(itemHolder: IH) {
        bindData(itemHolder)
    }

    abstract fun bindData(itemHolder: IH)

    fun getColor(@ColorRes id: Int) = ContextCompat.getColor(context, id)

    fun getDrawable(@DrawableRes id: Int) = ContextCompat.getDrawable(context, id)

    fun getDimen(@DimenRes id: Int) = context.resources.getDimension(id)

    fun getString(@StringRes id: Int) = context.resources.getString(id)

    internal fun moveStateToCreate() {
        assert(_holderLifecycleOwner == null)
        _holderLifecycleOwner = BindLifecycleOwner()
        moveToState(Lifecycle.Event.ON_CREATE)
    }

    internal fun attachItemHolder(itemHolder: IH) {
        _itemHolder = itemHolder
    }

    internal fun detachItemHolder() {
        _itemHolder = null
    }

    inner class BindLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry
    }

    internal fun moveStateToResume() {
        val event = Lifecycle.Event.ON_RESUME
        moveToState(event)
    }

    internal fun moveStateToPause() {
        moveToState(Lifecycle.Event.ON_PAUSE)
    }

    internal fun moveStateToDestroy() {
        moveToState(Lifecycle.Event.ON_DESTROY)
        _holderLifecycleOwner = null
    }

    private fun moveToState(event: Lifecycle.Event) {
        val lifecycleOwner = _holderLifecycleOwner
        require(lifecycleOwner != null)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(event)
    }
}

abstract class BindingViewHolder<IH : DataItemHolder>(binding: ViewBinding) :
    AbstractViewHolder<IH>(binding.root)

open class DefaultAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(private val group: String? = null) :
    RecyclerView.Adapter<VH>() {
    lateinit var target: RecyclerView.Adapter<VH>
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val size = list.size
        val viewHolder = if (viewType >= size) {
            val (variant, functionPosition) = secondList[viewType - size]
            list[functionPosition](parent, variant)
        } else {
            list[viewType](parent, "")
        }
        return (viewHolder as VH).apply {
            grouped = group ?: "default"
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val itemHolder = getItemAbstract(position) as IH
        holder.attachItemHolder(itemHolder)
        holder.moveStateToCreate()
        holder.onBind(itemHolder)
    }

    protected open fun getItemAbstract(position: Int): IH? {
        return if (target is ListAdapter<*, *>) {
            (target as ListAdapter<IH, VH>).currentList[position] as IH
        } else {
            throw NotImplementedError("${target::class.java.canonicalName}无法获取对应item holder")
        }
    }

    override fun getItemCount() = 0

    override fun getItemViewType(position: Int): Int {
        val item = getItemAbstract(position) ?: return super.getItemViewType(position)
        val ihClass = item::class.java
        return getType(ihClass, item)
            ?: throw Exception("${ihClass.canonicalName} not found.registerCenter count: ${registerCenter.size}")
    }

    override fun onViewAttachedToWindow(holder: VH) {
        super.onViewAttachedToWindow(holder)
        holder.moveStateToResume()
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.moveStateToPause()
    }

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        holder.moveStateToDestroy()
        holder.detachItemHolder()
    }

    private fun getType(ihClass: Class<out IH>, item: IH): Int? {
        val functionPosition = registerCenter[ihClass] ?: return null
        return if (item.variant.isNotEmpty()) {
            val secondRegisterKey = SecondRegisterKey(ihClass, item.variant)
            secondRegisterCenter.getOrPut(secondRegisterKey) {
                secondList.add(
                    VariantBuildViewHolderFunction(
                        secondRegisterKey.variant,
                        functionPosition
                    )
                )
                (secondList.size - 1) + list.size
            }
        } else {
            functionPosition
        }
    }

    companion object {
        val common_diff_util = object : DiffUtil.ItemCallback<DataItemHolder>() {
            override fun areItemsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean {
                return when {
                    oldItem === newItem -> true
                    oldItem.javaClass == newItem.javaClass && oldItem.variant == newItem.variant -> {
                        oldItem.areItemsTheSame(newItem)
                    }

                    else -> false
                }
            }

            override fun areContentsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean =
                oldItem.areContentsTheSame(newItem)
        }
    }
}

@file:Suppress("UNCHECKED_CAST")

package com.storyteller_f.ui_list.core

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.storyteller_f.ui_list.event.findActivityOrNull
import com.storyteller_f.ui_list.event.findFragmentOrNull

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

abstract class AbstractViewHolder<IH : DataItemHolder>(itemView: View) :
    RecyclerView.ViewHolder(itemView) {
    val context: Context = itemView.context

    private val _holderLifecycleOwnerLiveData = MutableLiveData<BindLifecycleOwner?>()
    val holderLifecycleLiveData: LiveData<BindLifecycleOwner?> by ::_holderLifecycleOwnerLiveData

    val holderLifecycleOwner: LifecycleOwner
        get() = _holderLifecycleOwnerLiveData.value!!

    private var _itemHolder: IH? = null

    /**
     * 所属的group
     */
    lateinit var grouped: String

    // 需要保证当前已经绑定过数据了
    val itemHolder get() = _itemHolder!!

    private var _observer: LifecycleObserver? = null

    fun onBind(itemHolder: IH) {
        bindData(itemHolder)
    }

    abstract fun bindData(itemHolder: IH)

    fun getColor(@ColorRes id: Int) = ContextCompat.getColor(context, id)

    fun getDrawable(@DrawableRes id: Int) = ContextCompat.getDrawable(context, id)

    fun getDimen(@DimenRes id: Int) = context.resources.getDimension(id)

    fun getString(@StringRes id: Int) = context.resources.getString(id)
    internal fun attachItemHolder(itemHolder: IH) {
        _itemHolder = itemHolder
    }

    internal fun detachItemHolder() {
        _itemHolder = null
    }

    /**
     * onViewAttachedToWindow 被触发或者外部生命周期onStart 触发
     */
    internal fun moveStateToStart(resetObserverIfNeed: Boolean = true) {
        moveToState(Lifecycle.Event.ON_START)
        // 开始监听外部LifecycleOwner
        val owner: LifecycleOwner? = itemView.findFragmentOrNull<Fragment>()
            ?: itemView.findActivityOrNull() as? ComponentActivity
        requireNotNull(owner)
        // 确保外部生命周期引起变化时，不会导致再次注册observer，仅在onViewAttachedToWindow 中才会注册
        if (_observer == null || resetObserverIfNeed) {
            _observer?.let {
                owner.lifecycle.removeObserver(it)
            }
            val observer = BindLifecycleObserver()
            owner.lifecycle.addObserver(observer)
            _observer = observer
        }
    }

    /**
     * onViewDetachedFromWindow 被触发或者外部生命周期触发onStop
     *
     * onViewDetachedFromWindow 会通过onViewAttachedToWindow 恢复
     * 外部生命周期会通过再次变成onStart 恢复。如果外部生命周期来自Fragment，监听Fragment 本身而不是viewLifecycleOwner
     * 确保在Fragment 恢复的时候可以获得onStart 事件
     * 两者都使用moveStateToStart 恢复状态
     */
    internal fun moveStateToStop() {
        moveToState(Lifecycle.Event.ON_STOP)
        // 不会关闭监听LifecycleOwner，为了能够获得onStart 的回调
    }

    /**
     * 完全通过外部生命周期确定
     */
    internal fun moveStateToResume() {
        moveToState(Lifecycle.Event.ON_RESUME)
    }

    /**
     * 完全通过外部生命周期确定
     */
    internal fun moveStateToPause() {
        moveToState(Lifecycle.Event.ON_PAUSE)
    }

    internal fun moveStateToCreate() {
        assert(_holderLifecycleOwnerLiveData.value == null)
        _holderLifecycleOwnerLiveData.value = BindLifecycleOwner()
        moveToState(Lifecycle.Event.ON_CREATE)
    }

    internal fun moveStateToDestroy() {
        moveToState(Lifecycle.Event.ON_DESTROY)
        _holderLifecycleOwnerLiveData.value = null
    }

    private fun moveToState(event: Lifecycle.Event) {
        val lifecycleOwner = _holderLifecycleOwnerLiveData.value
        require(lifecycleOwner != null)
        lifecycleOwner.lifecycleRegistry.handleLifecycleEvent(event)
    }

    /**
     * 切换到下一个页面或者熄灭屏幕不会触发RecycleView 事件，需要手动监听Activity 或者Fragment 的生命周期
     */
    inner class BindLifecycleObserver : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            moveStateToStart(false)
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            moveStateToStop()
        }

        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            moveStateToResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            moveStateToPause()
        }
    }

    inner class BindLifecycleOwner : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle = lifecycleRegistry
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
        holder.moveStateToStart()
    }

    override fun onViewDetachedFromWindow(holder: VH) {
        super.onViewDetachedFromWindow(holder)
        holder.moveStateToStop()
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

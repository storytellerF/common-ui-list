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
import com.storyteller_f.slim_ktx.IndexManager
import com.storyteller_f.ui_list.event.findActivityOrNull
import com.storyteller_f.ui_list.event.findFragmentOrNull
import kotlin.reflect.KClass

typealias BuildViewHolderFunction2 = (ViewGroup, String) -> AbstractViewHolder<out DataItemHolder>
typealias BuildViewHolderFunction3 = (ViewGroup, String, String) -> AbstractViewHolder<out DataItemHolder>

data class ViewHolderKey(
    val itemHolderClass: KClass<out DataItemHolder>,
    val type: String,
    val key: String
)

class BuildBatch(val b2: BuildViewHolderFunction2? = null, val b3: BuildViewHolderFunction3? = null)

/**
 * 外部只能通过holders 修改
 */
internal val registerCenter = mutableMapOf<KClass<out DataItemHolder>, BuildBatch>()

fun holders(vararg blocks: (MutableMap<KClass<out DataItemHolder>, BuildBatch>) -> Unit) {
    blocks.forEach {
        it(registerCenter)
    }
}

/**
 * @param type 和注册ViewHolder 的时候的type 保持一致
 * @param key 会在ViewHolder 初始化的时候传入，以达到ItemHolder 和ViewHolder 绑定的作用
 */
abstract class DataItemHolder(val type: String = "", val key: String = "") {

    /**
     * 可以直接进行强制类型转换，无需判断
     */
    abstract fun areItemsTheSame(other: DataItemHolder): Boolean

    /**
     * 具有默认实现。只要子类使用data 标识符或者继承equals 即可。
     */
    open fun areContentsTheSame(other: DataItemHolder): Boolean = this == other
}

abstract class AbstractViewHolder<IH : DataItemHolder>(itemView: View, val key: String = "") :
    RecyclerView.ViewHolder(itemView) {
    val context: Context = itemView.context

    private val _holderLifecycleOwnerLiveData = MutableLiveData<BindLifecycleOwner?>()
    val holderLifecycleLiveData: LiveData<BindLifecycleOwner?> by ::_holderLifecycleOwnerLiveData

    @Suppress("MemberVisibilityCanBePrivate")
    val holderLifecycleOwner: LifecycleOwner
        get() = _holderLifecycleOwnerLiveData.value!!

    private var _itemHolder: IH? = null

    /**
     * 需要保证当前已经绑定过数据了
     * 在[holderLifecycleOwner] 生命周期内或者onBind 中使用都是安全的
     */
    val itemHolder get() = _itemHolder!!

    /**
     * 在事件处理中使用这个更加合适
     */
    val itemHolderOrNull get() = itemHolder

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

abstract class BindingViewHolder<IH : DataItemHolder>(binding: ViewBinding, key: String = "") :
    AbstractViewHolder<IH>(binding.root, key = key)

open class DefaultAdapter<IH : DataItemHolder, VH : AbstractViewHolder<IH>>(
    private val localCenter: Map<KClass<out DataItemHolder>, BuildBatch>? = null
) :
    RecyclerView.Adapter<VH>() {
    lateinit var target: RecyclerView.Adapter<VH>

    private val indexManager = IndexManager<ViewHolderKey>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val key = indexManager.getKey(viewType)
        val any = localCenter?.get(key.itemHolderClass) ?: registerCenter[key.itemHolderClass]
        val viewHolder =
            any?.b2?.let { it(parent, key.type) } ?: any?.b3?.let { it(parent, key.type, key.key) }
        @Suppress("UNCHECKED_CAST")
        return viewHolder as VH
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val itemHolder = getItemAbstract(position) ?: return
        holder.attachItemHolder(itemHolder)
        holder.moveStateToCreate()
        holder.onBind(itemHolder)
    }

    /**
     * PagingAdapter 存在返回null 的可能性
     */
    protected open fun getItemAbstract(position: Int): IH? {
        return if (target is ListAdapter<*, *>) {
            @Suppress("UNCHECKED_CAST")
            (target as ListAdapter<IH, VH>).currentList[position] as IH
        } else {
            throw NotImplementedError("${target::class.java.canonicalName}无法获取对应item holder")
        }
    }

    override fun getItemCount() = 0

    override fun getItemViewType(position: Int): Int {
        val item = getItemAbstract(position) ?: return super.getItemViewType(position)
        return indexManager.getIndex(item.viewHolderKey)
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

    companion object {
        private val DataItemHolder.viewHolderKey get() = ViewHolderKey(this::class, type, key)

        val common_diff_util = object : DiffUtil.ItemCallback<DataItemHolder>() {
            override fun areItemsTheSame(
                oldItem: DataItemHolder,
                newItem: DataItemHolder
            ): Boolean {
                return when {
                    oldItem === newItem -> true
                    oldItem.viewHolderKey == newItem.viewHolderKey -> {
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

@file:Suppress("UNCHECKED_CAST", "unused")

package com.storyteller_f.common_vm_ktx

import android.util.Log
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.set

/**
 * @author storyteller_f
 */

class CountableMediatorLiveData : MediatorLiveData<Any>() {
    var currentIndex = 0
}

class KeyedLiveData<T>(value: T, val key: String = "") : MutableLiveData<T>(value)

/**
 * 合并LiveData，value 变成map
 */
fun LiveData<out Any>.plus(source: LiveData<out Any>, key: String = ""): MediatorLiveData<out Any> {
    val sourceKey = if (source is KeyedLiveData<*> && source.key.trim().isNotEmpty()) {
        source.key
    } else {
        key
    }
    if (this is CountableMediatorLiveData) {
        val index = currentIndex
        val k = if (sourceKey.trim().isEmpty()) index.toString() else key
        addSource(source) {
            value = copyMap(value as Map<String, Any?>?).apply {
                set(k, it)
            }
        }
        currentIndex++
        return this
    } else {
        val mediatorLiveData = CountableMediatorLiveData()
        val k1 = if (this is KeyedLiveData<*> && this.key.trim().isNotEmpty()) {
            this.key
        } else {
            "first"
        }
        mediatorLiveData.addSource(this) {
            if (mediatorLiveData.value is Map<*, *>) {
                mediatorLiveData.value =
                    copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                        set(k1, it)
                    }
            }
        }
        val k = if (sourceKey.trim().isEmpty()) "second" else sourceKey

        mediatorLiveData.addSource(source) {
            mediatorLiveData.value =
                copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                    set(k, it)
                }
        }
        mediatorLiveData.currentIndex = 2
        return mediatorLiveData
    }
}

fun combine(vararg arrayOfPairs: Pair<String, LiveData<out Any?>>): LiveData<Map<String, Any?>> {
    val mediatorLiveData = MediatorLiveData<Map<String, Any?>>()
    arrayOfPairs.forEach {
        val index = it.first
        mediatorLiveData.addSource(it.second) {
            mediatorLiveData.value = copyMap(mediatorLiveData.value).apply {
                set(index, it)
            }
        }
    }
    return mediatorLiveData.map {
        it as Map<String, Any?>
    }
}

fun copyMap(map: Map<String, Any?>?): MutableMap<String, Any?> {
    val newly = mutableMapOf<String, Any?>()
    map?.forEach {
        newly[it.key] = it.value
    }
    return newly
}

/**
 * 控制能否让下游observe 到数据变动
 */
class MuteLiveEvent<T> : MutableLiveData<T?>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner) {
            if (mPending.get()) {
                observer.onChanged(it)
            }
        }
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    fun reset(t: T?) {
        mPending.set(false)
        super.setValue(t)
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}

class SingleLiveEvent<T> : MutableLiveData<T?>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner) {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        }
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}

fun <T> LiveData<T>.toDiff(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T?, T?>> {
    val mediatorLiveData = MediatorLiveData<Pair<T?, T?>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l == null || it == null || compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}

/**
 * @param compare 如果返回真，那么就会被过滤掉
 */
fun <T> LiveData<T>.toDiffNoNull(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T, T>> {
    val mediatorLiveData = MediatorLiveData<Pair<T, T>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l != null && it != null && compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}

fun <T> LiveData<T>.debounce(ms: Long): MediatorLiveData<T> {
    val mediatorLiveData = MediatorLiveData<T>()
    var lastTime: Long? = null
    val timer = Timer()
    mediatorLiveData.addSource(this) {
        val l = lastTime
        if (l == null || l - System.currentTimeMillis() >= ms) {
            timer.purge()
            mediatorLiveData.value = it
            lastTime = System.currentTimeMillis()
        } else {
            timer.schedule(
                object : TimerTask() {
                    override fun run() {
                        mediatorLiveData.postValue(it)
                    }
                },
                ms
            )
        }
    }
    return mediatorLiveData
}

fun <T> LiveData<T>.state(owner: LifecycleOwner, ob: Observer<in T>) {
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

/**
 * @param f 返回是否相等
 */
@Suppress("ComplexCondition")
fun <X> LiveData<X>.distinctUntilChangedBy(f: (X, X) -> Boolean): LiveData<X?> {
    val outputLiveData: MediatorLiveData<X?> = MediatorLiveData<X?>()
    outputLiveData.addSource(
        this,
        object : Observer<X?> {
            var mFirstTime = true
            var previous: X? = null
            override fun onChanged(value: X?) {
                val previousValue = previous
                if (mFirstTime ||
                    previousValue == null && value != null ||
                    previousValue != null && (
                        previousValue !== value ||
                            !f(previousValue, value)
                        )
                ) {
                    mFirstTime = false
                    outputLiveData.value = value
                    previous = value
                }
            }
        }
    )
    return outputLiveData
}

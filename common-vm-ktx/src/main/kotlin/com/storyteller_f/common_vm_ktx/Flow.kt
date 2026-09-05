@file:Suppress("unused")

package com.storyteller_f.common_vm_ktx

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine as combineFlows
import kotlinx.coroutines.flow.debounce as flowDebounce
import kotlinx.coroutines.flow.distinctUntilChanged as flowDistinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Combines named flows into the latest value of each source. */
fun combine(vararg sources: Pair<String, Flow<Any?>>): Flow<Map<String, Any?>> =
    combineFlows(
        sources.map { (key, source) ->
            source.map { value -> key to value }
        }
    ) { values ->
        values.associate { (key, value) -> key to value }
    }

/** Emits the previous and current value whenever [compare] does not consider them equivalent. */
fun <T> Flow<T>.toDiff(compare: ((T, T) -> Boolean)? = null): Flow<Pair<T?, T?>> = flow {
    var previous: T? = null
    collect { current ->
        val old = previous
        if (old == null || current == null || compare?.invoke(old, current) != true) {
            emit(old to current)
        }
        previous = current
    }
}

/** Like [toDiff], but only emits after two non-null values have been received. */
fun <T> Flow<T?>.toDiffNoNull(compare: ((T, T) -> Boolean)? = null): Flow<Pair<T, T>> = flow {
    var previous: T? = null
    collect { current ->
        val old = previous
        if (old != null && current != null && compare?.invoke(old, current) != true) {
            emit(old to current)
        }
        previous = current
    }
}

/** Delays emission until the upstream has been quiet for [milliseconds]. */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounce(milliseconds: Long): Flow<T> = flowDebounce(milliseconds)

/** Filters consecutive values for which [areEquivalent] returns true. */
fun <T> Flow<T>.distinctUntilChangedBy(areEquivalent: (T, T) -> Boolean): Flow<T> =
    flowDistinctUntilChanged(areEquivalent)

fun <T> Flow<T>.state(owner: LifecycleOwner, function: (T) -> Unit) = observe(owner, function)

/** Collects until [owner] is destroyed; fragments collect against their view lifecycle. */
fun <T> Flow<T>.observe(owner: LifecycleOwner, function: (T) -> Unit) {
    val lifecycleOwner = if (owner is Fragment) owner.viewLifecycleOwner else owner
    lifecycleOwner.lifecycleScope.launch {
        collect(function)
    }
}

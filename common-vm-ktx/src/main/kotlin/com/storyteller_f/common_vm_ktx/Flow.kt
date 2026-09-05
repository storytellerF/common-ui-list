package com.storyteller_f.common_vm_ktx

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine as combineFlows
import kotlinx.coroutines.flow.debounce as flowDebounce
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

/** Delays emission until the upstream has been quiet for [milliseconds]. */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounce(milliseconds: Long): Flow<T> = flowDebounce(milliseconds)

/** Collects against a fragment's view lifecycle, or the supplied owner's lifecycle otherwise. */
fun <T> Flow<T>.state(owner: LifecycleOwner, function: (T) -> Unit) {
    val lifecycleOwner = if (owner is Fragment) owner.viewLifecycleOwner else owner
    lifecycleOwner.lifecycleScope.launch {
        collect(function)
    }
}

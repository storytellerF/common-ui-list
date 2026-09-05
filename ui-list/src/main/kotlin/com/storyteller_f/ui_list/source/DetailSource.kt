package com.storyteller_f.ui_list.source

import android.util.Log
import androidx.paging.LoadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DetailSource"

class DetailHandler<D : Any>(
    private val producer: suspend () -> D,
    local: (suspend () -> D?)? = null
) {
    constructor(detailProducer: DetailProducer<D>) : this(detailProducer.producer, detailProducer.local)

    private val _content = MutableStateFlow<D?>(null)
    val content: StateFlow<D?> = _content
    private val _loadState = MutableStateFlow<LoadState?>(null)
    val loadState: StateFlow<LoadState?> = _loadState

    private val local: (suspend () -> D?)? = local

    fun load(scope: CoroutineScope): Job {
        return request(scope, local)
    }

    fun refresh(scope: CoroutineScope): Job {
        return request(scope, null)
    }

    private fun request(scope: CoroutineScope, local: (suspend () -> D?)?): Job {
        return scope.launch {
            try {
                _loadState.value = LoadState.Loading
                val value = obtainValue(local)
                _content.value = value
                _loadState.value = LoadState.NotLoading(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "request: ", e)
                _loadState.value = LoadState.Error(e)
            }
        }
    }

    private suspend fun obtainValue(local: (suspend () -> D?)?): D {
        return withContext(Dispatchers.IO) {
            local?.let { obtainLocal(it) } ?: producer()
        }
    }

    private suspend fun obtainLocal(local: suspend () -> D?): D? {
        return try {
            local()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "obtainLocal: ", e)
            null
        }
    }
}

class DetailProducer<D : Any>(
    val producer: suspend () -> D,
    val local: (suspend () -> D?)? = null
)

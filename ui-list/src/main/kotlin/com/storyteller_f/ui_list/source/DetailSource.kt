package com.storyteller_f.ui_list.source

import android.util.Log
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import com.storyteller_f.common_vm_ktx.vm
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "DetailSource"

class DetailHandler<D : Any>(
    private val producer: suspend () -> D,
    local: (suspend () -> D?)? = null
) {
    constructor(detailProducer: DetailProducer<D>) : this(detailProducer.producer, detailProducer.local)

    val content = MutableLiveData<D>()
    val loadState = MutableLiveData<LoadState>()

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
                loadState.value = LoadState.Loading
                val value = obtainValue(local)
                content.value = value
                loadState.value = LoadState.NotLoading(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "request: ", e)
                loadState.value = LoadState.Error(e)
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

@Deprecated(
    message = "Use a business ViewModel that owns DetailHandler instead of the shared SimpleDetailViewModel.",
    level = DeprecationLevel.WARNING
)
class SimpleDetailViewModel<D : Any>(
    producer: suspend () -> D,
    local: (suspend () -> D?)? = null
) : ViewModel() {
    private val handler = DetailHandler(producer, local)

    val content = handler.content
    val loadState = handler.loadState

    init {
        handler.load(viewModelScope)
    }

    fun refresh() {
        handler.refresh(viewModelScope)
    }
}

class DetailProducer<D : Any>(
    val producer: suspend () -> D,
    val local: (suspend () -> D?)? = null
)

@Deprecated(
    message = "Define a business ViewModel and use DetailHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <D : Any, T> T.detail(
    detailContent: DetailProducer<D>,
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = vm({}) {
    SimpleDetailViewModel(
        detailContent.producer,
        detailContent.local
    )
}

@Deprecated(
    message = "Define a business ViewModel and use DetailHandler inside it.",
    level = DeprecationLevel.WARNING
)
@Suppress("DEPRECATION")
fun <D : Any, ARG, T> T.detail(
    arg: () -> ARG,
    detailContentProducer: (ARG) -> DetailProducer<D>,
) where T : HasDefaultViewModelProviderFactory, T : ViewModelStoreOwner = detail(detailContentProducer(arg()))

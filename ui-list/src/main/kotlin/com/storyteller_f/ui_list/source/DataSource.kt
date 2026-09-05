package com.storyteller_f.ui_list.source

import android.util.Log
import androidx.paging.LoadState
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections

val LoadState?.isError get() = this is LoadState.Error
val LoadState?.isLoading get() = this is LoadState.Loading
val LoadState?.isNotLoading get() = this is LoadState.NotLoading

class MoreInfoLoadState(val loadState: LoadState, val itemCount: Int)

class SimpleDataRepository<D : Datum<RK>, RK : RemoteKey>(
    private val service: suspend (Int, Int) -> CommonResponse<D, RK>,
) {
    // 保存所有接收到的结果
    private val inMemoryCache = mutableListOf<D>()

    // shared flow of results, which allows us to broadcast updates so
    // the subscriber will have the latest data
    private val results = MutableSharedFlow<List<D>>(replay = 1)
    val loadState = MutableSharedFlow<MoreInfoLoadState>(replay = 1)

    // 保存上一次请求的页数，如果成功，自增
    private var lastRequestedPage = 0

    // 避免同一时刻进行多个请求
    private var isRequestInProgress = Mutex()

    suspend fun obtainResult(): Flow<List<D>> {
        coroutineScope {
            requestNextPage()
        }
        return results
    }

    suspend fun requestMore() {
        if (isRequestInProgress.isLocked) return
        requestNextPage()
    }

    suspend fun retry() {
        requestNextPage()
    }

    suspend fun refresh() {
        if (isRequestInProgress.isLocked) return
        lastRequestedPage = 0
        inMemoryCache.clear()
        requestNextPage()
    }

    private suspend fun requestNextPage() {
        val successful = requestAndSaveData(lastRequestedPage + 1)
        if (successful) {
            lastRequestedPage++
        }
    }

    private suspend fun requestAndSaveData(pageCount: Int): Boolean {
        return isRequestInProgress.withLock {
            requestPage(pageCount)
        }
    }

    private suspend fun SimpleDataRepository<D, RK>.requestPage(pages: Int): Boolean {
        loadState.emit(MoreInfoLoadState(LoadState.Loading, 0))
        try {
            val response = service(pages, 30)
            val elements = response.items
            inMemoryCache.addAll(elements)
            results.emit(inMemoryCache)
            loadState.emit(
                MoreInfoLoadState(
                    LoadState.NotLoading(elements.isEmpty()),
                    inMemoryCache.size,
                )
            )
            return true
        } catch (exception: Exception) {
            Log.e(TAG, "requestPage: ", exception)
            loadState.emit(MoreInfoLoadState(LoadState.Error(exception), inMemoryCache.size))
        }
        return false
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(inMemoryCache, from, to)
    }

    companion object {
        private const val TAG = "DataSource"
    }
}
/**
 * 与 SourceHandler 类似，不过支持排序
 */
class DataHandler<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
    private val sourceRepository: SimpleDataRepository<D, RK>,
    processFactory: (D) -> Holder,
) {

    val content: Flow<FatData<D, Holder, RK>> = flow {
        emitAll(sourceRepository.obtainResult().map { data ->
            FatData<D, Holder, RK>(data.map { datum ->
                processFactory(datum)
            }.toMutableList(), ::swap)
        })
    }

    val loadState: Flow<MoreInfoLoadState> = sourceRepository.loadState

    fun requestMore(scope: CoroutineScope) {
        scope.launch {
            sourceRepository.requestMore()
        }
    }

    fun retry(scope: CoroutineScope) {
        scope.launch {
            sourceRepository.retry()
        }
    }

    fun refresh(scope: CoroutineScope) {
        scope.launch {
            sourceRepository.refresh()
        }
    }

    internal fun swap(from: Int, to: Int) {
        sourceRepository.swap(from, to)
    }

    open class FatData<D : Datum<RK>, Holder : DataItemHolder, RK : RemoteKey>(
        val list: MutableList<Holder>,
        private val swapper: (Int, Int) -> Unit,
    ) {
        fun swap(from: Int, to: Int) {
            Collections.swap(list, from, to)
            swapper(from, to)
        }
    }
}

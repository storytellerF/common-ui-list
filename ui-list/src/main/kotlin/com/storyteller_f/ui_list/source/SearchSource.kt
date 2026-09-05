package com.storyteller_f.ui_list.source

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import androidx.paging.map
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SimpleSearchSource<D : Model, SQ : Any>(
    val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
    private val sq: SQ
) : PagingSource<Int, D>() {
    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, D> {
        val position = params.key ?: STARTING_PAGE_INDEX
        return try {
            val response = service(sq, position, params.loadSize)
            val items = response.items
            val nextKey = if (items.isEmpty() || items.size < params.loadSize || response.total == 0) {
                null
            } else {
                // initial load size = 3 * NETWORK_PAGE_SIZE
                // ensure we're not requesting duplicating items, at the 2nd request
                position + (params.loadSize / SimpleSourceRepository.NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = items,
                prevKey = if (position == STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        } catch (exception: Exception) {
            Log.e(TAG, "load: ", exception)
            LoadResult.Error(exception)
        }
    }

    companion object {
        private const val TAG = "SearchSource"
    }
}
class SimpleSearchRepository<D : Model, SQ : Any>(
    private val service: suspend (SQ, Int, Int) -> SimpleResponse<D>,
) {
    fun search(sq: SQ): Flow<PagingData<D>> {
        return Pager(
            config = PagingConfig(
                pageSize = SimpleSourceRepository.NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { SimpleSearchSource(service, sq) }
        ).flow
    }
}

class SearchHandler<D : Model, SQ : Any, Holder : DataItemHolder>(
    private val repository: SimpleSearchRepository<D, SQ>,
    val processFactory: (D, SQ) -> Holder,
) {
    private var currentQueryValue: SQ? = null
    var lastJob: Job? = null

    private var currentSearchResult: Flow<PagingData<Holder>>? = null

    fun search(sq: SQ, scope: CoroutineScope): Flow<PagingData<Holder>> {
        val lastResult = currentSearchResult
        if (sq == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = sq
        val newResult: Flow<PagingData<Holder>> = repository.search(sq)
            .map { pagingData ->
                pagingData.map {
                    processFactory(it, sq)
                }
            }
            .cachedIn(scope)
        currentSearchResult = newResult
        return newResult
    }
}

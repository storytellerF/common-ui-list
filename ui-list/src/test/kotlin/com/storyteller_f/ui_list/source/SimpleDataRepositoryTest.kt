package com.storyteller_f.ui_list.source

import androidx.paging.LoadState
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleDataRepositoryTest {

    @Test
    fun `loads pages appends results and refreshes from the first page`() = runBlocking {
        val requestedPages = mutableListOf<Int>()
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { page, _ ->
            requestedPages += page
            CommonResponse(items = listOf(TestDatum("item-$page")))
        }

        val results = repository.obtainResult()
        assertEquals(listOf(TestDatum("item-1")), results.first())

        repository.requestMore()
        assertEquals(listOf(TestDatum("item-1"), TestDatum("item-2")), results.first())

        repository.refresh()
        assertEquals(listOf(TestDatum("item-1")), results.first())
        assertEquals(listOf(1, 2, 1), requestedPages)
        assertEquals(1, repository.loadState.first().itemCount)
        assertTrue(repository.loadState.first().loadState is LoadState.NotLoading)
    }

    @Test
    fun `reports errors when a page request fails`() = runBlocking {
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { _, _ ->
            error("network unavailable")
        }

        repository.obtainResult()

        val state = repository.loadState.first()
        assertTrue(state.loadState is LoadState.Error)
        assertEquals(0, state.itemCount)
    }

    @Test
    fun `obtaining results repeatedly initializes the repository only once`() = runBlocking {
        val requestedPages = mutableListOf<Int>()
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { page, _ ->
            requestedPages += page
            CommonResponse(items = listOf(TestDatum("item-$page")))
        }

        val first = repository.obtainResult()
        val second = repository.obtainResult()

        assertSame(first, second)
        assertEquals(listOf(1), requestedPages)
        assertEquals(listOf(TestDatum("item-1")), second.first())
    }

    @Test
    fun `concurrent result collectors share the single initialization request`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val requestedPages = mutableListOf<Int>()
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { page, _ ->
            requestedPages += page
            started.complete(Unit)
            release.await()
            CommonResponse(items = listOf(TestDatum("item-$page")))
        }

        val first = async { repository.obtainResult() }
        started.await()
        val second = async { repository.obtainResult() }
        release.complete(Unit)

        assertSame(first.await(), second.await())
        assertEquals(listOf(1), requestedPages)
    }

    @Test
    fun `does not start another page request while one is in progress`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val requestedPages = mutableListOf<Int>()
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { page, _ ->
            requestedPages += page
            started.complete(Unit)
            release.await()
            CommonResponse(items = listOf(TestDatum("item-$page")))
        }

        val initialRequest = async { repository.obtainResult() }
        started.await()
        repository.requestMore()
        repository.refresh()
        release.complete(Unit)
        initialRequest.await()

        assertEquals(listOf(1), requestedPages)
    }

    private data class TestDatum(private val id: String) : Datum<RemoteKey> {
        override fun commonId() = id
        override fun remoteKey(prevKey: Int?, nextKey: Int?) = RemoteKey(id, prevKey, nextKey)
    }
}

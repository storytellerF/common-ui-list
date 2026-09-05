package com.storyteller_f.ui_list.source

import androidx.paging.PagingSource
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleSearchSourceTest {

    @Test
    fun `load creates a terminal first page for a short result`() = runBlocking {
        val source = SimpleSearchSource<TestModel, String>({ _, _, _ ->
            SimpleResponse(total = 2, items = listOf(TestModel("one"), TestModel("two")))
        }, "query")

        val result = source.load(refreshParams(loadSize = 3)) as PagingSource.LoadResult.Page
        assertEquals(listOf(TestModel("one"), TestModel("two")), result.data)
        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `load advances a full page and returns service failures`() = runBlocking {
        val source = SimpleSearchSource<TestModel, String>({ _, page, size ->
            assertEquals(1, page)
            assertEquals(30, size)
            SimpleResponse(total = 60, items = (1..30).map { TestModel(it.toString()) })
        }, "query")

        val page = source.load(refreshParams(loadSize = 30)) as PagingSource.LoadResult.Page
        assertEquals(2, page.nextKey)

        val failing = SimpleSearchSource<TestModel, String>({ _, _, _ -> error("network") }, "query")
        assertTrue(failing.load(refreshParams()) is PagingSource.LoadResult.Error)
    }

    @Test
    fun `load keeps previous key and stops when the response total is zero`() = runBlocking {
        val source = SimpleSearchSource<TestModel, String>({ _, page, _ ->
            assertEquals(3, page)
            SimpleResponse(total = 0, items = listOf(TestModel("one")))
        }, "query")

        val result = source.load(
            PagingSource.LoadParams.Append(key = 3, loadSize = 30, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(2, result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `refresh key follows the closest page key`() {
        val source = SimpleSearchSource<TestModel, String>({ _, _, _ ->
            SimpleResponse(total = 0, items = emptyList())
        }, "query")
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(TestModel("one")),
                    prevKey = 2,
                    nextKey = 4,
                )
            ),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 30),
            leadingPlaceholderCount = 0,
        )

        assertEquals(3, source.getRefreshKey(state))
    }

    @Test
    fun `refresh key falls back to the following page when no previous key exists`() {
        val source = SimpleSearchSource<TestModel, String>({ _, _, _ ->
            SimpleResponse(total = 0, items = emptyList())
        }, "query")
        val state = PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(TestModel("one")),
                    prevKey = null,
                    nextKey = 6,
                )
            ),
            anchorPosition = 0,
            config = PagingConfig(pageSize = 30),
            leadingPlaceholderCount = 0,
        )

        assertEquals(5, source.getRefreshKey(state))
    }

    @Test
    fun `refresh key is absent when paging has no anchor`() {
        val source = SimpleSearchSource<TestModel, String>({ _, _, _ ->
            SimpleResponse(total = 0, items = emptyList())
        }, "query")
        val state = PagingState<Int, TestModel>(
            pages = emptyList(),
            anchorPosition = null,
            config = PagingConfig(pageSize = 30),
            leadingPlaceholderCount = 0,
        )

        assertNull(source.getRefreshKey(state))
    }

    @Test
    fun `search handler reuses the flow for an unchanged query`() {
        val repository = SimpleSearchRepository<TestModel, String> { _, _, _ ->
            SimpleResponse(total = 0, items = emptyList())
        }
        val handler = SearchHandler(repository) { model, query ->
            TestHolder("$query:${model.commonId()}")
        }
        val scope = CoroutineScope(SupervisorJob())

        try {
            val first = handler.search("query", scope)
            assertSame(first, handler.search("query", scope))
            assertNotSame(first, handler.search("other", scope))
        } finally {
            scope.cancel()
        }
    }

    private fun refreshParams(loadSize: Int = 30) = PagingSource.LoadParams.Refresh<Int>(
        key = null,
        loadSize = loadSize,
        placeholdersEnabled = false,
    )

    private data class TestModel(private val id: String) : Model {
        override fun commonId() = id
    }

    private class TestHolder(private val id: String) : DataItemHolder() {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestHolder && other.id == id
    }
}

package com.storyteller_f.ui_list.source

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.map
import androidx.room.Room
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.TestCommonRoomDatabase
import com.storyteller_f.ui_list.database.TestDatum
import com.storyteller_f.ui_list.database.TestRemoteKey
import com.storyteller_f.ui_list.database.TestRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SourceHandlerTest {
    private lateinit var room: TestRoomDatabase

    @Before
    fun setUp() {
        room = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            TestRoomDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        room.close()
    }

    @Test
    fun `repository stream is mapped and cached by its handler`() {
        val repository = SimpleSourceRepository<TestDatum, TestRemoteKey, TestRoomDatabase>(
            service = { _, _ -> CommonResponse(items = emptyList()) },
            database = TestCommonRoomDatabase(room),
            pagingSourceFactory = { EmptyPagingSource() },
        )
        var receivedStream: Flow<*>? = null
        val handler = SourceHandler(repository) { stream ->
            receivedStream = stream
            stream.map { pagingData -> pagingData.map { TestHolder(it.id) } }
        }

        val scope = CoroutineScope(SupervisorJob())
        try {
            assertNotNull(handler.content(scope))
            assertSame(repository.resultStream, receivedStream)
        } finally {
            scope.cancel()
        }
    }

    private class EmptyPagingSource : PagingSource<Int, TestDatum>() {
        override fun getRefreshKey(state: PagingState<Int, TestDatum>): Int? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TestDatum> =
            LoadResult.Page(emptyList(), null, null)
    }

    private class TestHolder(val id: String) : DataItemHolder() {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestHolder && other.id == id
    }
}

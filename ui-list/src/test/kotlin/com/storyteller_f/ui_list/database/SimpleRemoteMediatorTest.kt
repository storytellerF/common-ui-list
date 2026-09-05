package com.storyteller_f.ui_list.database

import androidx.paging.LoadType
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalPagingApi::class)
class SimpleRemoteMediatorTest {
    private lateinit var room: TestRoomDatabase
    private lateinit var database: TestCommonRoomDatabase

    @Before
    fun setUp() {
        room = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            TestRoomDatabase::class.java,
        ).allowMainThreadQueries().build()
        database = TestCommonRoomDatabase(room)
    }

    @After
    fun tearDown() {
        room.close()
    }

    @Test
    fun `refresh stores data remote keys and clears stale rows in one transaction`() = runBlocking {
        room.dataDao().insertAll(listOf(TestDatum("stale")))
        val requestedPages = mutableListOf<Int>()
        val mediator = SimpleRemoteMediator<TestDatum, TestRemoteKey, TestRoomDatabase>(
            service = { page, size ->
                requestedPages += page
                assertEquals(10, size)
                CommonResponse(items = listOf(TestDatum("one"), TestDatum("two")))
            },
            commonRoomDatabase = database,
        )

        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
        assertFalse((result as androidx.paging.RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(1), requestedPages)
        assertEquals(1, database.clearCalls)
        assertEquals(listOf("one", "two"), room.dataDao().all().map { it.id })
        assertEquals(TestRemoteKey("one", null, 2), room.keyDao().find("one"))
        assertEquals(TestRemoteKey("two", null, 2), room.keyDao().find("two"))
    }

    @Test
    fun `prepend and append use existing remote key boundaries`() = runBlocking {
        val item = TestDatum("one")
        room.dataDao().insertAll(listOf(item))
        room.keyDao().insertAll(listOf(TestRemoteKey("one", null, 2)))
        val requestedPages = mutableListOf<Int>()
        val mediator = SimpleRemoteMediator<TestDatum, TestRemoteKey, TestRoomDatabase>(
            service = { page, _ ->
                requestedPages += page
                CommonResponse(items = emptyList())
            },
            commonRoomDatabase = database,
        )
        val state = pagingState(
            listOf(PagingSource.LoadResult.Page(data = listOf(item), prevKey = null, nextKey = 2)),
        )

        val prepend = mediator.load(LoadType.PREPEND, state)
        val append = mediator.load(LoadType.APPEND, state)

        assertTrue(prepend is androidx.paging.RemoteMediator.MediatorResult.Success)
        assertTrue((prepend as androidx.paging.RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertTrue(append is androidx.paging.RemoteMediator.MediatorResult.Success)
        assertTrue((append as androidx.paging.RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(listOf(2), requestedPages)
    }

    @Test
    fun `network io errors are returned as mediator errors`() = runBlocking {
        val mediator = SimpleRemoteMediator<TestDatum, TestRemoteKey, TestRoomDatabase>(
            service = { _, _ -> throw IOException("offline") },
            commonRoomDatabase = database,
        )

        val result = mediator.load(LoadType.REFRESH, pagingState())

        assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Error)
        assertTrue((result as androidx.paging.RemoteMediator.MediatorResult.Error).throwable is IOException)
    }

    private fun pagingState(
        pages: List<PagingSource.LoadResult.Page<Int, TestDatum>> = emptyList(),
    ) = PagingState(
        pages = pages,
        anchorPosition = 0,
        config = PagingConfig(pageSize = 10),
        leadingPlaceholderCount = 0,
    )
}

@Entity(tableName = "test_data")
data class TestDatum(@PrimaryKey val id: String) : Datum<TestRemoteKey> {
    override fun commonId() = id
    override fun remoteKey(prevKey: Int?, nextKey: Int?) = TestRemoteKey(id, prevKey, nextKey)
}

@Entity(tableName = "test_remote_keys")
data class TestRemoteKey(
    @PrimaryKey override val itemId: String,
    override val prevKey: Int?,
    override val nextKey: Int?,
) : RemoteKey(itemId, prevKey, nextKey)

@Dao
interface TestDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TestDatum>)

    @Query("DELETE FROM test_data")
    suspend fun clear()

    @Query("SELECT * FROM test_data ORDER BY id")
    suspend fun all(): List<TestDatum>
}

@Dao
interface TestKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(keys: List<TestRemoteKey>)

    @Query("DELETE FROM test_remote_keys")
    suspend fun clear()

    @Query("SELECT * FROM test_remote_keys WHERE itemId = :id")
    suspend fun find(id: String): TestRemoteKey?
}

@Database(entities = [TestDatum::class, TestRemoteKey::class], version = 1, exportSchema = false)
abstract class TestRoomDatabase : RoomDatabase() {
    abstract fun dataDao(): TestDataDao
    abstract fun keyDao(): TestKeyDao
}

class TestCommonRoomDatabase(database: TestRoomDatabase) :
    CommonRoomDatabase<TestDatum, TestRemoteKey, TestRoomDatabase>(database) {
    var clearCalls = 0

    override suspend fun clearOld() {
        clearCalls++
        database.keyDao().clear()
        database.dataDao().clear()
    }

    override suspend fun insertRemoteKey(remoteKeys: MutableList<TestRemoteKey>) {
        database.keyDao().insertAll(remoteKeys)
    }

    override suspend fun getRemoteKey(id: String) = database.keyDao().find(id)

    override suspend fun insertAllData(repos: MutableList<TestDatum>) {
        database.dataDao().insertAll(repos)
    }

    override suspend fun deleteItemBy(d: TestDatum) = Unit
}

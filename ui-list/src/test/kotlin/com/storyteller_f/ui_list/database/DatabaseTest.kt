package com.storyteller_f.ui_list.database

import androidx.paging.PagingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class DatabaseTest {

    @Test
    fun `default type converter preserves timestamps`() {
        val converter = DefaultTypeConverter()
        val timestamp = 1_725_000_000_123L

        assertEquals(Date(timestamp), converter.convertTimestampToDate(timestamp))
        assertEquals(timestamp, converter.convertDateToTimestamp(Date(timestamp)))
    }

    @Test
    fun `remote key exposes its paging boundaries`() {
        val key = RemoteKey(itemId = "item", prevKey = 2, nextKey = null)

        assertEquals("item", key.itemId)
        assertEquals(2, key.prevKey)
        assertNull(key.nextKey)
    }

    @Test
    fun `paging config debug string contains every configuration value`() {
        val config = PagingConfig(
            pageSize = 20,
            prefetchDistance = 5,
            initialLoadSize = 40,
            enablePlaceholders = false,
            maxSize = 100,
            jumpThreshold = 60,
        )

        assertEquals("5 40 20 false 100 60", config.selfPrint())
    }
}

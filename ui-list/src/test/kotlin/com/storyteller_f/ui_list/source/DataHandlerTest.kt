package com.storyteller_f.ui_list.source

import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.data.CommonResponse
import com.storyteller_f.ui_list.database.RemoteKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataHandlerTest {

    @Test
    fun `content maps repository data to holders and keeps swaps in sync`() = runBlocking {
        val repository = SimpleDataRepository<TestDatum, RemoteKey> { _, _ ->
            CommonResponse(items = listOf(TestDatum("first"), TestDatum("second")))
        }
        val handler = DataHandler(repository) { datum -> TestHolder(datum.id) }

        val content = handler.content.first()
        assertEquals(listOf("first", "second"), content.list.map { it.id })

        content.swap(0, 1)
        assertEquals(listOf("second", "first"), content.list.map { it.id })
    }

    private data class TestDatum(val id: String) : Datum<RemoteKey> {
        override fun commonId() = id
        override fun remoteKey(prevKey: Int?, nextKey: Int?) = RemoteKey(id, prevKey, nextKey)
    }

    private class TestHolder(val id: String) : DataItemHolder() {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestHolder && other.id == id
    }
}

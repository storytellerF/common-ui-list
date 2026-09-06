package com.storyteller_f.ui_list.adapter

import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Datum
import com.storyteller_f.ui_list.database.RemoteKey
import com.storyteller_f.ui_list.source.DataHandler
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SimpleDataAdapterTest {

    @Test
    fun `swap updates the current fat data and skips the matching next submit`() {
        val adapter = SimpleDataAdapter<TestHolder, TestViewHolder>(emptyMap())
        val firstSwaps = mutableListOf<Pair<Int, Int>>()
        val first = fatData(listOf("one", "two"), firstSwaps)
        val secondSwaps = mutableListOf<Pair<Int, Int>>()
        val second = fatData(listOf("three", "four"), secondSwaps)

        adapter.submitData(first)
        adapter.swap(0, 1)
        assertEquals(listOf("two", "one"), first.list.map { it.id })
        assertEquals(listOf(0 to 1), firstSwaps)

        adapter.submitData(second)
        adapter.swap(0, 1)

        assertEquals(listOf("one", "two"), first.list.map { it.id })
        assertEquals(listOf(0 to 1, 0 to 1), firstSwaps)
        assertEquals(emptyList<Pair<Int, Int>>(), secondSwaps)
    }

    private fun fatData(
        ids: List<String>,
        swaps: MutableList<Pair<Int, Int>>,
    ) = DataHandler.FatData<TestDatum, TestHolder, RemoteKey>(
        list = ids.map(::TestHolder).toMutableList(),
        swapper = { from, to -> swaps += from to to },
    )

    private data class TestDatum(private val id: String) : Datum<RemoteKey> {
        override fun commonId() = id
        override fun remoteKey(prevKey: Int?, nextKey: Int?) = RemoteKey(id, prevKey, nextKey)
    }

    private class TestHolder(val id: String) : DataItemHolder() {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestHolder && other.id == id
    }

    private class TestViewHolder(itemView: android.view.View) : AbstractViewHolder<TestHolder>(itemView) {
        override fun bindData(itemHolder: TestHolder) = Unit
    }
}

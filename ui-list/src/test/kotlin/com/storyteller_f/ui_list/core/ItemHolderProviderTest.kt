package com.storyteller_f.ui_list.core

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ItemHolderProviderTest {
    @Test
    fun `adapter lookup reflects data updates without another bind`() {
        val original = TestItem("original")
        val adapter = DataAdapter(mutableListOf(original))
        val holder = adapter.createViewHolder(FrameLayout(RuntimeEnvironment.getApplication()), 0)
        adapter.bindViewHolder(holder, 0)

        val replacement = TestItem("replacement")
        adapter.items[0] = replacement

        assertSame(replacement, adapter.getItemHolderAt(0))
        assertEquals(listOf(original), holder.bound)
        adapter.items.clear()
        assertNull(adapter.getItemHolderAt(0))
    }

    @Test
    fun `nested concat lookup resolves content and ignores headers footers and placeholders`() {
        val item = TestItem("content")
        val content = DataAdapter(mutableListOf(item, null))
        val nested = ConcatAdapter(PlainAdapter(1), content, PlainAdapter(1))
        val adapter = ConcatAdapter(PlainAdapter(1), nested, PlainAdapter(1))

        assertSame(item, adapter.getItemHolderAt(2))
        for (position in listOf(0, 1, 3, 4, 5, RecyclerView.NO_POSITION, adapter.itemCount, Int.MAX_VALUE)) {
            assertNull(adapter.getItemHolderAt(position))
        }
    }

    @Test
    fun `concat lookup follows offset changes after notifications`() {
        val item = TestItem("content")
        val header = PlainAdapter(1)
        val adapter = ConcatAdapter(header, DataAdapter(mutableListOf(item)))
        assertSame(item, adapter.getItemHolderAt(1))

        header.count = 2
        header.notifyItemInserted(0)

        assertNull(adapter.getItemHolderAt(1))
        assertSame(item, adapter.getItemHolderAt(2))
    }

    private class DataAdapter(val items: MutableList<TestItem?>) :
        RecyclerView.Adapter<RecordingHolder>(), ItemHolderProvider<TestItem> {
        override fun getItemHolder(position: Int) = items.getOrNull(position)
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            RecordingHolder(View(parent.context))
        override fun onBindViewHolder(holder: RecordingHolder, position: Int) {
            items[position]?.let(holder::bindData)
        }
    }

    private class PlainAdapter(var count: Int) : RecyclerView.Adapter<RecordingHolder>() {
        override fun getItemCount() = count
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            RecordingHolder(View(parent.context))
        override fun onBindViewHolder(holder: RecordingHolder, position: Int) = Unit
    }

    private data class TestItem(val id: String) : DataItemHolder() {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestItem && other.id == id
    }

    private class RecordingHolder(itemView: View) : AbstractViewHolder<TestItem>(itemView) {
        val bound = mutableListOf<TestItem>()
        override fun bindData(itemHolder: TestItem) {
            bound += itemHolder
        }
    }
}

package com.storyteller_f.ui_list.core

import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultAdapterTest {

    @Test
    fun `creates a view holder with the two argument builder when registered`() {
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        val item = TestItem("one", content = "content", itemType = "row")
        var receivedType: String? = null
        val adapter = adapterFor(item, BuildBatch(b2 = { _, type ->
            receivedType = type
            RecordingHolder(View(activity))
        }))

        adapter.onCreateViewHolder(FrameLayout(activity), adapter.getItemViewType(0))

        assertEquals("row", receivedType)
    }

    @Test
    fun `creates a view holder with the three argument builder when registered`() {
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        val item = TestItem("one", content = "content", itemType = "row", itemKey = "header")
        var received = emptyList<String>()
        val adapter = adapterFor(item, BuildBatch(b3 = { _, type, key ->
            received = listOf(type, key)
            RecordingHolder(View(activity))
        }))

        adapter.onCreateViewHolder(FrameLayout(activity), adapter.getItemViewType(0))

        assertEquals(listOf("row", "header"), received)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fails clearly when no view holder builder is registered`() {
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()
        val item = TestItem("one", content = "content")
        val adapter = object : DefaultAdapter<TestItem, RecordingHolder>(emptyMap()) {
            override fun getItemAbstract(position: Int) = item
        }

        adapter.onCreateViewHolder(FrameLayout(activity), adapter.getItemViewType(0))
    }

    @Test
    fun `common diff treats the same instance as the same item`() {
        val holder = TestItem(id = "one", content = "first")

        assertTrue(DefaultAdapter.common_diff_util.areItemsTheSame(holder, holder))
    }

    @Test
    fun `common diff delegates item identity when view holder keys match`() {
        val old = TestItem(id = "one", content = "first", itemType = "row", itemKey = "key")
        val same = TestItem(id = "one", content = "second", itemType = "row", itemKey = "key")
        val different = TestItem(id = "two", content = "second", itemType = "row", itemKey = "key")

        assertTrue(DefaultAdapter.common_diff_util.areItemsTheSame(old, same))
        assertFalse(DefaultAdapter.common_diff_util.areItemsTheSame(old, different))
    }

    @Test
    fun `common diff rejects different view holder keys and compares contents`() {
        val old = TestItem(id = "one", content = "first", itemType = "row", itemKey = "one")
        val differentKey = TestItem(id = "one", content = "first", itemType = "header", itemKey = "two")
        val sameContent = TestItem(id = "one", content = "first", itemType = "row", itemKey = "one")
        val differentContent = TestItem(id = "one", content = "second", itemType = "row", itemKey = "one")

        assertFalse(DefaultAdapter.common_diff_util.areItemsTheSame(old, differentKey))
        assertTrue(DefaultAdapter.common_diff_util.areContentsTheSame(old, sameContent))
        assertFalse(DefaultAdapter.common_diff_util.areContentsTheSame(old, differentContent))
    }

    private data class TestItem(
        val id: String,
        val content: String,
        private val itemType: String = "",
        private val itemKey: String = "",
    ) : DataItemHolder(type = itemType, key = itemKey) {
        override fun areItemsTheSame(other: DataItemHolder): Boolean =
            other is TestItem && id == other.id
    }

    private fun adapterFor(item: TestItem, batch: BuildBatch) =
        object : DefaultAdapter<TestItem, RecordingHolder>(mapOf(TestItem::class to batch)) {
            override fun getItemAbstract(position: Int) = item
        }

    private class RecordingHolder(view: View) : AbstractViewHolder<TestItem>(view) {
        override fun bindData(itemHolder: TestItem) = Unit
    }
}

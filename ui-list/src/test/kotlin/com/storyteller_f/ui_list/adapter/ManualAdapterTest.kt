package com.storyteller_f.ui_list.adapter

import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.BuildBatch
import com.storyteller_f.ui_list.core.DataItemHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ManualAdapterTest {

    @Test
    fun `uses the registered builder for submitted items`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var receivedType: String? = null
        val adapter = ManualAdapter<TestItem, TestViewHolder>(
            mapOf(TestItem::class to BuildBatch(b2 = { _, type ->
                receivedType = type
                TestViewHolder(View(activity))
            }))
        )

        adapter.submitList(listOf(TestItem("id", "row")))
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val viewType = adapter.getItemViewType(0)
        val holder = adapter.onCreateViewHolder(FrameLayout(activity), viewType)

        adapter.onBindViewHolder(holder, 0)
        adapter.onViewAttachedToWindow(holder)
        adapter.onViewDetachedFromWindow(holder)
        adapter.onViewRecycled(holder)

        assertEquals("row", receivedType)
        assertEquals(activity, holder.itemView.context)
        assertNull(holder.holderLifecycleOwnerOrNull)
    }

    private class TestItem(
        private val id: String,
        type: String,
    ) : DataItemHolder(type = type) {
        override fun areItemsTheSame(other: DataItemHolder) = other is TestItem && other.id == id
    }

    private class TestViewHolder(view: View) : AbstractViewHolder<TestItem>(view) {
        override fun bindData(itemHolder: TestItem) = Unit
    }
}

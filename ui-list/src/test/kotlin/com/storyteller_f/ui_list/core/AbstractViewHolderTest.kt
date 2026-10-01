package com.storyteller_f.ui_list.core

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.storyteller_f.ui_list.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AbstractViewHolderTest {

    @Test
    fun `default adapter forwards recycler lifecycle events to its holder`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val item = TestItem("item")
        val holder = RecordingHolder(View(activity))
        val adapter = object : DefaultAdapter<TestItem, RecordingHolder>(emptyMap()) {
            override fun getItemAbstract(position: Int) = item
        }

        adapter.onBindViewHolder(holder, 0)
        adapter.onViewAttachedToWindow(holder)
        adapter.onViewDetachedFromWindow(holder)
        adapter.onViewRecycled(holder)

        assertEquals(listOf(item), holder.bound)
        assertNull(holder.holderLifecycleOwnerOrNull)
    }

    @Test
    fun `holder lifecycle follows create bind attach stop and recycle events`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val holder = RecordingHolder(View(activity))
        val item = TestItem("item")

        holder.moveStateToStop(isHolderEvent = true)
        holder.moveStateToCreate(isHolderEvent = true)
        assertEquals(Lifecycle.State.CREATED, holder.holderLifecycleOwner.lifecycle.currentState)

        holder.onBind(item)
        assertEquals(listOf(item), holder.bound)

        holder.moveStateToStart()
        assertTrue(
            holder.holderLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )

        holder.moveStateToPause(isHolderEvent = true)
        holder.moveStateToStop(isHolderEvent = true)
        assertEquals(Lifecycle.State.CREATED, holder.holderLifecycleOwner.lifecycle.currentState)

        holder.moveStateToDestroy(isHolderEvent = true)
        assertNull(holder.holderLifecycleOwnerOrNull)
        assertNull(holder.holderLifecycleOwnerFlow.value)
    }

    @Test
    fun `unbound holder exposes null and context resources`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val holder = RecordingHolder(View(activity))

        assertNull(holder.itemHolderOrNull)
        assertEquals(activity.getColor(android.R.color.black), holder.getColor(android.R.color.black))
        assertEquals(activity.getString(R.string.loading), holder.getString(R.string.loading))
        assertEquals(
            activity.resources.getDimension(R.dimen.row_item_margin_vertical),
            holder.getDimen(R.dimen.row_item_margin_vertical),
            0f,
        )
        assertNotNull(holder.getDrawable(android.R.drawable.ic_menu_add))
    }

    @Test
    fun `current item reflects adapter updates without another bind`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val original = TestItem("original")
        val adapter = CurrentItemAdapter(mutableListOf(original))

        val recycler = layoutRecycler(activity, adapter)
        val holder = recycler.findViewHolderForAdapterPosition(0) as RecordingHolder
        assertSame(original, holder.itemHolderOrNull)

        val replacement = TestItem("replacement")
        adapter.items[0] = replacement
        assertSame(replacement, holder.itemHolderOrNull)
        assertEquals(listOf(original), holder.bound)

        adapter.items.clear()
        assertNull(holder.itemHolderOrNull)
        adapter.notifyItemRemoved(0)
        assertEquals(RecyclerView.NO_POSITION, holder.bindingAdapterPosition)
        assertNull(holder.itemHolderOrNull)
    }

    @Test
    fun `concat holder resolves position in the adapter that bound it`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val header = CurrentItemAdapter(mutableListOf(TestItem("header")))
        val item = TestItem("content")
        val content = CurrentItemAdapter(mutableListOf(item))
        val concat = ConcatAdapter(header, content)
        val recycler = layoutRecycler(activity, concat)
        val holder = recycler.findViewHolderForAdapterPosition(1) as RecordingHolder

        assertEquals(0, holder.bindingAdapterPosition)
        assertSame(item, holder.itemHolderOrNull)
    }

    private fun layoutRecycler(
        activity: ComponentActivity,
        adapter: RecyclerView.Adapter<*>,
    ): RecyclerView {
        val recycler = RecyclerView(activity)
        recycler.layoutManager = LinearLayoutManager(activity)
        recycler.adapter = adapter
        activity.setContentView(recycler)
        val size = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
        recycler.measure(size, size)
        recycler.layout(0, 0, 500, 500)
        return recycler
    }

    private class CurrentItemAdapter(val items: MutableList<TestItem>) :
        RecyclerView.Adapter<RecordingHolder>(), ItemHolderProvider<TestItem> {
        override fun getItemHolder(position: Int) = items.getOrNull(position)
        override fun getItemCount() = items.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            RecordingHolder(View(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(100, 100)
            })
        override fun onBindViewHolder(holder: RecordingHolder, position: Int) {
            holder.onBind(items[position])
        }
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

package com.storyteller_f.ui_list.core

import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import com.storyteller_f.ui_list.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

        holder.attachItemHolder(item)
        holder.onBind(item)
        assertEquals(item, holder.itemHolder)
        assertEquals(listOf(item), holder.bound)

        holder.moveStateToStart()
        assertTrue(
            holder.holderLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )

        holder.moveStateToPause(isHolderEvent = true)
        holder.moveStateToStop(isHolderEvent = true)
        assertEquals(Lifecycle.State.CREATED, holder.holderLifecycleOwner.lifecycle.currentState)

        holder.moveStateToDestroy(isHolderEvent = true)
        holder.detachItemHolder()
        assertNull(holder.holderLifecycleOwnerOrNull)
        assertNull(holder.holderLifecycleOwnerFlow.value)
    }

    @Test
    fun `holder exposes its bound item and context resources`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val holder = RecordingHolder(View(activity))
        val item = TestItem("item")

        holder.attachItemHolder(item)

        assertEquals(item, holder.itemHolderOrNull)
        assertEquals(activity.getColor(android.R.color.black), holder.getColor(android.R.color.black))
        assertEquals(activity.getString(R.string.loading), holder.getString(R.string.loading))
        assertEquals(
            activity.resources.getDimension(R.dimen.row_item_margin_vertical),
            holder.getDimen(R.dimen.row_item_margin_vertical),
            0f,
        )
        assertNotNull(holder.getDrawable(android.R.drawable.ic_menu_add))
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

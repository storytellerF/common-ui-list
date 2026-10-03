package com.storyteller_f.ui_list.core

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
    fun `holder exposes context resources`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val holder = RecordingHolder(View(activity))

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
    fun `recycling a detached fragment row removes its original lifecycle observer`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = controller.get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        val fragment = HolderFragment()
        activity.supportFragmentManager.beginTransaction().add(container.id, fragment).commitNow()
        val root = fragment.requireView() as ViewGroup
        val holder = RecordingHolder(View(activity))
        root.addView(holder.itemView)
        holder.moveStateToCreate(true)
        holder.moveStateToStart()
        val oldOwner = holder.holderLifecycleOwner

        controller.pause().stop()
        holder.moveStateToStop(true)
        root.removeView(holder.itemView)
        holder.moveStateToDestroy(true)
        assertEquals(Lifecycle.State.DESTROYED, oldOwner.lifecycle.currentState)
        assertNull(holder.holderLifecycleOwnerOrNull)

        // RecyclerView has removed the row from the view tree before recycling it.
        // Returning to this Fragment must not dispatch START to the recycled holder.
        controller.restart().start().resume()
        assertNull(holder.holderLifecycleOwnerOrNull)
        controller.pause().stop().destroy()
    }

    @Test
    fun `destroying the observed host destroys the holder lifecycle`() {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val holder = RecordingHolder(View(controller.get()))
        holder.moveStateToCreate(true)
        holder.moveStateToStart()
        val owner = holder.holderLifecycleOwner

        controller.pause().stop().destroy()

        assertEquals(Lifecycle.State.DESTROYED, owner.lifecycle.currentState)
        assertNull(holder.holderLifecycleOwnerOrNull)
        holder.moveStateToDestroy(true)
    }

    class HolderFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
            FrameLayout(requireContext())
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

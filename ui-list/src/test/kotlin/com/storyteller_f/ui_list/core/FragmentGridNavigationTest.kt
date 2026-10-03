package com.storyteller_f.ui_list.core

import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FragmentGridNavigationTest {
    @Test
    fun `switching to grid then popping a child fragment does not restart recycled holders`() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).setup().visible()
        val activity = controller.get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)
        val manager = activity.supportFragmentManager
        val fragment = ListFragment()
        manager.beginTransaction().add(container.id, fragment).commitNow()
        layout(container)
        val listHolders = fragment.rows.holders.toList()
        assertTrue(listHolders.isNotEmpty())

        fragment.rows.rowType = "grid"
        (fragment.requireView() as RecyclerView).layoutManager = GridLayoutManager(activity, 2)
        fragment.rows.notifyItemRangeChanged(0, fragment.rows.itemCount)
        layout(container)
        listHolders.forEach { assertNull(it.holderLifecycleOwnerOrNull) }
        assertTrue(fragment.rows.holders.any { it.holderLifecycleOwnerOrNull != null })

        manager.beginTransaction().replace(container.id, Fragment()).addToBackStack("child").commit()
        manager.executePendingTransactions()
        manager.popBackStackImmediate()
        layout(container)
        listHolders.forEach { assertNull(it.holderLifecycleOwnerOrNull) }
        val recycler = fragment.requireView() as RecyclerView
        assertTrue(recycler.childCount > 0)
        for (index in 0 until recycler.childCount) {
            val holder = recycler.getChildViewHolder(recycler.getChildAt(index)) as RowHolder
            assertTrue(holder.holderLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        controller.pause().stop().destroy()
    }

    private fun layout(view: View) {
        val exact = View.MeasureSpec.EXACTLY
        view.measure(
            View.MeasureSpec.makeMeasureSpec(600, exact),
            View.MeasureSpec.makeMeasureSpec(800, exact),
        )
        view.layout(0, 0, 600, 800)
        shadowOf(Looper.getMainLooper()).idle()
    }

    class ListFragment : Fragment() {
        val rows = Rows()
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
            RecyclerView(requireContext()).apply {
                layoutManager = if (rows.rowType == "grid") {
                    GridLayoutManager(context, 2)
                } else {
                    LinearLayoutManager(context)
                }
                adapter = rows
            }
    }

    class Rows : DefaultAdapter<Row, RowHolder>(emptyMap()) {
        var rowType = "list"
        val holders = mutableListOf<RowHolder>()
        override fun getItemCount() = 6
        override fun getItemAbstract(position: Int) = Row(position, rowType)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RowHolder(
            View(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)
            },
        ).also { holders += it }
    }

    class Row(val index: Int, type: String) : DataItemHolder(type) {
        override fun areItemsTheSame(other: DataItemHolder) = other is Row && other.index == index
    }

    class RowHolder(view: View) : AbstractViewHolder<Row>(view) {
        override fun bindData(itemHolder: Row) = Unit
    }
}

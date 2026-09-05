package com.storyteller_f.ui_list.ui

import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.paging.LoadState
import com.storyteller_f.ui_list.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SimpleLoadStateAdapterTest {

    @Test
    fun `loading state only shows progress`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val adapter = SimpleLoadStateAdapter { }
        val holder = adapter.onCreateViewHolder(FrameLayout(activity), LoadState.Loading)

        adapter.onBindViewHolder(holder, LoadState.Loading)

        assertTrue(holder.itemView.findViewById<android.view.View>(R.id.progress_bar).isVisible)
        assertFalse(holder.itemView.findViewById<android.view.View>(R.id.retry_button).isVisible)
    }

    @Test
    fun `error state renders its message and retries`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        var retries = 0
        val adapter = SimpleLoadStateAdapter { retries++ }
        val holder = adapter.onCreateViewHolder(FrameLayout(activity), LoadState.Error(IllegalStateException("offline")))

        adapter.onBindViewHolder(holder, LoadState.Error(IllegalStateException("offline")))
        holder.itemView.findViewById<android.view.View>(R.id.retry_button).performClick()

        assertEquals("offline", holder.itemView.findViewById<android.widget.TextView>(R.id.error_msg).text)
        assertEquals(1, retries)
    }
}

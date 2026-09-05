package com.storyteller_f.common_ui

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class ViewTest {
    @Test
    fun `click post visibility and margins update the target view`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val view = View(activity).apply {
            layoutParams = ViewGroup.MarginLayoutParams(20, 20)
        }
        activity.setContentView(view)
        var clicks = 0
        var posted = false
        var visibleValue = ""

        view.setOnClick { clicks++ }
        view.performClick()
        view.pp { posted = it === view }
        view.setVisible(true) { assertTrue(it === view) }
        view.setVisible("shown", { value: String -> value == "shown" }) { _, value: String ->
            visibleValue = value
        }
        view.updateMargins {
            leftMargin = 3
            topMargin = 4
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, clicks)
        assertTrue(posted)
        assertTrue(view.visibility == View.VISIBLE)
        assertEquals("shown", visibleValue)
        assertEquals(3, (view.layoutParams as ViewGroup.MarginLayoutParams).leftMargin)
        assertEquals(4, (view.layoutParams as ViewGroup.MarginLayoutParams).topMargin)

        view.setVisible(1, { value: String -> value.isNotEmpty() }) { _, _ -> error("wrong type") }
        assertFalse(view.visibility == View.VISIBLE)
        assertTrue(
            RuntimeEnvironment.getApplication().lf.inflate(android.R.layout.simple_list_item_1, null)
                is android.widget.TextView,
        )
    }

    @Test
    fun `bind synchronizes state flow and edit text changes`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val text = EditText(activity)
        val state = MutableStateFlow<Name?>(Name("first"))

        state.bind(activity, text, Name::value) { value -> Name(value) }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("first", text.text.toString())

        state.value = Name("second")
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("second", text.text.toString())

        text.setText("typed")
        assertEquals(Name("typed"), state.value)
    }

    private data class Name(val value: String)
}

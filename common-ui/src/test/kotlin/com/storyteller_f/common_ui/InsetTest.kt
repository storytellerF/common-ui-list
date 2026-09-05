package com.storyteller_f.common_ui

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InsetTest {

    @Test
    fun `direction combines each edge and renders a useful description`() {
        val direction = Direction(1, 2, 3, 4) + Direction(5, 6, 7, 8)

        assertEquals(6, direction.start)
        assertEquals(8, direction.top)
        assertEquals(10, direction.end)
        assertEquals(12, direction.bottom)
        assertEquals("Direction(start=6, top=8, end=10, bottom=12)", direction.toString())
    }

    @Test
    fun `inset block maps every requested edge`() {
        val allEdges = InsetBlockDirection.START or
            InsetBlockDirection.TOP or
            InsetBlockDirection.END or
            InsetBlockDirection.BOTTOM

        val all = allEdges.insetBlock(9)
        assertEquals(9, all.start)
        assertEquals(9, all.top)
        assertEquals(9, all.end)
        assertEquals(9, all.bottom)

        val none = 0.insetBlock(9)
        assertEquals(0, none.start)
        assertEquals(0, none.top)
        assertEquals(0, none.end)
        assertEquals(0, none.bottom)
    }

    @Test
    fun `get or create returns the cached value and otherwise creates one`() {
        val existing = Any()
        assertSame(existing, getOrCreate({ existing }, { error("must not create") }))

        var produced = 0
        val created = getOrCreate({ null }) { ++produced }
        assertEquals(1, created)
        assertEquals(1, produced)
    }

    @Test
    fun `view inset block is cached and updates ltr padding`() {
        val view = View(RuntimeEnvironment.getApplication()).apply {
            setPaddingRelative(1, 2, 3, 4)
            layoutParams = ViewGroup.MarginLayoutParams(100, 100).apply {
                marginStart = 5
                topMargin = 6
                marginEnd = 7
                bottomMargin = 8
            }
        }

        val inset = view.getInsetBlock()
        assertEquals(1, inset.padding.start)
        assertEquals(2, inset.padding.top)
        assertEquals(3, inset.padding.end)
        assertEquals(4, inset.padding.bottom)
        assertEquals(5, inset.margin.start)
        assertEquals(6, inset.margin.top)
        assertEquals(7, inset.margin.end)
        assertEquals(8, inset.margin.bottom)
        assertSame(inset, view.getInsetBlock())

        view.layoutDirection = View.LAYOUT_DIRECTION_LTR
        view.updatePadding(Direction(10, 11, 12, 13))
        assertEquals(10, view.paddingLeft)
        assertEquals(11, view.paddingTop)
        assertEquals(12, view.paddingRight)
        assertEquals(13, view.paddingBottom)

    }

    @Test
    fun `inset listener applies status and navigation bars to padding and margins`() {
        val view = View(RuntimeEnvironment.getApplication()).apply {
            setPaddingRelative(1, 2, 3, 4)
            layoutParams = ViewGroup.MarginLayoutParams(100, 100).apply {
                marginStart = 5
                topMargin = 6
                marginEnd = 7
                bottomMargin = 8
            }
        }
        val status = InsetBlockDirection.TOP or InsetBlockDirection.START
        val navigation = InsetBlockDirection.BOTTOM or InsetBlockDirection.END
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 10, 0, 0))
            .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, 20))
            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, 30))
            .build()

        view.inset(status, status, navigation, navigation)
        ViewCompat.dispatchApplyWindowInsets(view, insets)

        assertEquals(11, view.paddingStart)
        assertEquals(12, view.paddingTop)
        assertEquals(23, view.paddingEnd)
        assertEquals(24, view.paddingBottom)
        val margins = view.layoutParams as ViewGroup.MarginLayoutParams
        assertEquals(15, margins.marginStart)
        assertEquals(16, margins.topMargin)
        assertEquals(27, margins.marginEnd)
        assertEquals(28, margins.bottomMargin)
        assertEquals(Insets.of(0, 0, 0, 20), insets.navigator)
        assertEquals(Insets.of(0, 10, 0, 0), insets.status)
        assertEquals(Insets.of(0, 0, 0, 30), insets.ime)
    }

    @Test
    fun `visibility helpers update the target views and invoke only visible callbacks`() {
        val context = RuntimeEnvironment.getApplication()
        val first = View(context)
        val second = View(context)
        var clicked = false

        first.setOnClick { clicked = true }
        first.performClick()
        first.setVisible(true) { assertSame(first, it) }
        second.setVisible(false) { error("hidden view must not invoke callback") }
        listOf(first, second).onVisible(second)

        assertTrue(clicked)
        assertEquals(View.GONE, first.visibility)
        assertEquals(View.VISIBLE, second.visibility)
    }
}

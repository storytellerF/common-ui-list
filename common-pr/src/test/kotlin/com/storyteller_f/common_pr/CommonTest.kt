package com.storyteller_f.common_pr

import android.util.TypedValue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class CommonTest {

    @Test
    fun `dip helpers convert using the current display density`() {
        val context = RuntimeEnvironment.getApplication()
        val density = context.resources.displayMetrics.density

        with(context) {
            assertEquals(2.5f * density, 2.5f.dip, 0.0001f)
            assertEquals((3.9f * density).toInt(), 3.9f.dipToInt)
        }

        assertEquals(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.5f,
                context.resources.displayMetrics,
            ),
            with(context) { 2.5f.dip },
            0.0001f,
        )
    }
}

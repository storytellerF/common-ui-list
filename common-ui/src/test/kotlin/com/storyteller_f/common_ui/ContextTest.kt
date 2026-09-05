package com.storyteller_f.common_ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextTest {
    @Test
    fun `activity and attached fragment provide their contexts`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "context").commitNow()

        assertEquals(activity, activity.ctx)
        assertEquals(activity, activity.context { this })
        assertEquals(activity, fragment.ctx)
        assertEquals(activity, fragment.context { this })
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `unknown lifecycle owner has no context`() {
        object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }.ctx
    }

}

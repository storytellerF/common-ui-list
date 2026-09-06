package com.storyteller_f.common_ui

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentViewBindingDelegateTest {

    @Test
    fun `binding is recreated after the fragment view lifecycle ends`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = TestBindingFragment()

        activity.supportFragmentManager.beginTransaction().add(fragment, "binding").commitNow()
        val firstBinding = fragment.binding
        activity.supportFragmentManager.beginTransaction().remove(fragment).commitNow()
        activity.supportFragmentManager.beginTransaction().add(fragment, "binding").commitNow()
        val secondBinding = fragment.binding

        assertNotSame(firstBinding, secondBinding)
        assertEquals(2, fragment.bindingCreations)
    }
}

class TestBindingFragment : Fragment() {
    var bindingCreations = 0
    val binding by viewBinding {
        bindingCreations++
        TestBinding(it)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ) = View(requireContext())
}

class TestBinding(private val view: View) : ViewBinding {
    override fun getRoot() = view
}

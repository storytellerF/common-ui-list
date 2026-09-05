package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class ResponseTest {

    @Before
    fun setUp() {
        waitingInActivity.clear()
        waitingInFragment.clear()
    }

    @After
    fun tearDown() {
        waitingInActivity.clear()
        waitingInFragment.clear()
    }

    @Test
    fun `activity receives fragment result and removes only the completed request`() {
        val activity = Robolectric.buildActivity(ResponseTestActivity::class.java).setup().get()
        val firstRequest = FragmentRequest(UUID.randomUUID())
        val secondRequest = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null

        activity.observeResponse(firstRequest, Bundle::class.java) { received = it }
        activity.observeResponse(secondRequest, Bundle::class.java) { }
        activity.supportFragmentManager.setFragmentResult(
            firstRequest.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "activity") })
            },
        )

        assertEquals("activity", received?.getString("value"))
        val waiting = waitingInActivity[activity.registryKey()]
        assertNotNull(waiting)
        assertEquals(listOf(secondRequest.toString()), waiting?.map { it.requestKey })
    }

    @Test
    fun `fragment receives fragment result and clears its completed request`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ResponseTestFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "response-test").commitNow()
        val request = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null

        fragment.observeResponse(request, Bundle::class.java) { received = it }
        activity.supportFragmentManager.setFragmentResult(
            request.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "fragment") })
            },
        )

        assertEquals("fragment", received?.getString("value"))
        assertEquals(emptyList<FragmentAction>(), waitingInFragment[fragment.registryKey()])
        assertNull(waitingInActivity[fragment.registryKey()])
    }

    @Test
    fun `fragment kclass response overload delegates to the listener`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ResponseTestFragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "response-kclass-test").commitNow()
        val request = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null

        fragment.observeResponse(request, Bundle::class) { received = it }
        activity.supportFragmentManager.setFragmentResult(
            request.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "fragment-kclass") })
            },
        )

        assertEquals("fragment-kclass", received?.getString("value"))
    }

    @Test
    fun `kclass response overload delegates to the activity listener`() {
        val activity = Robolectric.buildActivity(ResponseTestActivity::class.java).setup().get()
        val request = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null

        activity.observeResponse(request, Bundle::class) { received = it }
        activity.supportFragmentManager.setFragmentResult(
            request.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "kclass") })
            },
        )

        assertEquals("kclass", received?.getString("value"))
        assertEquals(emptyList<ActivityAction>(), waitingInActivity[activity.registryKey()])
    }

    @Test
    fun `activity re-registers an outstanding result listener after lifecycle recovery`() {
        val activity = Robolectric.buildActivity(ResponseTestActivity::class.java).setup().get()
        val request = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null
        waitingInActivity[activity.registryKey()] = listOf(
            ActivityAction({ _, value -> received = value as Bundle }, request.toString()),
        )

        activity.observeResponse()
        activity.supportFragmentManager.setFragmentResult(
            request.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "restored") })
            },
        )

        assertEquals("restored", received?.getString("value"))
        assertEquals(emptyList<ActivityAction>(), waitingInActivity[activity.registryKey()])
    }

    @Test
    fun `common fragment re-registers an outstanding result listener on start`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = ResponseCommonFragment()
        val request = FragmentRequest(UUID.randomUUID())
        var received: Bundle? = null
        waitingInFragment[fragment.registryKey()] = listOf(
            FragmentAction({ _, value -> received = value as Bundle }, request.toString()),
        )

        activity.supportFragmentManager.beginTransaction().add(fragment, "common-response-test").commitNow()
        activity.supportFragmentManager.setFragmentResult(
            request.toString(),
            Bundle().apply {
                putParcelable(FRAGMENT_RESULT_KEY, Bundle().apply { putString("value", "common-fragment") })
            },
        )

        assertEquals("common-fragment", received?.getString("value"))
        assertEquals(emptyList<FragmentAction>(), waitingInFragment[fragment.registryKey()])
    }
}

class ResponseTestActivity : FragmentActivity(), Registry

class ResponseTestFragment : Fragment(), Registry {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ) = View(requireContext())
}

class ResponseCommonFragment : CommonFragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = View(requireContext())
}

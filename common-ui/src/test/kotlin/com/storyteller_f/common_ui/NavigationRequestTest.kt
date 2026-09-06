package com.storyteller_f.common_ui

import android.os.Bundle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.testing.TestNavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class NavigationRequestTest {

    @Test
    fun `resource id request navigates and adds its request uuid`() {
        val navController = navController()
        val arguments = Bundle().apply { putString("source", "resource-id") }

        val request = navController.request(DESTINATION_ID, arguments)

        assertEquals(DESTINATION_ID, navController.currentDestination?.id)
        assertEquals("resource-id", navController.currentBackStackEntry?.arguments?.getString("source"))
        assertRequestUuid(request, navController.currentBackStackEntry?.arguments)
    }

    @Test
    fun `directions request navigates using direction arguments and adds its request uuid`() {
        val navController = navController()
        val directions = object : NavDirections {
            override val actionId = DESTINATION_ID
            override val arguments = Bundle().apply { putInt("page", 2) }
        }

        val request = navController.request(directions)

        assertEquals(DESTINATION_ID, navController.currentDestination?.id)
        assertEquals(2, navController.currentBackStackEntry?.arguments?.getInt("page"))
        assertRequestUuid(request, navController.currentBackStackEntry?.arguments)
    }

    private fun navController(): TestNavHostController {
        return TestNavHostController(RuntimeEnvironment.getApplication()).apply {
            val navigator = RecordingNavigator()
            navigatorProvider.addNavigator(navigator)
            graph = NavGraph(NavGraphNavigator(navigatorProvider)).apply {
                id = GRAPH_ID
                setStartDestination(START_DESTINATION_ID)
                addDestination(navigator.createDestination().apply { id = START_DESTINATION_ID })
                addDestination(navigator.createDestination().apply { id = DESTINATION_ID })
            }
        }
    }

    private fun assertRequestUuid(request: FragmentRequest, arguments: Bundle?) {
        val uuid = arguments?.getSerializable("uuid") as? UUID

        assertNotNull(uuid)
        assertTrue(request.toString().contains(uuid.toString()))
    }

    private companion object {
        const val GRAPH_ID = 100
        const val START_DESTINATION_ID = 1
        const val DESTINATION_ID = 2
    }
}

@Navigator.Name("recording")
private class RecordingNavigator : Navigator<NavDestination>() {
    override fun createDestination() = NavDestination(this)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?,
    ) {
        entries.forEach(state::push)
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }
}

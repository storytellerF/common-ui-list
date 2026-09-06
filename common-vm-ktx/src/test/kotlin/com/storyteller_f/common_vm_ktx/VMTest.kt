package com.storyteller_f.common_vm_ktx

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.KClass

@RunWith(RobolectricTestRunner::class)
class VMTest {

    @Test
    fun `ViewModelLazy creates a ViewModel once and caches it`() {
        val store = ViewModelStore()
        var created = 0
        val lazy = ViewModelLazy(TestViewModel::class, { store }, {
            testFactory { TestViewModel("value-${++created}") }
        })

        assertFalse(lazy.isInitialized())
        val first = lazy.value

        assertTrue(lazy.isInitialized())
        assertSame(first, lazy.value)
        assertEquals("value-1", first.value)
        assertEquals(1, created)
    }

    @Test
    fun `keyed lazy uses the supplied prefix and caches its ViewModel`() {
        val store = ViewModelStore()
        var created = 0
        val base = ViewModelLazy(TestViewModel::class, { store }, {
            testFactory { TestViewModel("value-${++created}") }
        })
        val lazy = keyPrefix("screen", base)

        val first = lazy.value

        assertTrue(lazy.isInitialized())
        assertSame(first, lazy.value)
        assertEquals("value-1", first.value)
        assertEquals(1, created)
    }

    @Test
    fun `keyed lazy accepts a dynamic prefix provider`() {
        val store = ViewModelStore()
        val base = ViewModelLazy(TestViewModel::class, { store }, {
            testFactory { TestViewModel("value") }
        })

        val lazy = keyPrefix({ "dynamic" }, base)

        assertEquals("value", lazy.value.value)
        assertSame(lazy.value, lazy.value)
    }

    @Test
    fun `activity scope and vm helper use the activity store`() {
        val activity = Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java).setup().get()

        val scope = activity.selfScope
        val lazy = activity.vm(arg = { "activity" }) { TestViewModel(it) }

        assertSame(activity.viewModelStore, scope.storeProducer())
        assertSame(activity, scope.ownerProducer())
        assertEquals("activity", lazy.value.value)
    }

    @Test
    fun `fragment scopes select self parent and activity stores`() {
        val activity = Robolectric.buildActivity(androidx.fragment.app.FragmentActivity::class.java).setup().get()
        val parent = androidx.fragment.app.Fragment()
        activity.supportFragmentManager.beginTransaction().add(parent, "parent").commitNow()
        val child = androidx.fragment.app.Fragment()
        parent.childFragmentManager.beginTransaction().add(child, "child").commitNow()

        assertSame(parent.viewModelStore, parent.selfScope.storeProducer())
        assertSame(parent, parent.selfScope.ownerProducer())
        assertSame(parent.viewModelStore, child.parentScope.storeProducer())
        assertSame(parent, child.parentScope.ownerProducer())
        assertSame(activity.viewModelStore, child.activityScope.storeProducer())
        assertSame(activity, child.activityScope.ownerProducer())
    }

    @Test
    fun `defaultFactory supplies its argument to the ViewModel producer`() {
        val factory = defaultFactory<TestViewModel, String>({ "argument" }) { TestViewModel(it) }

        val model = factory().create(TestViewModel::class, CreationExtras.Empty)

        assertEquals("argument", model.value)
    }

    @Test
    fun `generic and saved state value models expose StateFlow values`() {
        assertEquals("generic", genericValueModel("generic").data.value)

        val handle = SavedStateHandle(mapOf("item" to "initial"))
        val model = StateValueModel(handle, "item", "default")
        assertEquals("initial", model.data.value)

        handle["item"] = "updated"
        assertEquals("updated", model.data.value)
        assertEquals("default", stateValueModel("default", SavedStateHandle()).data.value)
    }

    @Test
    fun `buildExtras starts from the owner extras`() {
        val owner = object : HasDefaultViewModelProviderFactory {
            override val defaultViewModelProviderFactory = testFactory { TestViewModel("value") }
            override val defaultViewModelCreationExtras = CreationExtras.Empty
        }

        val key = object : CreationExtras.Key<String> {}
        val extras = owner.buildExtras {
            set(key, "value")
        }

        assertEquals("value", extras[key])
    }

    private fun testFactory(create: () -> TestViewModel) = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = create() as T
    }

    private class TestViewModel(val value: String) : ViewModel()
}

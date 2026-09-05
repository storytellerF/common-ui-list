package com.storyteller_f.common_vm_ktx

import android.os.Looper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class BuilderValueModelTest {
    @Test
    fun `builder value model loads immediately by default and can defer loading`() {
        val eager = BuilderValueModel { "loaded" }
        val deferred = BuilderValueModel<String>(onInit = false) { "not loaded" }

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals("loaded", eager.data.value)
        assertNull(deferred.data.value)
    }
}

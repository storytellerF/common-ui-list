package com.storyteller_f.compat_ktx

import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CompatKtxTest {

    @Test
    @Config(sdk = [27])
    fun `compat bundle accessors use the legacy API before Android 13`() {
        val nested = Bundle().apply { putString("value", "nested") }
        val bundle = Bundle().apply {
            putSerializable("text", "value")
            putParcelable("nested", nested)
        }

        assertEquals("value", bundle.getSerializableCompat("text", String::class.java))
        assertEquals("nested", bundle.getParcelableCompat("nested", Bundle::class.java)?.getString("value"))
        assertNull(bundle.getSerializableCompat("missing", String::class.java))
        assertNull(bundle.getParcelableCompat("missing", Bundle::class.java))
    }

    @Test
    @Config(sdk = [35])
    fun `compat bundle accessors use the typed API from Android 13`() {
        val nested = Bundle().apply { putInt("value", 42) }
        val bundle = Bundle().apply {
            putSerializable("number", 42)
            putParcelable("nested", nested)
        }

        assertEquals(42, bundle.getSerializableCompat("number", Int::class.javaObjectType))
        assertEquals(42, bundle.getParcelableCompat("nested", Bundle::class.java)?.getInt("value"))
    }

    @Test
    @Config(sdk = [27])
    fun `package archive lookup supports the legacy Android API`() {
        assertNull(
            org.robolectric.RuntimeEnvironment.getApplication()
                .packageManager
                .packageInfoCompat("missing.apk")
        )
    }

    @Test
    @Config(sdk = [35])
    fun `package archive lookup supports the typed Android API`() {
        assertNull(
            org.robolectric.RuntimeEnvironment.getApplication()
                .packageManager
                .packageInfoCompat("missing.apk")
        )
    }

    @Test
    fun `useCompat returns the block result and always closes`() {
        var closed = false

        val result = "resource".useCompat(
            close = { closed = true },
            block = { it.length },
        )

        assertEquals(8, result)
        assertTrue(closed)
    }

    @Test(expected = IllegalStateException::class)
    fun `useCompat closes when the block fails`() {
        var closed = false

        try {
            "resource".useCompat(
                close = { closed = true },
                block = { throw IllegalStateException("failed") },
            )
        } finally {
            assertTrue(closed)
        }
    }
}

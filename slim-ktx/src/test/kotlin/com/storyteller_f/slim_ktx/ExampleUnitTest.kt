package com.storyteller_f.slim_ktx

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun testBit() {
        assertEquals(true, 3.bit(1))
        assertEquals(false, 2.bit(1))
    }

    @Test
    fun testSame() {
        assertEquals(false, listOf(1, 2).same(listOf(1)))
        assertEquals(true, listOf(1, 2).same(listOf(1, 2)))
        assertEquals(false, listOf(1, 2).same(listOf(2, 1)))
    }

    @Test
    fun testToggle() {
        val listOf = mutableListOf(1, 2)
        listOf.toggle(1)
        assertEquals(false, listOf.contains(1))
        assertEquals(true, listOf.contains(2))
        listOf.toggle(1)
        assertEquals(true, listOf.contains(1))
        assertEquals(true, listOf.contains(2))
    }
}

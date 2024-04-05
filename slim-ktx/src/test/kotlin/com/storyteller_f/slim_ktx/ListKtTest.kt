package com.storyteller_f.slim_ktx

import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Test

class ListKtTest {

    @Test
    fun testDup() {
        assertEquals(true, listOf(1, 2, 1).dup())
        assertEquals(false, listOf(1, 2).dup())
        assertEquals(false, listOf(1).dup())
    }

    @Test
    fun testToggleObject() {
        data class TestDatum(val v: String)

        assertEquals(true, listOf(TestDatum("hello")).contains(TestDatum("hello")))
    }

    @Test
    fun testBit() {
        Assert.assertEquals(true, 3.bit(1))
        Assert.assertEquals(false, 2.bit(1))
    }

    @Test
    fun testSame() {
        Assert.assertEquals(false, listOf(1, 2).same(listOf(1)))
        Assert.assertEquals(true, listOf(1, 2).same(listOf(1, 2)))
        Assert.assertEquals(false, listOf(1, 2).same(listOf(2, 1)))
    }

    @Test
    fun testToggle() {
        val listOf = mutableListOf(1, 2)
        listOf.toggle(1)
        Assert.assertEquals(false, listOf.contains(1))
        Assert.assertEquals(true, listOf.contains(2))
        listOf.toggle(1)
        Assert.assertEquals(true, listOf.contains(1))
        Assert.assertEquals(true, listOf.contains(2))
    }
}

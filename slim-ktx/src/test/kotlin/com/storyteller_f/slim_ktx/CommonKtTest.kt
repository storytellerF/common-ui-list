package com.storyteller_f.slim_ktx

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonKtTest {

    @Test
    fun testAnd() {
        assertEquals(true, and({
            true
        }, {
            true
        }))
        assertEquals(false, and({
            false
        }, {
            true
        }))
        assertEquals(false, and({
            true
        }, {
            false
        }))
    }

    @Test
    fun testOr() {
        assertEquals(false, or({
            false
        }, {
            false
        }))
        assertEquals(true, or({
            false
        }, {
            true
        }))
        assertEquals(true, or({
            true
        }, {
            false
        }))
    }

    @Test
    fun testPropertiesSame() {
        class TestResult(val v: String)
        assertEquals(true, TestResult("hello").propertiesSame(TestResult("world")))
        assertEquals(false, TestResult("hello").propertiesSame(TestResult("world"), {
            v
        }))
    }

    @Test
    fun testBuildMask() {
        assertEquals(3, buildMask {
            it.add(1)
            it.add(2)
        })
        assertEquals(1, buildMask {
            it.add(1)
            it.add(1)
        })
    }

    @Test
    fun testBit() {
        assertEquals(false, 2.bit(1))
        assertEquals(true, 1.bit(1))
        assertEquals(true, 3.bit(1))
    }
}

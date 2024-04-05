package com.storyteller_f.slim_ktx

import junit.framework.TestCase
import org.junit.Test


class CastKtTest {

    @Test
    fun testCast() {
        val a = 1
        val a1 = 2 as Any
        TestCase.assertEquals(2, a.cast(a1))
    }

}
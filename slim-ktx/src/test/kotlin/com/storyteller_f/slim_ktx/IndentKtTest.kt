package com.storyteller_f.slim_ktx

import junit.framework.TestCase.assertEquals
import org.junit.Test

class IndentKtTest {
    @Test
    fun testCast() {
        val a = 1
        val a1 = 2 as Any
        assertEquals(2, a.cast(a1))
    }
}

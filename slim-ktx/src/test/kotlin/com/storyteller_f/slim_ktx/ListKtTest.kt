package com.storyteller_f.slim_ktx

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ListKtTest {

    @Test
    fun testDup() {
        assertEquals(true, listOf(1, 2, 1).dup())
        assertEquals(false, listOf(1, 2).dup())
        assertEquals(false, listOf(1).dup())
    }
}

package com.storyteller_f.slim_ktx

import org.junit.Assert.assertEquals
import org.junit.Test

class IndexManagerTest {

    @Test
    fun `assigns an index once and can look up keys by index`() {
        val manager = IndexManager<String>()

        assertEquals(0, manager.getIndex("first"))
        assertEquals(1, manager.getIndex("second"))
        assertEquals(0, manager.getIndex("first"))
        assertEquals(mapOf("first" to 0, "second" to 1), manager.map)
        assertEquals(listOf("first", "second"), manager.list)
        assertEquals("first", manager.getKey(0))
        assertEquals("second", manager.getKey(1))
    }
}

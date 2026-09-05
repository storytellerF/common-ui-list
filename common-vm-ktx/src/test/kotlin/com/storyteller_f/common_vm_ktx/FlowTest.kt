package com.storyteller_f.common_vm_ktx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowTest {

    @Test
    fun `list helpers copy and update sparse lists`() {
        val values = mutableListOf<String?>()

        values.addOrSet(2, "third")

        assertEquals(listOf(null, null, "third"), values)
        assertEquals("third", values.gon(2))
        assertEquals(null, values.gon(3))
        assertEquals(values, copyList(values))
        assertEquals(listOf("third"), copyListNotNull(values))
    }

    @Test
    fun `combine exposes the latest value for every named source`() = runBlocking {
        val count = MutableStateFlow<Any?>(1)
        val label = MutableStateFlow<Any?>("one")

        val values = combine("count" to count, "label" to label).take(1).toList()

        assertEquals(listOf(mapOf("count" to 1, "label" to "one")), values)
    }

    @Test
    fun `toDiff omits equivalent consecutive values`() = runBlocking {
        val values = flowOf(1, 1, 2).toDiff { previous, current -> previous == current }.toList()

        assertEquals(listOf(null to 1, 1 to 2), values)
    }

    @Test
    fun `toDiffNoNull waits for two non-null different values`() = runBlocking {
        val values = flowOf<Int?>(null, 1, 1, 2, null, 3)
            .toDiffNoNull { previous, current -> previous == current }
            .toList()

        assertEquals(listOf(1 to 2), values)
    }

    @Test
    fun `distinctUntilChangedBy uses the supplied equivalence`() = runBlocking {
        val values = flowOf(1, 3, 2, 4).distinctUntilChangedBy { previous, current ->
            previous % 2 == current % 2
        }.toList()

        assertEquals(listOf(1, 2), values)
    }

    @Test
    fun `update replaces the current state value`() {
        val state = MutableStateFlow(1)

        state.update { it + 1 }

        assertEquals(2, state.value)
    }
}

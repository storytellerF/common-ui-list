package com.storyteller_f.common_vm_ktx

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowTest {

    @Test
    fun `combine exposes the latest value for every named source`() = runBlocking {
        val count = MutableStateFlow<Any?>(1)
        val label = MutableStateFlow<Any?>("one")

        val values = combine("count" to count, "label" to label).take(1).toList()

        assertEquals(listOf(mapOf("count" to 1, "label" to "one")), values)
    }

}

package com.storyteller_f.slim_ktx

import org.junit.Assert.assertEquals
import org.junit.Test

class SequenceKtTest {

    @Test
    fun `nested group by groups both keys transforms values and skips null keys`() {
        data class Record(val category: String?, val name: String, val value: Int)

        val grouped = sequenceOf(
            Record("fruit", "apple", 1),
            Record("fruit", "apple", 2),
            Record("fruit", "pear", 3),
            Record(null, "ignored", 4),
        ).nestedGroupBy(
            doubleKeySelector = { record -> record.category?.let { it to record.name } },
            valueTransform = { it.value * 10 },
        )

        assertEquals(
            mapOf(
                "fruit" to mapOf(
                    "apple" to listOf(10, 20),
                    "pear" to listOf(30),
                ),
            ),
            grouped,
        )
    }
}

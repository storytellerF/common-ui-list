package com.storyteller_f.slim_ktx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndentKtTest {

    @Test
    fun testIndent() {
        assertTrue("test\nhello".indentStartAt().startsWith("test"))
        assertTrue("test\nhello".indentStartAt(0).startsWith(" "))
    }


    @Test
    fun testReplaceCode() {
        assertEquals("hello", "$1".replaceCode("hello".no()))
        assertEquals("hello", "$1".replaceCode("hello".yes()))
        assertEquals("hello\n    world", "$1".replaceCode("hello\nworld".yes()))
        assertEquals("hello\n    world", "    $1".trimAndReplaceCode("hello\nworld".yes()))
    }
}

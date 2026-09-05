package com.storyteller_f.ui_list.source

import androidx.paging.LoadState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DetailHandlerTest {

    @Test
    fun `load uses local content and refresh uses the remote producer`() = runBlocking {
        val handler = DetailHandler(
            producer = { "remote" },
            local = { "cached" },
        )

        handler.load(this).join()
        assertEquals("cached", handler.content.value)
        assertTrue(handler.loadState.value is LoadState.NotLoading)

        handler.refresh(this).join()
        assertEquals("remote", handler.content.value)
        assertTrue(handler.loadState.value is LoadState.NotLoading)
    }

    @Test
    fun `load falls back to remote content when local content fails`() = runBlocking {
        val handler = DetailHandler(
            producer = { "remote" },
            local = { error("local cache unavailable") },
        )

        handler.load(this).join()

        assertEquals("remote", handler.content.value)
        assertTrue(handler.loadState.value is LoadState.NotLoading)
    }

    @Test
    fun `load exposes an error when the remote producer fails`() = runBlocking {
        val handler = DetailHandler<String>(producer = { error("network unavailable") })

        handler.load(this).join()

        assertTrue(handler.loadState.value is LoadState.Error)
        assertEquals(null, handler.content.value)
    }
}

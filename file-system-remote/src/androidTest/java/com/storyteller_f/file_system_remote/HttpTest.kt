package com.storyteller_f.file_system_remote

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HttpTest {
    companion object {
        private lateinit var server: MockWebServer

        @JvmStatic
        @BeforeClass
        fun init() {
            server = MockWebServer().apply {
                enqueue(MockResponse().apply {
                    addHeader("content-type", "text/plain")
                    addHeader("Content-Disposition", "attachment; filename=\"test.text\"")
                    setBody("hello world")
                })
                start()
            }
        }

        @JvmStatic
        @AfterClass
        fun close() {
            server.close()
        }
    }

    @Test
    fun test() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = "http://localhost:${server.port}".toUri()
        val httpFileInstance = HttpFileInstance(appContext, uri)
        runBlocking {
            httpFileInstance.getInputStream().bufferedReader().use {
                val readText = it.readText()
                assertEquals("hello world", readText)
            }
            assertTrue(httpFileInstance.isFile())
            assertFalse(httpFileInstance.isDirectory())
            assertEquals(11, httpFileInstance.getFileLength())
            assertFalse(httpFileInstance.createFile())
            assertFalse(httpFileInstance.createDirectory())
        }
    }
}
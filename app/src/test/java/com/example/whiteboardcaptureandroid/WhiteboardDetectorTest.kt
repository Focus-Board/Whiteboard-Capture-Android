package com.example.whiteboardcaptureandroid

import android.content.Context
import android.graphics.Bitmap
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class WhiteboardDetectorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockBitmap: Bitmap

    private lateinit var detector: WhiteboardDetector

    @Before
    fun setup() {
        `when`(mockContext.assets).thenReturn(null)
        detector = WhiteboardDetector(mockContext)
    }

    @Test
    fun `fallback polygon has four corners`() {
        val corners = detector.detectWhiteboard(mockBitmap)
        assertEquals(4, corners?.size)
    }

    @Test
    fun `fallback corners are normalized`() {
        val corners = detector.detectWhiteboard(mockBitmap)
        corners?.forEach { (x, y) ->
            assertTrue(x in 0f..1f)
            assertTrue(y in 0f..1f)
        }
    }

    @Test
    fun `fallback polygon forms rectangle`() {
        val corners = detector.detectWhiteboard(mockBitmap)
        assertEquals(Pair(0.1f, 0.1f), corners?.get(0))
        assertEquals(Pair(0.9f, 0.1f), corners?.get(1))
        assertEquals(Pair(0.9f, 0.9f), corners?.get(2))
        assertEquals(Pair(0.1f, 0.9f), corners?.get(3))
    }

    @Test
    fun `detector close does not crash`() {
        detector.close()
    }

    @Test
    fun `multiple detections return same result`() {
        val result1 = detector.detectWhiteboard(mockBitmap)
        val result2 = detector.detectWhiteboard(mockBitmap)
        assertEquals(result1, result2)
    }
}
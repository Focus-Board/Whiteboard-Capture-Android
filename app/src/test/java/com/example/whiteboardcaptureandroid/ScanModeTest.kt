package com.example.whiteboardcaptureandroid

import org.junit.Test
import org.junit.Assert.*

class ScanModeTest {

    @Test
    fun `ml kit scanner mode has correct properties`() {
        assertEquals("ML Kit Scanner", ScanMode.ML_KIT.displayName)
        assertEquals("Google-hosted document scanner UI", ScanMode.ML_KIT.description)
    }

    @Test
    fun `whiteboard beta scanner mode has correct properties`() {
        assertEquals("Whiteboard Beta Scanner", ScanMode.WHITEBOARD_BETA.displayName)
        assertEquals("Experimental in-app whiteboard capture", ScanMode.WHITEBOARD_BETA.description)
    }

    @Test
    fun `fromOrdinal returns correct modes`() {
        assertEquals(ScanMode.ML_KIT, ScanMode.fromOrdinal(0))
        assertEquals(ScanMode.WHITEBOARD_BETA, ScanMode.fromOrdinal(1))
    }

    @Test
    fun `enum has exactly two modes`() {
        assertEquals(2, ScanMode.values().size)
    }
}
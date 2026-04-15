package com.example.whiteboardcaptureandroid

import org.junit.Test
import org.junit.Assert.*

class ScanModeTest {

    @Test
    fun `whiteboard mode has correct properties`() {
        assertEquals("Whiteboard Beta", ScanMode.WHITEBOARD.displayName)
        assertEquals("Experimental whiteboard detection", ScanMode.WHITEBOARD.description)
    }

    @Test
    fun `document mode has correct properties`() {
        assertEquals("Auto Scan", ScanMode.DOCUMENT.displayName)
        assertEquals("Default document capture", ScanMode.DOCUMENT.description)
    }

    @Test
    fun `fromOrdinal returns correct modes`() {
        assertEquals(ScanMode.DOCUMENT, ScanMode.fromOrdinal(0))
        assertEquals(ScanMode.WHITEBOARD, ScanMode.fromOrdinal(1))
    }

    @Test
    fun `enum has exactly two modes`() {
        assertEquals(2, ScanMode.values().size)
    }
}
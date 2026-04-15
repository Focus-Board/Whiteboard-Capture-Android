package com.example.whiteboardcaptureandroid

import org.junit.Test
import org.junit.Assert.*

class ScanModeTest {

    @Test
    fun `whiteboard mode has correct properties`() {
        assertEquals("Whiteboard", ScanMode.WHITEBOARD.displayName)
        assertEquals("Detect whiteboards and clean them", ScanMode.WHITEBOARD.description)
    }

    @Test
    fun `document mode has correct properties`() {
        assertEquals("Document", ScanMode.DOCUMENT.displayName)
        assertEquals("Scan documents with auto-crop", ScanMode.DOCUMENT.description)
    }

    @Test
    fun `fromOrdinal returns correct modes`() {
        assertEquals(ScanMode.WHITEBOARD, ScanMode.fromOrdinal(0))
        assertEquals(ScanMode.DOCUMENT, ScanMode.fromOrdinal(1))
    }

    @Test
    fun `enum has exactly two modes`() {
        assertEquals(2, ScanMode.values().size)
    }
}
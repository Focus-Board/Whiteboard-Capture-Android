package com.example.whiteboardcaptureandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanModeAdapterTest {

    private lateinit var adapter: ScanModeAdapter
    private var callbackInvoked = false

    @Before
    fun setup() {
        val modes = listOf(ScanMode.WHITEBOARD, ScanMode.DOCUMENT)
        adapter = ScanModeAdapter(modes) { callbackInvoked = true }
    }

    @Test
    fun adapter_hasCorrectItemCount() {
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun setSelectedMode_whiteboard() {
        adapter.setSelectedMode(ScanMode.WHITEBOARD)
        // No crash = pass
    }

    @Test
    fun setSelectedMode_document() {
        adapter.setSelectedMode(ScanMode.DOCUMENT)
        // No crash = pass
    }

    @Test
    fun adapter_handlesModeList() {
        val modes = listOf(ScanMode.WHITEBOARD, ScanMode.DOCUMENT)
        val testAdapter = ScanModeAdapter(modes) {}
        assertEquals(2, testAdapter.itemCount)
    }
}
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
        val modes = listOf(ScanMode.ML_KIT, ScanMode.WHITEBOARD_BETA)
        adapter = ScanModeAdapter(modes) { callbackInvoked = true }
    }

    @Test
    fun adapter_hasCorrectItemCount() {
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun setSelectedMode_mlKit() {
        adapter.setSelectedMode(ScanMode.ML_KIT)
        // No crash = pass
    }

    @Test
    fun setSelectedMode_whiteboardBeta() {
        adapter.setSelectedMode(ScanMode.WHITEBOARD_BETA)
        // No crash = pass
    }

    @Test
    fun adapter_handlesModeList() {
        val modes = listOf(ScanMode.ML_KIT, ScanMode.WHITEBOARD_BETA)
        val testAdapter = ScanModeAdapter(modes) {}
        assertEquals(2, testAdapter.itemCount)
    }
}
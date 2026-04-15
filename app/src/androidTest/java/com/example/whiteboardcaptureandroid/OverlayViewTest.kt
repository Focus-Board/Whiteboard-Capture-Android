package com.example.whiteboardcaptureandroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayViewTest {

    private lateinit var overlayView: OverlayView

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        overlayView = OverlayView(context)
    }

    @Test
    fun updatePolygon_acceptsFourCorners() {
        val corners = listOf(
            Pair(0.1f, 0.1f), Pair(0.9f, 0.1f),
            Pair(0.9f, 0.9f), Pair(0.1f, 0.9f)
        )
        overlayView.updatePolygon(corners)
        // No crash = pass
    }

    @Test
    fun updatePolygon_acceptsNull() {
        overlayView.updatePolygon(null)
        // No crash = pass
    }

    @Test
    fun setDetectionMode_whiteboard() {
        overlayView.setDetectionMode(OverlayView.DetectionMode.WHITEBOARD)
        // No crash = pass
    }

    @Test
    fun setDetectionMode_document() {
        overlayView.setDetectionMode(OverlayView.DetectionMode.DOCUMENT)
        // No crash = pass
    }

    @Test
    fun setDetectionMode_multipleSwitch() {
        overlayView.setDetectionMode(OverlayView.DetectionMode.WHITEBOARD)
        overlayView.setDetectionMode(OverlayView.DetectionMode.DOCUMENT)
        overlayView.setDetectionMode(OverlayView.DetectionMode.WHITEBOARD)
        // No crash = pass
    }
}
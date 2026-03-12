package com.example.whiteboardcaptureandroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * Custom view that draws detected whiteboard boundaries.
 * Shows a dynamic polygon that adapts to the detected shape.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Current detected polygon (null if nothing detected)
    private var polygon: List<Pair<Float, Float>>? = null

    // Paint for drawing the polygon outline
    private val linePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    // Paint for corner circles
    private val cornerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Paint for semi-transparent fill
    private val fillPaint = Paint().apply {
        color = Color.argb(50, 0, 255, 0)  // Semi-transparent green
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    companion object {
        private const val CORNER_RADIUS = 12f
    }

    /**
     * Update the polygon to draw.
     * @param corners List of 4 corner points (normalized 0-1 coordinates)
     */
    fun updatePolygon(corners: List<Pair<Float, Float>>?) {
        polygon = corners
        invalidate()  // Request redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val corners = polygon
        if (corners == null || corners.size != 4) {
            // No detection - draw nothing
            return
        }

        // Convert normalized coordinates to screen pixels
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val path = Path()

        // Start at first corner
        val firstPoint = corners[0]
        path.moveTo(
            firstPoint.first * viewWidth,
            firstPoint.second * viewHeight
        )

        // Draw lines to each corner
        for (i in 1 until corners.size) {
            val point = corners[i]
            path.lineTo(
                point.first * viewWidth,
                point.second * viewHeight
            )
        }

        // Close the path
        path.close()

        // Draw filled polygon (semi-transparent)
        canvas.drawPath(path, fillPaint)

        // Draw polygon outline
        canvas.drawPath(path, linePaint)

        // Draw corner circles
        for ((x, y) in corners) {
            canvas.drawCircle(
                x * viewWidth,
                y * viewHeight,
                CORNER_RADIUS,
                cornerPaint
            )
        }
    }
}
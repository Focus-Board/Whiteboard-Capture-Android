package com.example.whiteboardcaptureandroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class DetectionMode {
        WHITEBOARD,
        DOCUMENT
    }

    private var polygon: List<Pair<Float, Float>>? = null
    private var detectionMode: DetectionMode = DetectionMode.WHITEBOARD

    private val linePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val fillPaint = Paint().apply {
        color = Color.argb(50, 0, 255, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    companion object {
        private const val CORNER_RADIUS = 12f
    }

    fun updatePolygon(corners: List<Pair<Float, Float>>?) {
        polygon = corners
        invalidate()
    }

    fun setDetectionMode(mode: DetectionMode) {
        detectionMode = mode

        // Update colors based on mode
        when (mode) {
            DetectionMode.WHITEBOARD -> {
                linePaint.color = Color.GREEN
                fillPaint.color = Color.argb(50, 0, 255, 0)
                cornerPaint.color = Color.RED
            }
            DetectionMode.DOCUMENT -> {
                linePaint.color = Color.BLUE
                fillPaint.color = Color.argb(50, 0, 150, 255)
                cornerPaint.color = Color.CYAN
            }
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val corners = polygon
        if (corners == null || corners.size != 4) {
            return
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val path = Path()

        val firstPoint = corners[0]
        path.moveTo(
            firstPoint.first * viewWidth,
            firstPoint.second * viewHeight
        )

        for (i in 1 until corners.size) {
            val point = corners[i]
            path.lineTo(
                point.first * viewWidth,
                point.second * viewHeight
            )
        }

        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, linePaint)

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
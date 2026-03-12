package com.example.whiteboardcaptureandroid

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

class WhiteboardDetector(context: Context) {

    private var interpreter: Interpreter? = null
    private val inputSize = 640

    // Class indices for v2 model
    private val CLASS_HANDWRITING = 0
    private val CLASS_WHITEBOARD = 1

    companion object {
        private const val TAG = "WhiteboardDetector"
        private const val MODEL_FILE = "best_int8.tflite"
        private const val CONFIDENCE_THRESHOLD = 0.30f
        private const val IOU_THRESHOLD = 0.5f
    }

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, MODEL_FILE)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}", e)
        }
    }

    fun detectWhiteboard(bitmap: Bitmap): List<Pair<Float, Float>>? {
        val currentInterpreter = interpreter ?: return getFallbackPolygon()

        Log.d(TAG, "Input bitmap: ${bitmap.width}x${bitmap.height}")

        try {
            // Calculate letterbox padding to maintain aspect ratio
            val scale = min(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            val padX = (inputSize - scaledWidth) / 2
            val padY = (inputSize - scaledHeight) / 2

            // Resize with letterbox (not stretch)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            val paddedBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paddedBitmap)
            canvas.drawColor(0xFF808080.toInt()) // Gray padding (YOLO default)
            canvas.drawBitmap(scaledBitmap, padX.toFloat(), padY.toFloat(), null)

            val inputBuffer = bitmapToByteBuffer(paddedBitmap)
            val outputArray = Array(1) { Array(300) { FloatArray(6) } }

            currentInterpreter.run(inputBuffer, outputArray)

            // Find best WHITEBOARD detection (class index 1, which is index 5 in output)
            val allDetections = outputArray[0]
            var bestDetection: FloatArray? = null
            var maxConfidence = 0f

            for (i in 0 until 300) {
                // Index 5 = Whiteboard confidence (not index 4!)
                val whiteboardConf = allDetections[i][4 + CLASS_WHITEBOARD]
                if (whiteboardConf > maxConfidence && whiteboardConf > CONFIDENCE_THRESHOLD) {
                    maxConfidence = whiteboardConf
                    bestDetection = allDetections[i]
                }
            }

            val finalDetection = bestDetection
            if (finalDetection != null) {
                // TFLite outputs CORNER format: [xmin, ymin, xmax, ymax, ...]
                // All values are NORMALIZED (0-1) relative to 640x640
                var xmin = finalDetection[0]
                var ymin = finalDetection[1]
                var xmax = finalDetection[2]
                var ymax = finalDetection[3]

                Log.d(TAG, "Raw detection: [$xmin, $ymin, $xmax, $ymax] conf=$maxConfidence")
                Log.d(TAG, "Letterbox: scale=$scale, padX=$padX, padY=$padY")

                // Convert from 640x640 padded space to pixel coords
                xmin = xmin * inputSize
                ymin = ymin * inputSize
                xmax = xmax * inputSize
                ymax = ymax * inputSize

                // Remove letterbox padding and scale back to original image coordinates
                xmin = (xmin - padX) / scale / bitmap.width
                ymin = (ymin - padY) / scale / bitmap.height
                xmax = (xmax - padX) / scale / bitmap.width
                ymax = (ymax - padY) / scale / bitmap.height

                Log.d(TAG, "After transform: [$xmin, $ymin, $xmax, $ymax]")

                // Clamp to valid range
                xmin = max(0f, min(1f, xmin))
                ymin = max(0f, min(1f, ymin))
                xmax = max(0f, min(1f, xmax))
                ymax = max(0f, min(1f, ymax))

                Log.d(TAG, "Detected whiteboard: conf=$maxConfidence, box=[$xmin,$ymin,$xmax,$ymax]")

                // Check for suspicious values
                if (xmax - xmin < 0.05f || ymax - ymin < 0.05f) {
                    Log.w(TAG, "Detection too small, likely false positive")
                    return getFallbackPolygon()
                }

                return listOf(
                    Pair(xmin, ymin),  // top-left
                    Pair(xmax, ymin),  // top-right
                    Pair(xmax, ymax),  // bottom-right
                    Pair(xmin, ymax)   // bottom-left
                )
            } else {
                Log.d(TAG, "No whiteboard detected above threshold")
                return getFallbackPolygon()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Detection failed", e)
            return getFallbackPolygon()
        }
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            // RGB order, normalized to 0-1
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }

    private fun getFallbackPolygon(): List<Pair<Float, Float>> {
        return listOf(
            Pair(0.1f, 0.1f), Pair(0.9f, 0.1f),
            Pair(0.9f, 0.9f), Pair(0.1f, 0.9f)
        )
    }

    fun close() {
        interpreter?.close()
    }
}
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

        try {
            val scale = min(inputSize.toFloat() / bitmap.width, inputSize.toFloat() / bitmap.height)
            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()
            val padX = (inputSize - scaledWidth) / 2
            val padY = (inputSize - scaledHeight) / 2

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
            val paddedBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paddedBitmap)
            canvas.drawColor(0xFF808080.toInt())
            canvas.drawBitmap(scaledBitmap, padX.toFloat(), padY.toFloat(), null)

            val inputBuffer = bitmapToByteBuffer(paddedBitmap)
            val outputArray = Array(1) { Array(300) { FloatArray(6) } }

            currentInterpreter.run(inputBuffer, outputArray)

            val allDetections = outputArray[0]
            var bestDetection: FloatArray? = null
            var maxConfidence = 0f

            for (i in 0 until 300) {
                val whiteboardConf = allDetections[i][4 + CLASS_WHITEBOARD]
                if (whiteboardConf > maxConfidence && whiteboardConf > CONFIDENCE_THRESHOLD) {
                    maxConfidence = whiteboardConf
                    bestDetection = allDetections[i]
                }
            }

            val finalDetection = bestDetection
            if (finalDetection != null) {
                var xmin = finalDetection[0]
                var ymin = finalDetection[1]
                var xmax = finalDetection[2]
                var ymax = finalDetection[3]

                xmin = xmin * inputSize
                ymin = ymin * inputSize
                xmax = xmax * inputSize
                ymax = ymax * inputSize

                xmin = (xmin - padX) / scale / bitmap.width
                ymin = (ymin - padY) / scale / bitmap.height
                xmax = (xmax - padX) / scale / bitmap.width
                ymax = (ymax - padY) / scale / bitmap.height

                xmin = max(0f, min(1f, xmin))
                ymin = max(0f, min(1f, ymin))
                xmax = max(0f, min(1f, xmax))
                ymax = max(0f, min(1f, ymax))

                if (xmax - xmin < 0.05f || ymax - ymin < 0.05f) {
                    return getFallbackPolygon()
                }

                return listOf(
                    Pair(xmin, ymin),
                    Pair(xmax, ymin),
                    Pair(xmax, ymax),
                    Pair(xmin, ymax)
                )
            } else {
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
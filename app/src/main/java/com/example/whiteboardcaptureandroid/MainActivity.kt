package com.example.whiteboardcaptureandroid

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whiteboardcaptureandroid.WhiteboardDetector
import com.example.whiteboardcaptureandroid.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: WhiteboardDetector

    companion object {
        private const val TAG = "WhiteboardScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "Activity created")

            cameraExecutor = Executors.newSingleThreadExecutor()
            detector = WhiteboardDetector(this)

            binding.statusText.text = "Initializing..."

            binding.captureButton.setOnClickListener {
                binding.statusText.text = "Capture clicked!"
            }

            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        binding.statusText.text = "Starting camera..."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                // Image Analysis - Process frames for detection
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processFrame(imageProxy)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer  // Add analyzer
                )

                binding.statusText.text = "Detecting whiteboard..."
                Log.d(TAG, "Camera started with ML detection")

            } catch (e: Exception) {
                Log.e(TAG, "Camera failed", e)
                binding.statusText.text = "Camera error"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Process each camera frame for whiteboard detection
     */
    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)

            // Run ML detection
            val corners = detector.detectWhiteboard(bitmap)

            // Update overlay on UI thread
            runOnUiThread {
                binding.overlayView.updatePolygon(corners)
            }

            bitmap.recycle()

        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Convert CameraX ImageProxy to Bitmap (with rotation correction)
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image!!

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()

        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        // CRITICAL: Rotate bitmap to match preview orientation
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                bitmap.recycle()
            }
        } else {
            bitmap
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        detector.close()
    }
}
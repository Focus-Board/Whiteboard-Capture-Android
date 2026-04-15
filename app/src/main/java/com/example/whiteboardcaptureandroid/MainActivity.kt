package com.example.whiteboardcaptureandroid

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whiteboardcaptureandroid.databinding.ActivityMainBinding
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var detector: WhiteboardDetector
    private lateinit var modeAdapter: ScanModeAdapter
    private lateinit var documentScannerHelper: DocumentScannerHelper

    private var currentMode: ScanMode = ScanMode.WHITEBOARD
    private var camera: Camera? = null
    private var isFlashOn = false

    companion object {
        private const val TAG = "WhiteboardScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val EXTRA_DISABLE_CAMERA_START = "extra_disable_camera_start"
    }

    // ML Kit Scanner Launcher
    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            documentScannerHelper.handleScanResult(scanResult)
        } else {
            // User cancelled - switch back to whiteboard mode
            Log.d(TAG, "ML Kit scan cancelled, switching to Whiteboard mode")
            switchToWhiteboardMode()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "Activity created")

            cameraExecutor = Executors.newSingleThreadExecutor()
            detector = WhiteboardDetector(this)

            // Initialize Document Scanner
            documentScannerHelper = DocumentScannerHelper(
                activity = this,
                scannerLauncher = scannerLauncher,
                onScanComplete = { imageUris, _ ->
                    handleScannedDocument(imageUris)
                },
                onScanError = { error ->
                    Toast.makeText(this, "Scan failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    switchToWhiteboardMode()
                }
            )

            setupModeSelector()
            setupButtons()
            updateStatusForMode(currentMode)

            val disableCameraForTest = intent?.getBooleanExtra(EXTRA_DISABLE_CAMERA_START, false) == true
            if (disableCameraForTest) {
                Log.d(TAG, "Camera startup disabled by test intent extra")
                updateStatusForMode(currentMode)
            } else if (allPermissionsGranted()) {
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

    private fun setupButtons() {
        // Capture button - behavior depends on mode
        binding.captureButton.setOnClickListener {
            when (currentMode) {
                ScanMode.WHITEBOARD -> captureWhiteboardImage()
                ScanMode.DOCUMENT -> documentScannerHelper.startScan()
            }
        }

        // Flash toggle
        binding.flashButton.setOnClickListener {
            toggleFlash()
        }

        // Gallery button
        binding.galleryButton.setOnClickListener {
            openGallery()
        }
    }

    private fun toggleFlash() {
        camera?.let { cam ->
            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)

            binding.flashButton.setImageResource(
                if (isFlashOn) R.drawable.ic_flash_on
                else R.drawable.ic_flash_off
            )

            Toast.makeText(
                this,
                if (isFlashOn) "Flash ON" else "Flash OFF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openGallery() {
        Toast.makeText(this, "Gallery feature coming soon!", Toast.LENGTH_SHORT).show()
        // TODO: Implement gallery picker
    }

    private fun setupModeSelector() {
        val modes = listOf(
            ScanMode.WHITEBOARD,
            ScanMode.DOCUMENT
        )

        modeAdapter = ScanModeAdapter(modes) { selectedMode ->
            onModeChanged(selectedMode)
        }

        binding.modeRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = modeAdapter
        }
    }

    private fun onModeChanged(mode: ScanMode) {
        Log.d(TAG, "Mode changing from $currentMode to $mode")

        currentMode = mode
        updateStatusForMode(mode)

        when (mode) {
            ScanMode.WHITEBOARD -> {
                // Show camera with TFLite overlay
                showCameraPreview()
                binding.overlayView.setDetectionMode(OverlayView.DetectionMode.WHITEBOARD)
            }
            ScanMode.DOCUMENT -> {
                // Immediately launch ML Kit scanner
                Log.d(TAG, "Launching ML Kit scanner for Document mode")
                documentScannerHelper.startScan()
                binding.overlayView.setDetectionMode(OverlayView.DetectionMode.DOCUMENT)
            }
        }
    }

    private fun showCameraPreview() {
        // Make camera visible again
        binding.previewView.alpha = 1f
        binding.overlayView.alpha = 1f
    }

    private fun switchToWhiteboardMode() {
        // Programmatically switch back to whiteboard
        runOnUiThread {
            currentMode = ScanMode.WHITEBOARD
            modeAdapter.setSelectedMode(ScanMode.WHITEBOARD)
            updateStatusForMode(ScanMode.WHITEBOARD)
            showCameraPreview()
            binding.overlayView.setDetectionMode(OverlayView.DetectionMode.WHITEBOARD)
        }
    }

    private fun updateStatusForMode(mode: ScanMode) {
        binding.statusText.text = when (mode) {
            ScanMode.WHITEBOARD -> "Point at whiteboard"
            ScanMode.DOCUMENT -> "Opening scanner..."
        }
    }

    private fun captureWhiteboardImage() {
        binding.statusText.text = "Capturing whiteboard..."
        Toast.makeText(this, "Capturing whiteboard...", Toast.LENGTH_SHORT).show()

        // Animate capture button
        animateCaptureButton()

        // TODO: Implement actual capture with perspective correction

        binding.statusText.postDelayed({
            updateStatusForMode(currentMode)
        }, 2000)
    }

    private fun handleScannedDocument(imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No document scanned", Toast.LENGTH_SHORT).show()
            switchToWhiteboardMode()
            return
        }

        Log.d(TAG, "Document scanned: ${imageUris[0]}")
        Toast.makeText(this, "Document scanned successfully!", Toast.LENGTH_SHORT).show()

        // TODO: Open preview activity with scanned image
        binding.statusText.text = "Scanned: ${imageUris.size} page(s)"

        // Switch back to whiteboard mode after scan
        binding.statusText.postDelayed({
            switchToWhiteboardMode()
        }, 2000)
    }

    private fun animateCaptureButton() {
        binding.captureButton.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                binding.captureButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun startCamera() {
        binding.statusText.text = "Starting camera..."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

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

                // Bind and save camera reference
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                updateStatusForMode(currentMode)
                Log.d(TAG, "Camera started with ML detection")

            } catch (e: Exception) {
                Log.e(TAG, "Camera failed", e)
                binding.statusText.text = "Camera error"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Only run TFLite detection in Whiteboard mode
            if (currentMode == ScanMode.WHITEBOARD) {
                val bitmap = imageProxyToBitmap(imageProxy)
                val corners = detector.detectWhiteboard(bitmap)

                runOnUiThread {
                    binding.overlayView.updatePolygon(corners)
                }

                bitmap.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error", e)
        } finally {
            imageProxy.close()
        }
    }

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
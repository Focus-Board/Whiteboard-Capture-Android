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
import android.view.View
import android.view.MenuItem
import android.widget.PopupMenu
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
    private lateinit var scannerAdapter: ScanModeAdapter
    private lateinit var documentScannerHelper: DocumentScannerHelper

    private var currentWorkflowMode: WorkflowMode = WorkflowMode.CALENDAR_ENTRY
    private var currentScannerMode: ScanMode = ScanMode.ML_KIT
    private var isWorkflowSelected = false
    private var isAutoLaunchDisabled = false
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var isCameraStarted = false
    private var isFlashOn = false

    companion object {
        private const val TAG = "WhiteboardScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val EXTRA_DISABLE_CAMERA_START = "extra_disable_camera_start"
        const val EXTRA_DISABLE_AUTO_LAUNCH = "extra_disable_auto_launch"
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
            refreshStatus()
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
                    refreshStatus()
                }
            )

            setupModeSelector()
            setupWorkflowMenu()
            setupButtons()
            showLaunchScreen()

            val disableCameraForTest = intent?.getBooleanExtra(EXTRA_DISABLE_CAMERA_START, false) == true
            isAutoLaunchDisabled = intent?.getBooleanExtra(EXTRA_DISABLE_AUTO_LAUNCH, false) == true
            if (disableCameraForTest) {
                Log.d(TAG, "Startup action disabled by test intent extra")
            } else if (!allPermissionsGranted()) {
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
        // Capture button - behavior depends on the selected scanner
        binding.captureButton.setOnClickListener {
            when (currentScannerMode) {
                ScanMode.ML_KIT -> launchMlKitScanner()
                ScanMode.WHITEBOARD_BETA -> captureWhiteboardImage()
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

        binding.startCalendarButton.setOnClickListener {
            selectWorkflow(WorkflowMode.CALENDAR_ENTRY)
        }

        binding.startVectorButton.setOnClickListener {
            selectWorkflow(WorkflowMode.VECTOR_CONVERSION)
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
            ScanMode.ML_KIT,
            ScanMode.WHITEBOARD_BETA
        )

        scannerAdapter = ScanModeAdapter(modes) { selectedScannerMode ->
            onScannerModeChanged(selectedScannerMode)
        }

        binding.modeRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = scannerAdapter
        }

        scannerAdapter.setSelectedMode(currentScannerMode)
    }

    private fun setupWorkflowMenu() {
        binding.menuButton.setOnClickListener {
            showWorkflowMenu()
        }
    }

    private fun showWorkflowMenu() {
        PopupMenu(this, binding.menuButton).apply {
            menuInflater.inflate(R.menu.workflow_menu, menu)
            setOnMenuItemClickListener { item ->
                handleWorkflowMenuSelection(item)
            }
            show()
        }
    }

    private fun handleWorkflowMenuSelection(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.workflow_calendar_entry -> {
                updateWorkflowMode(WorkflowMode.CALENDAR_ENTRY)
                true
            }
            R.id.workflow_vector_conversion -> {
                updateWorkflowMode(WorkflowMode.VECTOR_CONVERSION)
                true
            }
            else -> false
        }
    }

    private fun updateWorkflowMode(mode: WorkflowMode) {
        Log.d(TAG, "Workflow changing from $currentWorkflowMode to $mode")

        currentWorkflowMode = mode
        refreshStatus()
    }

    private fun onScannerModeChanged(mode: ScanMode) {
        Log.d(TAG, "Scanner changing from $currentScannerMode to $mode")

        currentScannerMode = mode
        refreshStatus()

        if (isWorkflowSelected && !isAutoLaunchDisabled) {
            launchCurrentScanner()
        }
    }

    private fun showCameraPreview() {
        // Make camera visible again
        binding.previewView.alpha = 1f
        binding.overlayView.alpha = 1f
    }

    private fun showLaunchScreen() {
        binding.startupContainer.visibility = View.VISIBLE
        binding.mainContentContainer.visibility = View.GONE
        refreshStatus()
    }

    private fun hideLaunchScreen() {
        binding.startupContainer.visibility = View.GONE
        binding.mainContentContainer.visibility = View.VISIBLE
    }

    private fun selectWorkflow(mode: WorkflowMode) {
        updateWorkflowMode(mode)
        isWorkflowSelected = true
        hideLaunchScreen()
        refreshStatus()

        if (!isAutoLaunchDisabled) {
            launchCurrentScanner()
        }
    }

    private fun hideCameraPreview() {
        binding.previewView.alpha = 0f
        binding.overlayView.alpha = 0f
    }

    private fun launchMlKitScanner() {
        Log.d(TAG, "Launching ML Kit scanner")
        stopCamera()
        hideCameraPreview()
        binding.overlayView.setDetectionMode(OverlayView.DetectionMode.DOCUMENT)
        documentScannerHelper.startScan()
    }

    private fun launchCurrentScanner() {
        when (currentScannerMode) {
            ScanMode.ML_KIT -> launchMlKitScanner()
            ScanMode.WHITEBOARD_BETA -> {
                if (allPermissionsGranted()) {
                    startCamera()
                } else {
                    ActivityCompat.requestPermissions(
                        this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
                    )
                }
            }
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        isCameraStarted = false
    }

    private fun refreshStatus() {
        if (!isWorkflowSelected) {
            binding.statusText.text = getString(R.string.startupPrompt)
            return
        }

        val workflowText = when (currentWorkflowMode) {
            WorkflowMode.CALENDAR_ENTRY -> getString(R.string.workflowCalendarStatus)
            WorkflowMode.VECTOR_CONVERSION -> getString(R.string.workflowVectorStatus)
        }

        val scannerText = when (currentScannerMode) {
            ScanMode.ML_KIT -> getString(R.string.scannerMlKit)
            ScanMode.WHITEBOARD_BETA -> getString(R.string.scannerWhiteboardBeta)
        }

        binding.statusText.text = "$workflowText · $scannerText"
    }

    private fun captureWhiteboardImage() {
        binding.statusText.text = "Capturing whiteboard..."
        Toast.makeText(this, "Capturing whiteboard...", Toast.LENGTH_SHORT).show()

        // Animate capture button
        animateCaptureButton()

        // TODO: Implement local capture -> canvas/vector conversion entry point.
        when (currentWorkflowMode) {
            WorkflowMode.CALENDAR_ENTRY -> {
                binding.statusText.text = "Review calendar capture locally before server upload"
            }
            WorkflowMode.VECTOR_CONVERSION -> {
                binding.statusText.text = "Vector conversion running locally"
            }
        }

        binding.statusText.postDelayed({
            refreshStatus()
        }, 2000)
    }

    private fun handleScannedDocument(imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No document scanned", Toast.LENGTH_SHORT).show()
            refreshStatus()
            return
        }

        Log.d(TAG, "Document scanned: ${imageUris[0]}")
        Toast.makeText(this, "Document scanned successfully!", Toast.LENGTH_SHORT).show()

        when (currentWorkflowMode) {
            WorkflowMode.CALENDAR_ENTRY -> {
                binding.statusText.text = "Calendar entry capture ready for server approval"
            }
            WorkflowMode.VECTOR_CONVERSION -> {
                binding.statusText.text = "Vector conversion ready for local processing"
            }
        }

        binding.statusText.postDelayed({
            refreshStatus()
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
        if (isCameraStarted) {
            showCameraPreview()
            return
        }

        binding.statusText.text = "Starting camera..."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

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

                provider.unbindAll()

                // Bind and save camera reference
                camera = provider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                isCameraStarted = true
                showCameraPreview()

                refreshStatus()
                Log.d(TAG, "Camera started with ML detection")

            } catch (e: Exception) {
                Log.e(TAG, "Camera failed", e)
                binding.statusText.text = "Camera error"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            // Only run TFLite detection in whiteboard beta scanner mode
            if (currentScannerMode == ScanMode.WHITEBOARD_BETA) {
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
                if (isWorkflowSelected && !isAutoLaunchDisabled) {
                    launchCurrentScanner()
                }
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        cameraExecutor.shutdown()
        detector.close()
    }
}
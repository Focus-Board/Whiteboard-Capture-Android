package com.example.whiteboardcaptureandroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.whiteboardcaptureandroid.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // View binding - gives us access to UI elements
    private lateinit var binding: ActivityMainBinding

    // Camera executor - runs camera operations in background
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "WhiteboardScanner"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Update status
        binding.statusText.text = "Checking permissions..."

        // Check if we have camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up capture button click listener
        binding.captureButton.setOnClickListener {
            captureImage()
        }

        // Create background executor for camera
        cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun startCamera() {
        binding.statusText.text = "Starting camera..."

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                // Get camera provider
                val cameraProvider = cameraProviderFuture.get()

                // Build preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.previewView.surfaceProvider)
                    }

                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind camera to lifecycle
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

                // Update status
                binding.statusText.text = "Ready to scan"
                Log.d(TAG, "Camera started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                binding.statusText.text = "Camera failed: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Capture image (placeholder for now)
     */
    private fun captureImage() {
        binding.statusText.text = "Capturing..."

        // TODO: Implement actual capture in next phase
        Toast.makeText(this, "Capture button clicked!", Toast.LENGTH_SHORT).show()

        // Reset status after a moment
        binding.statusText.postDelayed({
            binding.statusText.text = "Ready to scan"
        }, 1000)
    }

    /**
     * Check if all required permissions are granted
     */
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
                Toast.makeText(
                    this,
                    "Camera permission is required",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
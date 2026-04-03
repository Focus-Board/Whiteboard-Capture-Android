package com.example.whiteboardcaptureandroid

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class DocumentScannerHelper(
    private val activity: Activity,
    private val scannerLauncher: ActivityResultLauncher<IntentSenderRequest>,
    private val onScanComplete: (List<Uri>, Bitmap?) -> Unit,
    private val onScanError: (Exception) -> Unit
) {

    private val scanner: GmsDocumentScanner

    companion object {
        private const val TAG = "DocumentScanner"
    }

    init {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setPageLimit(1)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()

        scanner = GmsDocumentScanning.getClient(options)
    }

    fun startScan() {
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                Log.d(TAG, "Starting ML Kit scanner")
                scannerLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to start scanner", e)
                onScanError(e)
            }
    }

    fun handleScanResult(result: GmsDocumentScanningResult?) {
        if (result == null) {
            Log.w(TAG, "Scan cancelled or no result")
            return
        }

        val pages = result.pages
        if (pages.isNullOrEmpty()) {
            Log.w(TAG, "No pages scanned")
            return
        }

        val imageUris = pages.mapNotNull { it.imageUri }

        Log.d(TAG, "Scan complete: ${imageUris.size} images")

        onScanComplete(imageUris, null)
    }
}
package com.safecheck.android.data.extract

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX analyzer that decodes QR codes on-device via ML Kit (design.md §6, R-3.3).
 * The decoded payload is returned to the caller; it is NEVER opened automatically — the
 * app only submits it to the shared API for analysis.
 */
class QrAnalyzer(
    private val onDecoded: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var handled = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || handled) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.mapNotNull { it.rawValue }.firstOrNull { it.isNotBlank() }
                if (!value.isNullOrBlank() && !handled) {
                    handled = true
                    onDecoded(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}

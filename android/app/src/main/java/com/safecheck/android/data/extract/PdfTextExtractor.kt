package com.safecheck.android.data.extract

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Extracts text from a selected PDF (design.md §6, R-3.6). For the MVP this uses OCR over the
 * rendered pages (via ML Kit), which works for both text and scanned/image PDFs without adding
 * a heavy PDF parsing dependency. If too little text is recovered, the caller falls back to a
 * bundled sample document so the journey still demos (AC-3.6.1).
 *
 * Rendering uses Android's built-in PdfRenderer; no third-party PDF library is required.
 */
class PdfTextExtractor(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Minimum characters before we consider extraction "good enough" (else fall back). */
    private val minUsefulChars = 40

    /**
     * @return extracted text, or null if extraction failed / produced too little text.
     * The caller uses the bundled sample when this is null.
     */
    suspend fun extract(uri: Uri, maxPages: Int = 3): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val pageBitmaps = renderPages(uri, maxPages) ?: return@withContext null
        val builder = StringBuilder()
        for (bmp in pageBitmaps) {
            builder.append(recognizeText(bmp)).append('\n')
        }
        val text = builder.toString().trim()
        if (text.length >= minUsefulChars) text else null
    }

    private fun renderPages(uri: Uri, maxPages: Int): List<android.graphics.Bitmap>? {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            pfd.use { descriptor ->
                android.graphics.pdf.PdfRenderer(descriptor).use { renderer ->
                    val count = minOf(renderer.pageCount, maxPages)
                    (0 until count).map { i ->
                        renderer.openPage(i).use { page ->
                            val bmp = android.graphics.Bitmap.createBitmap(
                                page.width.coerceAtLeast(1),
                                page.height.coerceAtLeast(1),
                                android.graphics.Bitmap.Config.ARGB_8888,
                            )
                            // White background so OCR sees dark text on light.
                            android.graphics.Canvas(bmp).drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun recognizeText(bitmap: android.graphics.Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener { cont.resume("") }
        }
}

package com.safecheck.android.domain.usecase

import android.content.Context
import android.net.Uri
import com.safecheck.android.data.api.SafeCheckApi
import com.safecheck.android.data.api.dto.DocumentRequest
import com.safecheck.android.data.api.toRiskResult
import com.safecheck.android.data.extract.PdfTextExtractor
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.domain.model.DocumentAnalysis
import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.domain.redaction.RedactionEngine

/**
 * Document/PDF journey (design.md §6, R-3.6):
 *   pick PDF -> extract text (OCR over rendered pages) -> [fallback to bundled sample if
 *   extraction is too sparse] -> on-device redaction -> shared /v1/document -> DocumentAnalysis.
 */
class SubmitDocumentUseCase(
    private val appContext: Context,
    private val pdfTextExtractor: PdfTextExtractor,
    private val redactionEngine: RedactionEngine,
    private val api: SafeCheckApi,
    private val caseStore: CaseStore,
) {
    suspend operator fun invoke(uri: Uri?): DocumentAnalysis {
        var usedFallback = false
        val extracted = uri?.let { runCatching { pdfTextExtractor.extract(it) }.getOrNull() }
        val rawText = if (!extracted.isNullOrBlank()) {
            extracted
        } else {
            usedFallback = true
            loadSampleDocument()
        }

        val redaction = redactionEngine.redact(rawText)
        val response = api.document(
            DocumentRequest(
                content = redaction.maskedText,
                sourceType = "manual",
                redactionHits = redaction.hitTypeNames,
            )
        )

        val result = response.toRiskResult()
        val case = SafetyCase(
            caseId = response.caseId,
            inputType = "document",
            sourceType = "manual",
            timestamp = System.currentTimeMillis(),
            title = if (usedFallback) "Sample document" else "Document check",
            result = result,
        )
        caseStore.save(case)

        return DocumentAnalysis(
            caseId = response.caseId,
            result = result,
            keyInformation = response.keyInformation,
            deadlines = response.deadlines,
            requiredActions = response.requiredActions,
            usedSampleFallback = usedFallback,
        )
    }

    private fun loadSampleDocument(): String =
        runCatching {
            appContext.assets.open("sample_document.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("Sample document unavailable.")
}

package com.safecheck.android.domain.model

/**
 * Document intelligence result (Master Spec §12, R-3.6). Wraps the shared risk result plus
 * the document-specific extractions (key info, deadlines, required actions).
 */
data class DocumentAnalysis(
    val caseId: String,
    val result: RiskResult,
    val keyInformation: List<String>,
    val deadlines: List<String>,
    val requiredActions: List<String>,
    val usedSampleFallback: Boolean,
)

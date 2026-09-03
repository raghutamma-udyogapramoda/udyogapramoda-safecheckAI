package com.safecheck.android.domain.analysis.entity

enum class EntityCategory {
    BANK,
    GOVERNMENT,
    COURIER,
    REMOTE_TOOL,
    CREDENTIAL,
    PAYMENT,
    URGENCY_THREAT,
}

data class ExtractedEntity(
    val category: EntityCategory,
    val label: String,
    val value: String,
    val confidence: Double = 0.9,
    val detectorName: String = "RuleDetector",
)

interface EntityDetector {
    fun detect(text: String): List<ExtractedEntity>
}

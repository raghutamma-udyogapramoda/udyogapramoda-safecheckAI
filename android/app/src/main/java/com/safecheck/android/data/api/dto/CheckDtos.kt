package com.safecheck.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTOs for the shared SafeCheck API (Master Spec §27). Field names use snake_case to match
 * the shared contract so the same payloads render identically on Web and Android.
 *
 * IMPORTANT: `content` is already REDACTED on-device before it is placed here (R-8.1).
 * `redaction_hits` carries hit TYPES only — never raw secret values (Master Spec §15.4).
 */
@Serializable
data class CheckRequest(
    @SerialName("input_type") val inputType: String, // sms|text|screenshot|url|qr|document|email
    val content: String,                              // redacted content
    @SerialName("source_type") val sourceType: String = "manual",
    @SerialName("redaction_hits") val redactionHits: List<String> = emptyList(),
    @SerialName("sender") val sender: String? = null,
) {
    val text: String get() = content
}

@Serializable
data class CheckResponse(
    @SerialName("case_id") val caseId: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_level") val riskLevel: String,  // LOW|MEDIUM|HIGH
    val evidence: List<EvidenceDto> = emptyList(),
    @SerialName("sub_scores") val subScores: SubScoresDto,
    val explanation: String,
    @SerialName("recommended_actions") val recommendedActions: List<String> = emptyList(),
    @SerialName("unavailable_signals") val unavailableSignals: List<String> = emptyList(),
    @SerialName("model_versions") val modelVersions: ModelVersionsDto = ModelVersionsDto(),
)

@Serializable
data class EvidenceDto(
    @SerialName("evidence_id") val evidenceId: String,
    @SerialName("sub_engine") val subEngine: String, // ml|url|rules
    val label: String,
    val points: Int,
    @SerialName("observed_value") val observedValue: String? = null,
    val confidence: Double? = null,
    @SerialName("correlation_group") val correlationGroup: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("severity") val severity: String? = null,
)

@Serializable
data class SubScoresDto(
    @SerialName("ml_pts") val mlPts: Int,
    @SerialName("url_pts") val urlPts: Int,
    @SerialName("rule_pts") val rulePts: Int,
)

@Serializable
data class ModelVersionsDto(
    @SerialName("rule_version") val ruleVersion: String = "unknown",
    @SerialName("model_version") val modelVersion: String = "unknown",
    @SerialName("prompt_version") val promptVersion: String = "unknown",
)

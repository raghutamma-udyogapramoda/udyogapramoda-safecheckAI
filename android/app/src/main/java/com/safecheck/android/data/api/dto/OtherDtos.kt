package com.safecheck.android.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Document (Master Spec §27 POST /v1/document) ---

@Serializable
data class DocumentRequest(
    val content: String,                              // redacted extracted text
    @SerialName("source_type") val sourceType: String = "manual",
    @SerialName("redaction_hits") val redactionHits: List<String> = emptyList(),
)

@Serializable
data class DocumentResponse(
    @SerialName("case_id") val caseId: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_level") val riskLevel: String,
    @SerialName("sub_scores") val subScores: SubScoresDto,
    val evidence: List<EvidenceDto> = emptyList(),
    val explanation: String,
    @SerialName("key_information") val keyInformation: List<String> = emptyList(),
    val deadlines: List<String> = emptyList(),
    @SerialName("required_actions") val requiredActions: List<String> = emptyList(),
    @SerialName("recommended_actions") val recommendedActions: List<String> = emptyList(),
    @SerialName("unavailable_signals") val unavailableSignals: List<String> = emptyList(),
)

// --- Safety Circle (Master Spec §27) ---

@Serializable
data class ShareRequest(
    @SerialName("case_id") val caseId: String,
    @SerialName("contact_id") val contactId: String,
    @SerialName("sanitized_summary") val sanitizedSummary: String, // never raw secrets/logs
)

@Serializable
data class ShareResponse(
    @SerialName("review_link") val reviewLink: String,
    @SerialName("expires_in_minutes") val expiresInMinutes: Int,
)

@Serializable
data class ReviewRequest(
    @SerialName("case_id") val caseId: String,
    @SerialName("reviewer_id") val reviewerId: String,
    val decision: String, // LOOKS_SAFE|LOOKS_SUSPICIOUS|UNSURE
    val note: String? = null,
)

@Serializable
data class ReviewResponse(
    @SerialName("case_id") val caseId: String,
    val decision: String,
    val note: String? = null,
)

// --- Recovery (Master Spec §27) ---

@Serializable
data class IncidentRequest(
    @SerialName("case_id") val caseId: String,
    @SerialName("incident_state") val incidentState: String,
    @SerialName("recovery_actions") val recoveryActions: List<String> = emptyList(),
    val outcome: String? = null,
)

@Serializable
data class IncidentResponse(
    @SerialName("case_id") val caseId: String,
    @SerialName("incident_state") val incidentState: String,
    val stored: Boolean,
)

// --- History (Master Spec §27 GET /v1/history, P1) ---

@Serializable
data class SafetyCaseSummary(
    @SerialName("case_id") val caseId: String,
    @SerialName("input_type") val inputType: String,
    @SerialName("risk_level") val riskLevel: String,
    val score: Int,
    val title: String,
    val timestamp: Long,
)

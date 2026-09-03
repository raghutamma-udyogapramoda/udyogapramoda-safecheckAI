package com.safecheck.android.domain.model

/**
 * A Recovery incident record (Master Spec §22, §28). Built by the Recovery wizard under a
 * strict zero OTP/PIN/password storage rule (requirements R-9.1.4). Never stores secrets.
 */
data class Incident(
    val caseId: String,
    val state: IncidentState,
    val completedActions: List<String>,
    val outcome: String? = null,
    val createdAt: Long,
)

enum class IncidentState(val display: String) {
    STOP("STOP"),
    SECURE("SECURE"),
    REPORT("REPORT"),
    DOCUMENT("DOCUMENT"),
    LEARN("LEARN/PREVENT"),
    COMPLETE("COMPLETE"),
}

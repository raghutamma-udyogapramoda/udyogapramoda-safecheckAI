package com.safecheck.android.domain.model

/**
 * A Safety Circle advisory decision (Master Spec §21, §28). Advisory only — it is shown
 * beside the machine verdict but never changes the immutable score (requirements R-7.1).
 */
data class Review(
    val caseId: String,
    val reviewerId: String,
    val reviewerName: String,
    val decision: ReviewDecision,
    val timestamp: Long,
    val note: String? = null,
)

enum class ReviewDecision {
    LOOKS_SAFE,
    LOOKS_SUSPICIOUS,
    UNSURE,
    NO_RESPONSE, // default when a contact does not respond (R-7.1.4)
}

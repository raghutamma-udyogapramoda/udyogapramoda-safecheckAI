package com.safecheck.android.domain.model

import com.safecheck.android.ui.theme.RiskBand

/**
 * The core unit of analysis for every input, from either client (Master Spec §28).
 * On Android this is built from the shared API response and rendered as the Risk Result.
 *
 * Privacy: this object stores only sanitized evidence and metadata. Raw captured content
 * is NOT persisted here (requirements R-8.2). [sourceType] carries the true origin,
 * including "sms_real" vs "sms_demo" for the single SMS pipeline (requirements R-5.2).
 */
data class SafetyCase(
    val caseId: String,
    val inputType: String,       // sms | text | screenshot | url | qr | document | email
    val sourceType: String,      // e.g. manual, sms_real, sms_demo, share
    val timestamp: Long,
    val title: String,           // short, sanitized label for lists/notifications
    val result: RiskResult,
    val review: Review? = null,      // Safety Circle advisory (Phase 6)
    val incident: Incident? = null,  // Recovery (Phase 7)
) {
    val band: RiskBand get() = result.band
    val score: Int get() = result.score
}

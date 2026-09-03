package com.safecheck.android.domain.usecase

import com.safecheck.android.data.api.SafeCheckApi
import com.safecheck.android.data.api.dto.ShareRequest
import com.safecheck.android.domain.model.SafetyCase

/**
 * Builds a SANITIZED case summary and shares it with a trusted contact (R-7.1.2).
 * The summary contains only the band, score, and evidence LABELS — never raw content,
 * OTPs, PINs, passwords, or personal logs (Master Spec §21).
 */
class ShareToSafetyCircleUseCase(
    private val api: SafeCheckApi,
) {
    data class ShareOutcome(val reviewLink: String, val expiresInMinutes: Int, val sanitizedSummary: String)

    suspend operator fun invoke(case: SafetyCase, contactId: String): ShareOutcome {
        val summary = buildSanitizedSummary(case)
        val response = api.shareToCircle(
            ShareRequest(caseId = case.caseId, contactId = contactId, sanitizedSummary = summary)
        )
        return ShareOutcome(response.reviewLink, response.expiresInMinutes, summary)
    }

    /** Sanitized, human-readable summary safe to send to a trusted contact. */
    fun buildSanitizedSummary(case: SafetyCase): String = buildString {
        append(case.result.band.label).append(" RISK (").append(case.result.score).append("/100)\n")
        append("Type: ").append(case.inputType).append('\n')
        append("Why: ")
        append(case.result.evidence.joinToString(", ") { it.label })
        // Intentionally excludes raw content and any observed secret values.
    }
}

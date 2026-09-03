package com.safecheck.android.domain.usecase

import com.safecheck.android.data.api.SafeCheckApi
import com.safecheck.android.data.api.dto.CheckRequest
import com.safecheck.android.data.api.toRiskResult
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.domain.redaction.RedactionEngine

/**
 * The vertical-slice orchestration on the client side (design.md §2, §5):
 *   input  ->  on-device redaction  ->  shared API check  ->  SafetyCase  ->  persist
 *
 * The client attaches only REDACTED content and hit TYPES to the request (R-8.1), and reads
 * the verdict from the response (R-1.2). It never scores locally.
 */
class AnalyzeContentUseCase(
    private val redactionEngine: RedactionEngine,
    private val api: SafeCheckApi,
    private val caseStore: CaseStore,
) {
    /**
     * @param inputType one of: text, url, email, sms, screenshot, qr
     * @param rawContent the raw captured content (redacted here before it leaves the device)
     * @param sourceType provenance, e.g. "manual", "sms_real", "sms_demo", "share"
     * @param title short sanitized label for lists/notifications
     */
    suspend operator fun invoke(
        inputType: String,
        rawContent: String,
        sourceType: String = "manual",
        title: String,
        sender: String? = null,
    ): SafetyCase {
        val redaction = redactionEngine.redact(rawContent)

        val response = api.check(
            CheckRequest(
                inputType = inputType,
                content = redaction.maskedText,
                sourceType = sourceType,
                redactionHits = redaction.hitTypeNames,
                sender = sender,
            )
        )

        val case = SafetyCase(
            caseId = response.caseId,
            inputType = inputType,
            sourceType = sourceType,
            timestamp = System.currentTimeMillis(),
            title = title,
            result = response.toRiskResult(),
        )
        caseStore.save(case)
        return case
    }
}

package com.safecheck.android.sms

import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.domain.usecase.AnalyzeContentUseCase

/**
 * The SINGLE entry point for SMS analysis (design.md §5, requirements R-5.2.3).
 *
 * Both the real [SmsBroadcastReceiver] and the [DemoSmsTrigger] call [ingest]. From here the
 * flow is identical: on-device redaction -> shared API check -> SafetyCase -> notification ->
 * Risk Result. The only difference is the internal [SmsSource] recorded on the case as
 * source_type ("sms_real" vs "sms_demo"). There is no separate/fake result path.
 */
class SmsIngestion(
    private val analyze: AnalyzeContentUseCase,
    private val onCaseReady: suspend (SafetyCase) -> Unit,
) {
    /**
     * @param sender masked/short sender label for the case title (never the raw body)
     * @param body the raw SMS text (redacted inside [analyze] before it leaves the device)
     * @param source true origin (REAL or DEMO)
     */
    suspend fun ingest(sender: String, body: String, source: SmsSource): SafetyCase {
        val case = analyze(
            inputType = "sms",
            rawContent = body,
            sourceType = source.sourceType,
            title = "SMS from $sender",
            sender = sender,
        )
        onCaseReady(case)
        return case
    }
}

package com.safecheck.android.ui.manual

/** The manual input types supported on Android (requirements R-3.1..R-3.6). */
enum class ManualInputType(val label: String, val apiType: String) {
    TEXT("Text", "text"),
    URL("URL", "url"),
    QR("QR", "qr"),
    SCREENSHOT("Screenshot", "screenshot"),
    EMAIL("Email", "email"),
    DOCUMENT("Document", "document"),
}

/** Analysis lifecycle for the Manual Check screen. */
sealed interface AnalysisPhase {
    data object Idle : AnalysisPhase
    data object Analyzing : AnalysisPhase
    data class Error(val message: String) : AnalysisPhase
    data class Done(val caseId: String) : AnalysisPhase
}

data class ManualCheckUiState(
    val selected: ManualInputType = ManualInputType.TEXT,
    val text: String = "",
    val url: String = "",
    val emailSender: String = "",
    val emailBody: String = "",
    val phase: AnalysisPhase = AnalysisPhase.Idle,
) {
    val canSubmit: Boolean
        get() = when (selected) {
            ManualInputType.TEXT -> text.isNotBlank()
            ManualInputType.URL -> url.isNotBlank()
            ManualInputType.EMAIL -> emailBody.isNotBlank()
            // QR/Screenshot/Document are captured via camera/picker in later phases.
            else -> false
        }
}

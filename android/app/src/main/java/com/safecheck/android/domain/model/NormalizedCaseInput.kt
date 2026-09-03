package com.safecheck.android.domain.model

/**
 * The canonical, unified input model for SafeCheck analysis (P1/P2 architecture).
 * Every modality (SMS, Text, URL, QR, Screenshot, PDF/Document, Email) is normalized into
 * this structure after on-device PII redaction.
 */
data class NormalizedCaseInput(
    val modality: InputModality,
    val rawContent: String,
    val sanitizedContent: String,
    val sourceType: String = "manual",
    val title: String,
    val sender: String? = null,
    val senderContext: SenderContext? = null,
    val urls: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)

enum class InputModality(val apiType: String) {
    TEXT("text"),
    URL("url"),
    QR("qr"),
    SCREENSHOT("screenshot"),
    DOCUMENT("document"),
    SMS("sms"),
    EMAIL("email");

    companion object {
        fun fromApi(type: String): InputModality = entries.firstOrNull {
            it.apiType.equals(type, ignoreCase = true)
        } ?: TEXT
    }
}

data class SenderContext(
    val senderId: String,
    val isKnownContact: Boolean = false,
    val contactName: String? = null,
    val isCommercialDltHeader: Boolean = false,
    val isForeignNumber: Boolean = false,
    val countryCode: String? = null,
)

package com.safecheck.android.domain.redaction

/**
 * A record that a sensitive value was found and masked. Stores the TYPE ONLY — never the
 * raw value — so it is safe to attach to evidence, audit logs, and API requests
 * (requirements R-8.1, Master Spec §15.4).
 */
data class RedactionHit(val type: PiiType)

enum class PiiType {
    OTP,
    CARD_NUMBER,
    BANK_ACCOUNT,
    IFSC,
    UPI_ID,
    PAN,
    AADHAAR,
    PASSWORD,
    PIN,
}

/** Result of redaction: the masked text plus the list of hit types (no raw values). */
data class RedactionResult(
    val maskedText: String,
    val hits: List<RedactionHit>,
) {
    val hitTypeNames: List<String> get() = hits.map { it.type.name }
}

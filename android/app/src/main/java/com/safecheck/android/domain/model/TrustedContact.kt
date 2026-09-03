package com.safecheck.android.domain.model

import kotlinx.serialization.Serializable

/**
 * A Safety Circle trusted contact (Master Spec §28). For the hackathon these are local/
 * mock contacts (requirements R-7.1.1). Must be explicitly added before use.
 */
@Serializable
data class TrustedContact(
    val contactId: String,
    val name: String,
    val relationship: String,
    val verifiedChannel: String, // e.g. "sms:+91•••", "whatsapp" — display/sanitized only
    val isPrimary: Boolean = false,
    val phoneNumber: String = "",
)

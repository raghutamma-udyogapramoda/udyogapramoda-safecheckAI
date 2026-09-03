package com.safecheck.android.domain.model

/**
 * A privacy-conscious governance log entry (Master Spec §15.8, §28). Records only
 * governance facts — consent changes, monitoring toggles, analysis event ids, fallback
 * events. Must NEVER contain raw OTPs, passwords, PINs, or message/document content.
 */
data class AuditLogEntry(
    val actor: String,          // "user" | "system"
    val actionType: String,     // e.g. "consent_change", "sms_channel_enabled", "analysis"
    val detail: String,         // governance-only description (no secrets/content)
    val timestamp: Long,
    val policyVersion: String = "mvp-1",
)

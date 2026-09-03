package com.safecheck.android.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.safecheck.android.data.store.db.AuditDao
import com.safecheck.android.data.store.db.AuditEntity
import com.safecheck.android.domain.model.AuditLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.stringPreferencesKey

private val Context.dataStore by preferencesDataStore(name = "safecheck_settings")

/**
 * Persists user consent and per-channel monitoring settings (requirements R-5.1, R-8.2).
 * Nothing is enabled by default. Consent/toggle changes are written to the audit log as
 * governance facts (no content, no secrets).
 */
class SettingsStore(
    private val context: Context,
    private val auditDao: AuditDao,
) {
    private val smsEnabledKey = booleanPreferencesKey("sms_channel_enabled")
    private val largeTextKey = booleanPreferencesKey("large_text_enabled")
    private val languageKey = stringPreferencesKey("app_language")

    val smsChannelEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[smsEnabledKey] ?: false }

    suspend fun isSmsChannelEnabled(): Boolean =
        context.dataStore.data.map { it[smsEnabledKey] ?: false }.first()

    val largeTextEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[largeTextKey] ?: false }

    val selectedLanguage: Flow<String> =
        context.dataStore.data.map { it[languageKey] ?: "en" }

    /** Governance audit events for the Privacy screen (no secrets/content). */
    val auditEntries: Flow<List<AuditLogEntry>> =
        auditDao.observeRecent(50).map { list ->
            list.map { AuditLogEntry(it.actor, it.actionType, it.detail, it.timestamp, it.policyVersion) }
        }

    suspend fun setSmsChannelEnabled(enabled: Boolean) {
        context.dataStore.edit { it[smsEnabledKey] = enabled }
        audit(
            actionType = if (enabled) "sms_channel_enabled" else "sms_channel_disabled",
            detail = "User ${if (enabled) "enabled" else "disabled"} automatic SMS detection",
        )
    }

    suspend fun setLargeTextEnabled(enabled: Boolean) {
        context.dataStore.edit { it[largeTextKey] = enabled }
    }

    suspend fun setSelectedLanguage(lang: String) {
        context.dataStore.edit { it[languageKey] = lang }
        audit(
            actionType = "language_changed",
            detail = "User selected language: $lang",
        )
    }

    private suspend fun audit(actionType: String, detail: String) {
        auditDao.insert(
            AuditEntity(
                actor = "user",
                actionType = actionType,
                detail = detail,
                timestamp = System.currentTimeMillis(),
                policyVersion = "mvp-1",
            )
        )
    }
}

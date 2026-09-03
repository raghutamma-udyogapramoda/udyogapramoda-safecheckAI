package com.safecheck.android.data.store.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Governance audit entry (Master Spec §15.8). Governance facts only — never raw OTPs,
 * passwords, PINs, or message/document content (requirements R-8.2.3).
 */
@Entity(tableName = "audit")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actor: String,
    val actionType: String,
    val detail: String,
    val timestamp: Long,
    val policyVersion: String,
)

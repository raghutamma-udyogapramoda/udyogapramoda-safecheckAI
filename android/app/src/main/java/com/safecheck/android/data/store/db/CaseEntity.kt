package com.safecheck.android.data.store.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted sanitized case. Stores the risk RESULT (already sanitized, arithmetic evidence)
 * as JSON — never raw captured content, OTPs, PINs, or passwords (requirements R-8.2).
 */
@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey val caseId: String,
    val inputType: String,
    val sourceType: String,
    val timestamp: Long,
    val title: String,
    val band: String,
    val score: Int,
    val resultJson: String,
)

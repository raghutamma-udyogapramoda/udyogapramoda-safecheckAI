package com.safecheck.android.data.store

import com.safecheck.android.domain.model.SafetyCase
import kotlinx.coroutines.flow.Flow

/**
 * Local store of sanitized cases (requirements R-8.2 minimal retention). Only the
 * sanitized [SafetyCase] result is persisted; raw captured content is never stored here.
 * A Room-backed implementation lands in T3.7; the interface keeps callers decoupled.
 */
interface CaseStore {
    suspend fun save(case: SafetyCase)
    suspend fun get(caseId: String): SafetyCase?
    fun observeRecent(limit: Int = 20): Flow<List<SafetyCase>>
    suspend fun clearAll()
    suspend fun delete(caseId: String)
}

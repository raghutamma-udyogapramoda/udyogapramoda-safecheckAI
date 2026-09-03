package com.safecheck.android.data.store

import com.safecheck.android.data.api.dto.CheckResponse
import com.safecheck.android.data.store.db.CaseDao
import com.safecheck.android.data.store.db.CaseEntity
import com.safecheck.android.domain.model.SafetyCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Room-backed [CaseStore]. Persists only the sanitized result JSON (requirements R-8.2).
 */
class RoomCaseStore(
    private val dao: CaseDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : CaseStore {

    override suspend fun save(case: SafetyCase) {
        val dto = case.result.toStorageDto(case.caseId)
        dao.insert(
            CaseEntity(
                caseId = case.caseId,
                inputType = case.inputType,
                sourceType = case.sourceType,
                timestamp = case.timestamp,
                title = case.title,
                band = case.result.band.label,
                score = case.result.score,
                resultJson = json.encodeToString(CheckResponse.serializer(), dto),
            )
        )
    }

    override suspend fun get(caseId: String): SafetyCase? {
        val e = dao.get(caseId) ?: return null
        return e.toSafetyCase()
    }

    override fun observeRecent(limit: Int): Flow<List<SafetyCase>> =
        dao.observeRecent(limit).map { list -> list.map { it.toSafetyCase() } }

    override suspend fun clearAll() {
        dao.deleteAll()
    }

    override suspend fun delete(caseId: String) {
        dao.delete(caseId)
    }

    private fun CaseEntity.toSafetyCase(): SafetyCase {
        val dto = json.decodeFromString(CheckResponse.serializer(), resultJson)
        return SafetyCase(
            caseId = caseId,
            inputType = inputType,
            sourceType = sourceType,
            timestamp = timestamp,
            title = title,
            result = dto.toStorageRiskResult(),
        )
    }
}

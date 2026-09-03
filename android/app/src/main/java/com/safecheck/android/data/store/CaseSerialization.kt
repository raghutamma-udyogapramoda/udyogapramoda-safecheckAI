package com.safecheck.android.data.store

import com.safecheck.android.data.api.dto.CheckResponse
import com.safecheck.android.data.api.dto.EvidenceDto
import com.safecheck.android.data.api.dto.ModelVersionsDto
import com.safecheck.android.data.api.dto.SubScoresDto
import com.safecheck.android.data.api.toRiskResult
import com.safecheck.android.domain.model.RiskResult

/**
 * Serialization bridge for persistence. The already-@Serializable [CheckResponse] DTO is
 * reused as the on-disk shape for a [RiskResult] so we don't annotate domain models.
 * Only sanitized result data is serialized — never raw content (requirements R-8.2).
 */

fun RiskResult.toStorageDto(caseId: String): CheckResponse = CheckResponse(
    caseId = caseId,
    riskScore = score,
    riskLevel = band.label,
    evidence = evidence.map {
        EvidenceDto(
            evidenceId = it.evidenceId,
            subEngine = it.subEngine.display,
            label = it.label,
            points = it.points,
            observedValue = it.observedValue,
            confidence = it.confidence,
        )
    },
    subScores = SubScoresDto(subScores.mlPts, subScores.urlPts, subScores.rulePts),
    explanation = explanation,
    recommendedActions = recommendedActions,
    unavailableSignals = unavailableSignals,
    modelVersions = ModelVersionsDto(
        modelVersions.ruleVersion,
        modelVersions.modelVersion,
        modelVersions.promptVersion,
    ),
)

fun CheckResponse.toStorageRiskResult(): RiskResult = toRiskResult()

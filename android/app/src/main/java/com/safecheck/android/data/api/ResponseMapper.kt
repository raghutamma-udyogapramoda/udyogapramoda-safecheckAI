package com.safecheck.android.data.api

import com.safecheck.android.data.api.dto.CheckResponse
import com.safecheck.android.data.api.dto.DocumentResponse
import com.safecheck.android.data.api.dto.EvidenceDto
import com.safecheck.android.data.api.dto.ModelVersionsDto
import com.safecheck.android.data.api.dto.SubScoresDto
import com.safecheck.android.domain.model.Evidence
import com.safecheck.android.domain.model.ModelVersions
import com.safecheck.android.domain.model.RiskResult
import com.safecheck.android.domain.model.SubEngine
import com.safecheck.android.domain.model.SubScores
import com.safecheck.android.ui.theme.RiskBand

/**
 * Maps shared-API DTOs into the client domain model. Pure translation — no scoring logic.
 * The band comes from the API's risk_level; the score comes from the API's risk_score.
 * The client never derives these itself (requirements R-1.2).
 */

fun CheckResponse.toRiskResult(): RiskResult = RiskResult(
    score = riskScore,
    band = RiskBand.fromApi(riskLevel),
    subScores = subScores.toDomain(),
    evidence = evidence.map { it.toDomain() },
    explanation = explanation,
    recommendedActions = recommendedActions,
    unavailableSignals = unavailableSignals,
    modelVersions = modelVersions.toDomain(),
)

fun DocumentResponse.toRiskResult(): RiskResult = RiskResult(
    score = riskScore,
    band = RiskBand.fromApi(riskLevel),
    subScores = subScores.toDomain(),
    evidence = evidence.map { it.toDomain() },
    explanation = explanation,
    recommendedActions = recommendedActions,
    unavailableSignals = unavailableSignals,
)

private fun SubScoresDto.toDomain() = SubScores(mlPts = mlPts, urlPts = urlPts, rulePts = rulePts)

private fun EvidenceDto.toDomain() = Evidence(
    evidenceId = evidenceId,
    subEngine = SubEngine.fromApi(subEngine),
    label = label,
    points = points,
    observedValue = observedValue,
    confidence = confidence,
    correlationGroup = correlationGroup,
    source = source,
    severity = severity,
)

private fun ModelVersionsDto.toDomain() = ModelVersions(ruleVersion, modelVersion, promptVersion)

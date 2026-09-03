package com.safecheck.android.domain.model

import com.safecheck.android.ui.theme.RiskBand

/**
 * The structured result rendered by the Risk Result screen (Master Spec §19).
 * All fields originate from the shared brain via the API. The client never computes
 * [score] or [band] (requirements R-1.2, R-6.1).
 *
 * [unavailableSignals] lists external checks that could not be completed (e.g. VirusTotal
 * down). SafeCheck shows these honestly and never fabricates a reputation result
 * (Master Spec §15.7, requirements R-10.2).
 */
data class RiskResult(
    val score: Int,
    val band: RiskBand,
    val subScores: SubScores,
    val evidence: List<Evidence>,
    val explanation: String,
    val recommendedActions: List<String>,
    val unavailableSignals: List<String> = emptyList(),
    val modelVersions: ModelVersions = ModelVersions(),
)

/** Versioning for prompts/models/rules so any verdict is explainable after the fact (§28). */
data class ModelVersions(
    val ruleVersion: String = "unknown",
    val modelVersion: String = "unknown",
    val promptVersion: String = "unknown",
)

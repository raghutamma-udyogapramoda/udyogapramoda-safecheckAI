package com.safecheck.android.domain.model

/**
 * The three independent sub-engine outputs summed by the deterministic risk engine:
 * Score = min(100, ML_pts + URL_pts + Rule_pts) (Master Spec §17).
 *
 * These are received from the shared brain and displayed for transparency. The client
 * does NOT compute the final score — [sum] is only used to verify the values it was given
 * add up, never to override the authoritative score in [RiskResult.score].
 */
data class SubScores(
    val mlPts: Int,
    val urlPts: Int,
    val rulePts: Int,
) {
    /** Raw sum of sub-engine points (pre-cap). Used only for display/verification. */
    val sum: Int get() = mlPts + urlPts + rulePts
}

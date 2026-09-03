package com.safecheck.android.domain.model

/**
 * One itemized evidence signal justifying the score (Master Spec §19, §28).
 * Produced by the shared brain; the client only renders it. `points` and `subEngine`
 * are shown so the arithmetic is verifiable (requirements R-6.2).
 */
data class Evidence(
    val evidenceId: String,
    val subEngine: SubEngine,
    val label: String,
    val points: Int,
    val observedValue: String? = null,
    val confidence: Double? = null,
    val correlationGroup: String? = null,
    val source: String? = null,
    val severity: String? = null,
)

enum class SubEngine(val display: String) {
    ML("ml"),
    URL("url"),
    RULES("rules"),
    UNKNOWN("unknown");

    companion object {
        fun fromApi(value: String?): SubEngine = when (value?.lowercase()) {
            "ml" -> ML
            "url" -> URL
            "rules", "rule" -> RULES
            else -> UNKNOWN
        }
    }
}

package com.safecheck.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Risk bands are shared product terminology (LOW / MEDIUM / HIGH / UNCERTAIN) with fixed score
 * ranges from the deterministic engine. This client only *renders* the band it is
 * given by the shared API — it never computes it (design.md §2, requirements R-1.2).
 */
enum class RiskBand(val label: String, val range: String) {
    LOW("LOW", "0–39"),
    MEDIUM("MEDIUM", "40–69"),
    HIGH("HIGH", "70–100"),
    UNCERTAIN("UNCERTAIN", "Needs Verification");

    companion object {
        /** Map an API string to a band; defaults to LOW if unrecognized. */
        fun fromApi(value: String?): RiskBand = when (value?.uppercase()) {
            "HIGH" -> HIGH
            "MEDIUM" -> MEDIUM
            "UNCERTAIN" -> UNCERTAIN
            else -> LOW
        }
    }
}

/** Color tokens for a risk band. Pure presentation; carries no scoring logic. */
data class RiskColors(val accent: Color, val container: Color, val onContainer: Color)

fun RiskBand.colors(): RiskColors = when (this) {
    RiskBand.LOW -> RiskColors(RiskLowGreen, RiskLowContainer, Color(0xFFA8DAB5))
    RiskBand.MEDIUM -> RiskColors(RiskMediumAmber, RiskMediumContainer, Color(0xFFFEEFC3))
    RiskBand.HIGH -> RiskColors(RiskHighRed, RiskHighContainer, Color(0xFFFAD2CF))
    RiskBand.UNCERTAIN -> RiskColors(RiskUncertainPurple, RiskUncertainContainer, Color(0xFFE8D7FC))
}


package com.safecheck.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes. Bottom-nav destinations are the four primary areas; the rest are
 * detail routes reached from them (design.md §8).
 */
object Routes {
    const val HOME = "home"
    const val PROTECTION = "protection"      // Automatic Protection
    const val CIRCLE = "circle"              // Safety Circle
    const val RECOVERY = "recovery"

    const val MANUAL_CHECK = "manual_check"
    const val QR_SCAN = "qr_scan"
    const val DOCUMENT = "document"
    const val RISK_RESULT = "risk_result"    // risk_result/{caseId}
    const val PRIVACY = "privacy"

    fun riskResult(caseId: String) = "risk_result/$caseId"
    fun circleForCase(caseId: String) = "circle?caseId=$caseId"
    fun recoveryForCase(caseId: String) = "recovery?caseId=$caseId"
}

/** Bottom navigation items. */
enum class BottomDest(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Filled.Home),
    PROTECTION(Routes.PROTECTION, "Protection", Icons.Filled.Shield),
    CIRCLE(Routes.CIRCLE, "Circle", Icons.Filled.Group),
    RECOVERY(Routes.RECOVERY, "Recovery", Icons.Filled.HealthAndSafety),
}

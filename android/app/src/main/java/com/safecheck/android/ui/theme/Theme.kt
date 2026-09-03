package com.safecheck.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.isSpecified

private val SafeCheckColorScheme = androidx.compose.material3.darkColorScheme(
    primary = SafeCheckBlue,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF0D1D3A),
    primaryContainer = SafeCheckNavy,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    background = SafeCheckNavyDark,
    onBackground = NeutralText,
    surface = SafeCheckSurface,
    onSurface = NeutralText,
    surfaceVariant = SafeCheckSurfaceElevated,
    onSurfaceVariant = NeutralMuted,
    outline = NeutralOutline,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF232832),
    error = RiskHighRed,
    onError = androidx.compose.ui.graphics.Color(0xFF3C1414),
    errorContainer = RiskHighContainer,
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFAD2CF),
)

/**
 * Global text-scale multiplier for the large-text accessibility option (requirements
 * R-6.5.2). Screens read this and scale key text. Default 1.0f = normal size.
 */
val LocalTextScale = staticCompositionLocalOf { 1.0f }

@Composable
fun SafeCheckTheme(
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTextScale provides textScale) {
        MaterialTheme(
            colorScheme = SafeCheckColorScheme,
            typography = SafeCheckTypography.scaled(textScale),
            content = content
        )
    }
}

/** Scales all font sizes/line heights by [factor] for the large-text option (R-6.5.2). */
private fun androidx.compose.material3.Typography.scaled(factor: Float): androidx.compose.material3.Typography {
    if (factor == 1.0f) return this
    fun androidx.compose.ui.text.TextStyle.s() = copy(
        fontSize = fontSize.times(factor),
        lineHeight = if (lineHeight.isSpecified) lineHeight.times(factor) else lineHeight,
    )
    return copy(
        displayLarge = displayLarge.s(), displayMedium = displayMedium.s(), displaySmall = displaySmall.s(),
        headlineLarge = headlineLarge.s(), headlineMedium = headlineMedium.s(), headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(), titleMedium = titleMedium.s(), titleSmall = titleSmall.s(),
        bodyLarge = bodyLarge.s(), bodyMedium = bodyMedium.s(), bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(), labelMedium = labelMedium.s(), labelSmall = labelSmall.s(),
    )
}

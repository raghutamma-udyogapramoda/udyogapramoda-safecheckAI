package com.safecheck.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.safecheck.android.ui.theme.RiskHighContainer
import com.safecheck.android.ui.theme.RiskHighRed
import com.safecheck.android.ui.theme.RiskLowContainer
import com.safecheck.android.ui.theme.RiskMediumAmber
import com.safecheck.android.ui.theme.RiskMediumContainer


enum class BannerKind { INFO, WARNING, ERROR, SUCCESS }

/**
 * Honest status banner for degraded states, explanations, and warnings.
 * Uses semantic colors calibrated for dark-first Google Authenticator theme.
 */
@Composable
fun StatusBanner(
    message: String,
    kind: BannerKind = BannerKind.INFO,
    modifier: Modifier = Modifier,
) {
    val bg = when (kind) {
        BannerKind.INFO -> androidx.compose.ui.graphics.Color(0xFF1B2433)
        BannerKind.WARNING -> RiskMediumContainer
        BannerKind.ERROR -> RiskHighContainer
        BannerKind.SUCCESS -> RiskLowContainer
    }
    val fg = when (kind) {
        BannerKind.INFO -> androidx.compose.ui.graphics.Color(0xFF8AB4F8)
        BannerKind.WARNING -> RiskMediumAmber
        BannerKind.ERROR -> RiskHighRed
        BannerKind.SUCCESS -> androidx.compose.ui.graphics.Color(0xFF81C995)
    }
    val icon: ImageVector = when (kind) {
        BannerKind.INFO -> Icons.Filled.Info
        BannerKind.WARNING -> Icons.Filled.Warning
        BannerKind.ERROR -> Icons.Filled.Warning
        BannerKind.SUCCESS -> Icons.Filled.Info
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg)
        Text(
            text = message,
            color = androidx.compose.ui.graphics.Color(0xFFE8EAED),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

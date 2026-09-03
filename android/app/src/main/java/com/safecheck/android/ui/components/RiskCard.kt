package com.safecheck.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.RiskBand
import com.safecheck.android.ui.theme.SafeCheckTheme
import com.safecheck.android.ui.theme.colors

/**
 * The Risk Card renders the verdict produced by the shared deterministic engine:
 * band + score (X/100) + the "Score Immutable by LLM" guarantee (requirements R-6.1).
 * It displays values only; it never computes them.
 */
@Composable
fun RiskCard(
    band: RiskBand,
    score: Int,
    modifier: Modifier = Modifier,
) {
    val c = band.colors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.container)
            .border(1.dp, c.accent, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${band.label} RISK",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = c.accent,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$score/100",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = c.accent,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Score Immutable by LLM",
            style = MaterialTheme.typography.labelMedium,
            color = NeutralMuted,
        )
    }
}

@Preview
@Composable
private fun RiskCardHighPreview() {
    SafeCheckTheme { RiskCard(band = RiskBand.HIGH, score = 87) }
}

@Preview
@Composable
private fun RiskCardLowPreview() {
    SafeCheckTheme { RiskCard(band = RiskBand.LOW, score = 12) }
}

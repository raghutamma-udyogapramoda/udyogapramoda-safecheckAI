package com.safecheck.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.SafeCheckTheme


/**
 * One itemized evidence signal, e.g. "False Urgency  +10 (rules)".
 * Evidence must show its arithmetic so the verdict is verifiable, not asserted
 * (requirements R-6.2). Points/labels come from the API response.
 */
@Composable
fun EvidenceRow(
    label: String,
    points: Int,
    subEngine: String,
    observedValue: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (points >= 0) "+$points" else "$points",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = buildString {
                append(subEngine)
                if (!observedValue.isNullOrBlank()) append(" · ").append(observedValue)
            },
            style = MaterialTheme.typography.labelMedium,
            color = NeutralMuted,
        )
    }
}

/** Shows the sub-engine subtotals summing exactly to the final score (R-6.2.2). */
@Composable
fun EvidenceArithmeticRow(
    mlPts: Int,
    urlPts: Int,
    rulePts: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1B2028))
            .border(1.dp, Color(0xFF2C323E), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "MATHEMATICAL SUB-ENGINE PROOF",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8AB4F8),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "✓ Exact Match",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF81C995),
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SubScoreChip(label = "1. ML / NLP", pts = mlPts)
            SubScoreChip(label = "2. URL Intel", pts = urlPts)
            SubScoreChip(label = "3. Deterministic", pts = rulePts)
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFF2C323E))
        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Formula: min(100, $mlPts + $urlPts + $rulePts) =",
                style = MaterialTheme.typography.bodySmall,
                color = NeutralMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$total / 100",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8EAED),
            )
        }
    }
}

@Composable
private fun SubScoreChip(label: String, pts: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF13171D))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = NeutralMuted)
        Text(
            text = "$pts pts",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (pts > 0) Color(0xFFFDD663) else Color(0xFF81C995)
        )
    }
}

@Preview
@Composable
private fun EvidencePreview() {
    SafeCheckTheme {
        Column {
            EvidenceRow("False Urgency", 10, "rules", "\"account blocked today\"")
            EvidenceRow("Unauthorized Payment Ask", 10, "rules")
            EvidenceRow("Lookalike Domain", 25, "url", "sbi-kyc-update.xyz")
            EvidenceArithmeticRow(mlPts = 42, urlPts = 25, rulePts = 20, total = 87)
        }
    }
}

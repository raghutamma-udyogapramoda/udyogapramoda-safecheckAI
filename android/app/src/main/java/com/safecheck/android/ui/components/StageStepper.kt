package com.safecheck.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.NeutralOutline
import com.safecheck.android.ui.theme.RiskLowGreen
import com.safecheck.android.ui.theme.SafeCheckNavy
import com.safecheck.android.ui.theme.SafeCheckTheme

/**
 * Vertical stepper used by the Recovery wizard (STOP → SECURE → REPORT → DOCUMENT →
 * LEARN/PREVENT) — requirements R-9.1. Shows completed / current / upcoming stages.
 */
@Composable
fun StageStepper(
    stages: List<String>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        stages.forEachIndexed { index, stage ->
            val done = index < currentIndex
            val current = index == currentIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = when {
                    done -> RiskLowGreen
                    current -> SafeCheckNavy
                    else -> NeutralOutline
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                )
            }
            if (index < stages.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Preview
@Composable
private fun StageStepperPreview() {
    SafeCheckTheme {
        StageStepper(
            stages = listOf("STOP", "SECURE", "REPORT", "DOCUMENT", "LEARN/PREVENT"),
            currentIndex = 2,
        )
    }
}

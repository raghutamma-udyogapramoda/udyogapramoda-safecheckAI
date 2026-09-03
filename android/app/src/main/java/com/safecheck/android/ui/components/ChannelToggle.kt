package com.safecheck.android.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.SafeCheckTheme

/**
 * A per-channel automatic-monitoring toggle with rationale text (requirements R-5.1).
 * Nothing is enabled by default; enabling requires explicit user action + consent.
 */
@Composable
fun ChannelToggle(
    title: String,
    rationale: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralMuted,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Preview
@Composable
private fun ChannelTogglePreview() {
    SafeCheckTheme {
        ChannelToggle(
            title = "SMS automatic detection",
            rationale = "Reads incoming SMS text only, on-device, and redacts secrets before analysis.",
            checked = false,
            onCheckedChange = {},
        )
    }
}

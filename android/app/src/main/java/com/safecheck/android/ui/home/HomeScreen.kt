package com.safecheck.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.RiskBand
import com.safecheck.android.ui.theme.SafeCheckNavy
import com.safecheck.android.ui.theme.SafeCheckTheme
import com.safecheck.android.ui.theme.colors

/**
 * Home / protection status (requirements R-2.1). Shows branding, an overall protection
 * status summary, the primary entry points, and a recent-activity list with empty state.
 * The mode choice (Manual Check vs Automatic Protection) is explicit.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onManualCheck: () -> Unit,
    onAutomaticProtection: () -> Unit,
    onSafetyCircle: () -> Unit,
    onRecovery: () -> Unit,
    onPrivacy: () -> Unit,
    onOpenCase: (String) -> Unit,
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = "SafeCheck",
            style = MaterialTheme.typography.headlineLarge,
            color = SafeCheckNavy,
        )
        Text(
            text = "Your Privacy. Your Safety. Always in Control.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )

        Spacer(Modifier.height(16.dp))
        ProtectionStatusCard(state.protectionSummary)

        Spacer(Modifier.height(20.dp))
        Text("What would you like to do?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        EntryCard("Manual Check", "Check a message, link, QR, screenshot or document", Icons.Filled.Search, onManualCheck)
        Spacer(Modifier.height(10.dp))
        EntryCard("Automatic Protection", "Opt-in SMS detection and alerts, per channel", Icons.Filled.Shield, onAutomaticProtection)
        Spacer(Modifier.height(10.dp))
        EntryCard("Safety Circle", "Ask a trusted contact for a second opinion", Icons.Filled.Group, onSafetyCircle)
        Spacer(Modifier.height(10.dp))
        EntryCard("Recovery", "Already clicked or paid? Get guided help", Icons.Filled.HealthAndSafety, onRecovery)
        Spacer(Modifier.height(10.dp))
        EntryCard("Privacy & Settings", "See what's processed and control monitoring", Icons.Filled.Lock, onPrivacy)

        Spacer(Modifier.height(24.dp))
        var filterBand by remember { mutableStateOf<RiskBand?>(null) }
        var showClearConfirm by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (state.recent.isNotEmpty()) {
                androidx.compose.material3.TextButton(onClick = { showClearConfirm = true }) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (state.recent.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                androidx.compose.material3.FilterChip(
                    selected = filterBand == null,
                    onClick = { filterBand = null },
                    label = { Text("All", style = MaterialTheme.typography.labelSmall) },
                )
                androidx.compose.material3.FilterChip(
                    selected = filterBand == RiskBand.HIGH,
                    onClick = { filterBand = if (filterBand == RiskBand.HIGH) null else RiskBand.HIGH },
                    label = { Text("High", style = MaterialTheme.typography.labelSmall) },
                )
                androidx.compose.material3.FilterChip(
                    selected = filterBand == RiskBand.MEDIUM,
                    onClick = { filterBand = if (filterBand == RiskBand.MEDIUM) null else RiskBand.MEDIUM },
                    label = { Text("Medium", style = MaterialTheme.typography.labelSmall) },
                )
                androidx.compose.material3.FilterChip(
                    selected = filterBand == RiskBand.LOW,
                    onClick = { filterBand = if (filterBand == RiskBand.LOW) null else RiskBand.LOW },
                    label = { Text("Safe", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        val filteredRecent = if (filterBand != null) {
            state.recent.filter { it.band == filterBand }
        } else {
            state.recent
        }

        if (filteredRecent.isEmpty()) {
            Text(
                if (state.recent.isEmpty()) "No checks yet. Your recent results will appear here."
                else "No ${filterBand?.label} risk items in history.",
                style = MaterialTheme.typography.bodyMedium,
                color = NeutralMuted,
            )
        } else {
            filteredRecent.forEach { item ->
                RecentRow(item, onClick = { onOpenCase(item.caseId) })
                Spacer(Modifier.height(8.dp))
            }
        }

        if (showClearConfirm) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear Scan History") },
                text = { Text("Are you sure you want to remove all saved scan results? This action cannot be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ProtectionStatusCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Protection Status", style = MaterialTheme.typography.labelMedium, color = NeutralMuted)
                Text(summary, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EntryCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
            }
        }
    }
}

@Composable
private fun RecentRow(item: RecentCaseItem, onClick: () -> Unit) {
    val c = item.band.colors()
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = c.container),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.accent.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
            }
            Text("${item.band.label} ${item.score}", style = MaterialTheme.typography.titleMedium, color = c.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun HomePreview() {
    SafeCheckTheme {
        HomeScreen(
            state = HomeUiState(
                protectionSummary = "Manual protection active",
                recent = listOf(
                    RecentCaseItem("1", "SMS from +91••••", "Lookalike bank link", RiskBand.HIGH, 87),
                    RecentCaseItem("2", "Pasted message", "Looks legitimate", RiskBand.LOW, 12),
                ),
            ),
            onManualCheck = {}, onAutomaticProtection = {}, onSafetyCircle = {},
            onRecovery = {}, onPrivacy = {}, onOpenCase = {},
        )
    }
}

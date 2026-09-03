package com.safecheck.android.ui.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecheck.android.domain.model.AuditLogEntry
import com.safecheck.android.ui.theme.NeutralMuted

/**
 * Privacy & Settings (R-8.1.5). Explains what is processed and shared, offers the large-text
 * accessibility toggle, and shows the governance audit log (facts only — no secrets/content).
 */
@Composable
fun PrivacyScreen(
    state: PrivacyUiState,
    onSetLargeText: (Boolean) -> Unit,
    onSetLanguage: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Privacy & Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your Privacy. Your Safety. Always in Control.", style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)

        Spacer(Modifier.height(20.dp))
        Text("Language / भाषा", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Select language for risk explanations and advice.", style = MaterialTheme.typography.bodySmall, color = NeutralMuted)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.FilterChip(
                selected = state.language == "en",
                onClick = { onSetLanguage("en") },
                label = { Text("English") },
            )
            androidx.compose.material3.FilterChip(
                selected = state.language == "hi",
                onClick = { onSetLanguage("hi") },
                label = { Text("हिंदी (Hindi)") },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text("What SafeCheck does with your data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Bullet("Sensitive data (OTP, card, account, IFSC, UPI, PAN, Aadhaar, passwords, PINs) is masked on your device before anything is analyzed.")
        Bullet("Only redacted content is sent for analysis. SafeCheck never asks for your OTP, PIN, or password.")
        Bullet("Automatic monitoring is opt-in per channel and off by default. You can turn it off anytime.")
        Bullet("Safety Circle shares a sanitized summary only — never your secrets or full messages.")
        Bullet("Recovery records never store OTPs, PINs, or passwords.")

        Spacer(Modifier.height(20.dp))
        Text("Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Large text", style = MaterialTheme.typography.bodyLarge)
                Text("Increase text size across result screens.", style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
            }
            Switch(checked = state.largeText, onCheckedChange = onSetLargeText)
        }

        Spacer(Modifier.height(20.dp))
        Text("Activity log (governance)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Consent and monitoring changes. No message content or secrets are logged.", style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
        Spacer(Modifier.height(8.dp))
        if (state.audit.isEmpty()) {
            Text("No activity yet.", style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
        } else {
            state.audit.forEach { entry -> AuditRow(entry) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun AuditRow(entry: AuditLogEntry) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(entry.actionType, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Text(entry.detail, style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)
    }
}

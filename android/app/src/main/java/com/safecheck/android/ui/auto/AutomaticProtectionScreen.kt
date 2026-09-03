package com.safecheck.android.ui.auto

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.components.ChannelToggle
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.theme.NeutralMuted

/**
 * Automatic Protection screen (R-5.1). Explicit per-channel opt-in with rationale + consent,
 * a clear privacy explanation, and the Demo Simulation trigger. Nothing is on by default.
 */
@Composable
fun AutomaticProtectionScreen(
    smsEnabled: Boolean,
    demoRunning: Boolean,
    onSetSmsEnabled: (Boolean) -> Unit,
    onFireDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* best-effort; notification is optional, the in-app case still opens */ }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetSmsEnabled(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Automatic Protection", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Let SafeCheck watch selected channels for risky messages. Every channel is opt-in " +
                "and you stay in control.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(16.dp)) {
                ChannelToggle(
                    title = "SMS automatic detection",
                    rationale = "Reads incoming SMS text only, on-device. Secrets (OTP, card, UPI, etc.) " +
                        "are masked before any analysis. No WhatsApp or call audio is ever read.",
                    checked = smsEnabled,
                    onCheckedChange = { wantOn ->
                        if (wantOn) {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECEIVE_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onSetSmsEnabled(true)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    val notifGranted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!notifGranted) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                            }
                        } else {
                            onSetSmsEnabled(false)
                        }
                    },
                )
                HorizontalDivider()
                ChannelToggle(
                    title = "Notification monitoring",
                    rationale = "Coming soon (P1). Will scan UPI/email notification text, opt-in.",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                )
                HorizontalDivider()
                ChannelToggle(
                    title = "Unknown-number call metadata",
                    rationale = "Coming soon (P1). Flags unknown-number calls by number/time only — never audio.",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        StatusBanner(
            "Privacy: SafeCheck reads structured SMS text only, redacts secrets on your device, and " +
                "shares only what's needed to produce a result. You can turn any channel off anytime.",
            kind = BannerKind.INFO,
        )

        Spacer(Modifier.height(24.dp))
        Text("Try it (demo)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Simulate a suspicious incoming SMS. It runs the exact same detection, privacy, " +
                "notification, and result flow as a real SMS.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )
        Spacer(Modifier.height(10.dp))
        if (demoRunning) {
            Row {
                CircularProgressIndicator()
                Text("Simulating incoming SMS…", modifier = Modifier.padding(start = 12.dp))
            }
        } else {
            SafeCheckButton("Simulate suspicious SMS", onClick = onFireDemo)
        }
        Spacer(Modifier.height(24.dp))
    }
}

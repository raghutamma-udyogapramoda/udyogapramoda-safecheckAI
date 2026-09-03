package com.safecheck.android.ui.manual

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.SafeCheckTheme

/**
 * Manual Check input UI (requirements R-3.1, R-3.2, R-3.5). Tabs cover Text/URL/QR/
 * Screenshot/Email/Document. Text/URL/Email accept input and submit here; QR/Screenshot/
 * Document capture is wired in Phases 4–5. Camera/OCR/PDF are not invoked from this screen yet.
 */
@Composable
fun ManualCheckScreen(
    state: ManualCheckUiState,
    onSelect: (ManualInputType) -> Unit,
    onText: (String) -> Unit,
    onUrl: (String) -> Unit,
    onEmailSender: (String) -> Unit,
    onEmailBody: (String) -> Unit,
    onSubmit: () -> Unit,
    onScanQr: () -> Unit = {},
    onPickImage: (Uri) -> Unit = {},
    onOpenDocument: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) onPickImage(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Manual Check", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Paste content or pick an input type. Sensitive data is masked on your device before anything is analyzed.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ManualInputType.entries.forEach { type ->
                FilterChip(
                    selected = state.selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(type.label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        when (state.selected) {
            ManualInputType.TEXT -> OutlinedTextField(
                value = state.text,
                onValueChange = onText,
                label = { Text("Paste suspicious text") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
            ManualInputType.URL -> OutlinedTextField(
                value = state.url,
                onValueChange = onUrl,
                label = { Text("Enter a URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ManualInputType.EMAIL -> Column {
                OutlinedTextField(
                    value = state.emailSender,
                    onValueChange = onEmailSender,
                    label = { Text("Sender (e.g. support@bank.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.emailBody,
                    onValueChange = onEmailBody,
                    label = { Text("Email body") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                )
            }
            ManualInputType.QR -> Column {
                Text(
                    "Scan a QR code with your camera. It is decoded on your device and never opened.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralMuted,
                )
                Spacer(Modifier.height(12.dp))
                SafeCheckButton("Scan QR", onClick = onScanQr)
            }
            ManualInputType.SCREENSHOT -> Column {
                Text(
                    "Pick a screenshot or photo. Text is read on your device and secrets are masked before analysis.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralMuted,
                )
                Spacer(Modifier.height(12.dp))
                SafeCheckButton("Choose image", onClick = { imagePicker.launch("image/*") })
            }
            ManualInputType.DOCUMENT -> Column {
                Text(
                    "Check a PDF for key information, deadlines, actions and risk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralMuted,
                )
                Spacer(Modifier.height(12.dp))
                SafeCheckButton("Open document check", onClick = onOpenDocument)
            }
        }

        Spacer(Modifier.height(16.dp))
        val usesInlineAction = state.selected == ManualInputType.QR || state.selected == ManualInputType.SCREENSHOT
        when (val phase = state.phase) {
            is AnalysisPhase.Analyzing -> Row {
                CircularProgressIndicator()
                Text("Analyzing…", modifier = Modifier.padding(start = 12.dp))
            }
            is AnalysisPhase.Error -> StatusBanner(phase.message, kind = BannerKind.WARNING)
            else -> if (!usesInlineAction) {
                SafeCheckButton(
                    text = "Check for risk",
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ManualCheckPreview() {
    SafeCheckTheme {
        ManualCheckScreen(
            state = ManualCheckUiState(selected = ManualInputType.TEXT, text = "Your account is blocked"),
            onSelect = {}, onText = {}, onUrl = {}, onEmailSender = {}, onEmailBody = {}, onSubmit = {},
        )
    }
}

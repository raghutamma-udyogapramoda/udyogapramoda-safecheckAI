package com.safecheck.android.ui.document

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecheck.android.domain.model.DocumentAnalysis
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.components.EvidenceArithmeticRow
import com.safecheck.android.ui.components.EvidenceRow
import com.safecheck.android.ui.components.RiskCard
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.SafeCheckOutlinedButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.theme.NeutralMuted

/**
 * Document / PDF screen (R-3.6). Pick a PDF or run the bundled sample. Shows key info,
 * deadlines, required actions, simplified explanation, and risk where applicable.
 */
@Composable
fun DocumentScreen(
    state: DocumentState,
    onPickPdf: (Uri) -> Unit,
    onUseSample: () -> Unit,
    onOpenRecovery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) onPickPdf(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Document / PDF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Check a PDF for important information, deadlines, actions, and risk. Text is read on " +
                "your device and secrets are masked before analysis.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )
        Spacer(Modifier.height(16.dp))

        SafeCheckButton("Choose a PDF", onClick = { pdfPicker.launch("application/pdf") })
        Spacer(Modifier.height(10.dp))
        SafeCheckOutlinedButton("Use sample document", onClick = { onUseSample() })

        Spacer(Modifier.height(20.dp))
        when (state) {
            is DocumentState.Idle -> Unit
            is DocumentState.Analyzing -> Row {
                CircularProgressIndicator()
                Text("Reading document…", modifier = Modifier.padding(start = 12.dp))
            }
            is DocumentState.Error -> StatusBanner(state.message, kind = BannerKind.WARNING)
            is DocumentState.Loaded -> DocumentResult(state.analysis, onOpenRecovery)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DocumentResult(
    analysis: DocumentAnalysis,
    onOpenRecovery: (String) -> Unit,
) {
    val result = analysis.result
    Column {
        if (analysis.usedSampleFallback) {
            StatusBanner(
                "Showing the built-in sample document (the selected file could not be read).",
                kind = BannerKind.INFO,
            )
            Spacer(Modifier.height(12.dp))
        }

        RiskCard(band = result.band, score = result.score)

        if (result.unavailableSignals.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StatusBanner(
                "Some checks could not be completed: ${result.unavailableSignals.joinToString(", ")}.",
                kind = BannerKind.WARNING,
            )
        }

        BulletSection("Key information", analysis.keyInformation)
        BulletSection("Deadlines", analysis.deadlines)
        BulletSection("What the document asks you to do", analysis.requiredActions)

        Spacer(Modifier.height(20.dp))
        Text("Evidence", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        result.evidence.forEach { ev ->
            EvidenceRow(ev.label, ev.points, ev.subEngine.display, ev.observedValue)
        }
        EvidenceArithmeticRow(
            mlPts = result.subScores.mlPts,
            urlPts = result.subScores.urlPts,
            rulePts = result.subScores.rulePts,
            total = result.score,
        )

        Spacer(Modifier.height(20.dp))
        Text("Explanation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(result.explanation, style = MaterialTheme.typography.bodyLarge)

        if (result.recommendedActions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("What you should do", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            result.recommendedActions.forEach { action ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(action, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SafeCheckOutlinedButton("Already clicked or paid? Open Recovery", onClick = { onOpenRecovery(analysis.caseId) })
    }
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    items.forEach { item ->
        Text("•  $item", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
    }
}

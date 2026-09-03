package com.safecheck.android.ui.recovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.SafeCheckOutlinedButton
import com.safecheck.android.ui.components.StageStepper
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.theme.NeutralMuted

/**
 * Recovery wizard (R-9). Interactive five-stage flow with checkable actions,
 * one-tap 1930 helpline dialer, cybercrime.gov.in portal launcher, and scenario triage.
 */
@Composable
fun RecoveryScreen(
    state: RecoveryUiState,
    onToggleAction: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = RecoveryContent.stages
    val context = LocalContext.current
    var selectedScenario by remember { mutableStateOf(IncidentScenario.GENERAL) }

    fun dialHelpline(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openPortal(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Emergency Recovery", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Immediate steps to secure your accounts, freeze fraudulent transactions, and document evidence.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )

        Spacer(Modifier.height(16.dp))

        // --- DIRECT HELPLINE FAST-ACTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFFF28B82), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Official Indian Helplines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "If money was transferred, calling within the 'Golden Hour' helps police freeze the beneficiary account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeutralMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { dialHelpline("1930") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dial 1930")
                    }
                    OutlinedButton(
                        onClick = { openPortal("https://cybercrime.gov.in") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Portal")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- SCENARIO SELECTOR ---
        Text("What happened?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IncidentScenario.entries.forEach { scenario ->
                FilterChip(
                    selected = selectedScenario == scenario,
                    onClick = { selectedScenario = scenario },
                    label = { Text(scenario.label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        StatusBanner(
            message = RecoveryContent.guidanceForScenario(selectedScenario),
            kind = if (selectedScenario != IncidentScenario.GENERAL) BannerKind.WARNING else BannerKind.INFO,
        )

        Spacer(Modifier.height(16.dp))

        if (state.completed) {
            CompletionView(onFinish)
            return@Column
        }

        val progress = (state.currentIndex + 1f) / stages.size
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            "Stage ${state.currentIndex + 1} of ${stages.size}: ${stages[state.currentIndex].title}",
            style = MaterialTheme.typography.labelMedium,
            color = NeutralMuted,
            modifier = Modifier.padding(top = 6.dp),
        )

        Spacer(Modifier.height(12.dp))
        StageStepper(stages = stages.map { it.title }, currentIndex = state.currentIndex)

        Spacer(Modifier.height(20.dp))
        val stage = stages[state.currentIndex]
        Text(stage.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stage.intro, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(12.dp))
        val done = state.completedActionsByStage[state.currentIndex].orEmpty()
        stage.actions.forEachIndexed { i, action ->
            ActionRow(
                text = action,
                checked = done.contains(i),
                onToggle = { onToggleAction(i) },
            )
        }

        stage.warning?.let {
            Spacer(Modifier.height(12.dp))
            StatusBanner(it, kind = BannerKind.WARNING)
        }

        Spacer(Modifier.height(20.dp))
        SafeCheckButton(
            text = if (state.currentIndex == stages.lastIndex) "Finish Recovery" else "Next stage",
            onClick = onNext,
        )
        if (state.currentIndex > 0) {
            Spacer(Modifier.height(8.dp))
            SafeCheckOutlinedButton("Back", onClick = onBack)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActionRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (checked) "Done" else "Not done",
            tint = if (checked) MaterialTheme.colorScheme.primary else NeutralMuted,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompletionView(onFinish: () -> Unit) {
    Column {
        StatusBanner(
            "Recovery protocol completed. You have stopped unauthorized contact, initiated bank freezes, " +
                "reported to the cyber helpline (1930), and preserved evidence. Keep your complaint acknowledgement number safe.",
            kind = BannerKind.SUCCESS,
        )
        Spacer(Modifier.height(16.dp))
        SafeCheckButton("Return to Home", onClick = onFinish)
    }
}


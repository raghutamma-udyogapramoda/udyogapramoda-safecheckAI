package com.safecheck.android.ui.result

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.safecheck.android.accessibility.rememberTtsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.safecheck.android.domain.analysis.ExplanationEngine
import com.safecheck.android.ui.LocalAppContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safecheck.android.domain.model.Evidence
import com.safecheck.android.domain.model.ModelVersions
import com.safecheck.android.domain.model.RiskResult
import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.domain.model.SubEngine
import com.safecheck.android.domain.model.SubScores
import com.safecheck.android.ui.components.BannerKind
import com.safecheck.android.ui.components.EvidenceArithmeticRow
import com.safecheck.android.ui.components.EvidenceRow
import com.safecheck.android.ui.components.RiskCard
import com.safecheck.android.ui.components.SafeCheckButton
import com.safecheck.android.ui.components.SafeCheckOutlinedButton
import com.safecheck.android.ui.components.StatusBanner
import com.safecheck.android.ui.theme.NeutralMuted
import com.safecheck.android.ui.theme.RiskBand
import com.safecheck.android.ui.theme.SafeCheckTheme

/**
 * Risk Result screen (requirements R-6.1–R-6.4). Renders the Risk Card, itemized Evidence
 * with sub-engine arithmetic, plain-language Explanation, recommended Safe Actions, and the
 * Safety Circle + Recovery entry points. All values come from the shared brain.
 */
@Composable
fun RiskResultScreen(
    state: RiskResultState,
    onAskSafetyCircle: (String) -> Unit,
    onOpenRecovery: (String) -> Unit,
    onCheckAnother: () -> Unit = {},
    onReturnHome: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (state) {
        is RiskResultState.Loading -> Centered(modifier) { CircularProgressIndicator() }
        is RiskResultState.NotFound -> Centered(modifier) { Text("Case not found.") }
        is RiskResultState.Loaded -> ResultContent(
            case = state.case,
            onAskSafetyCircle = onAskSafetyCircle,
            onOpenRecovery = onOpenRecovery,
            onCheckAnother = onCheckAnother,
            onReturnHome = onReturnHome,
            modifier = modifier,
        )
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ResultContent(
    case: SafetyCase,
    onAskSafetyCircle: (String) -> Unit,
    onOpenRecovery: (String) -> Unit,
    onCheckAnother: () -> Unit,
    onReturnHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = case.result
    val tts = rememberTtsController()
    val container = com.safecheck.android.ui.LocalAppContainer.current
    val currentLang by container.settingsStore.selectedLanguage.collectAsState(initial = "en")
    val isHindi = currentLang == "hi"

    val explanationText = remember(isHindi, case.caseId) {
        if (isHindi) {
            ExplanationEngine.generateFromEvidence(
                result = result,
                language = ExplanationEngine.Language.HINDI,
            ).narrative
        } else {
            result.explanation
        }
    }

    val actionList = remember(isHindi, case.caseId) {
        if (isHindi) {
            ExplanationEngine.generateFromEvidence(
                result = result,
                language = ExplanationEngine.Language.HINDI,
            ).recommendedActions
        } else {
            result.recommendedActions
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            if (isHindi) "जोखिम परिणाम" else "Risk Result",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(case.title, style = MaterialTheme.typography.bodyMedium, color = NeutralMuted)

        Spacer(Modifier.height(16.dp))
        RiskCard(band = result.band, score = result.score)

        TextButton(onClick = {
            tts.speak(
                if (isHindi) {
                    "जोखिम स्तर: ${result.band.label}। स्कोर: ${result.score}। $explanationText"
                } else {
                    "${result.band.label} risk. Score ${result.score} out of 100. ${result.explanation}"
                }
            )
        }) {
            Icon(Icons.Filled.VolumeUp, contentDescription = null)
            Text(if (isHindi) "  इसे बोलकर सुनें" else "  Read this aloud")
        }

        // Honest degraded-signal disclosure (requirements R-10.2).
        if (result.unavailableSignals.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            StatusBanner(
                message = "Some checks could not be completed: ${result.unavailableSignals.joinToString(", ")}. " +
                    "The result uses the signals that were available.",
                kind = BannerKind.WARNING,
            )
        }

        Spacer(Modifier.height(24.dp))
        Section(if (isHindi) "साक्ष्य (Evidence)" else "Evidence")
        Text(
            if (isHindi) "यह स्कोर क्यों मिला — प्रत्येक संकेत और उसके अंक।" else "Why this score — each signal and its points.",
            style = MaterialTheme.typography.bodyMedium,
            color = NeutralMuted,
        )
        result.evidence.forEach { ev ->
            EvidenceRow(
                label = ev.label,
                points = ev.points,
                subEngine = ev.subEngine.display,
                observedValue = ev.observedValue,
            )
        }
        EvidenceArithmeticRow(
            mlPts = result.subScores.mlPts,
            urlPts = result.subScores.urlPts,
            rulePts = result.subScores.rulePts,
            total = result.score,
        )

        Spacer(Modifier.height(24.dp))
        Section(if (isHindi) "सरल विवरण (Explanation)" else "Explanation")
        Text(explanationText, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (isHindi) "यह विवरण आपको परिणाम समझाने के लिए तैयार किया गया है। यह स्कोर को नहीं बदलता — स्कोर हमेशा सुरक्षित गणना से आता है।"
            else "The explanation is written by the assistant to help you understand the result. It does not set the score — the score comes from the deterministic engine.",
            style = MaterialTheme.typography.labelMedium,
            color = NeutralMuted,
            modifier = Modifier.padding(top = 6.dp),
        )

        if (actionList.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Section(if (isHindi) "आपको क्या करना चाहिए" else "What you should do")
            actionList.forEach { action ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(action, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SafeCheckButton(
            text = if (isHindi) "सुरक्षा चक्र से पूछें" else "Ask Safety Circle",
            onClick = { onAskSafetyCircle(case.caseId) },
        )
        Spacer(Modifier.height(10.dp))
        SafeCheckOutlinedButton(
            text = if (isHindi) "क्या पहले ही क्लिक या भुगतान कर चुके हैं? रिकवरी खोलें" else "Already clicked or paid? Open Recovery",
            onClick = { onOpenRecovery(case.caseId) },
        )
        Spacer(Modifier.height(10.dp))
        SafeCheckOutlinedButton(
            text = if (isHindi) "कोई अन्य आइटम जांचें" else "Check Another Item",
            onClick = onCheckAnother,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onReturnHome,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isHindi) "होम पर वापस जाएं" else "Return to Home")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
}

@Preview
@Composable
private fun RiskResultPreview() {
    val result = RiskResult(
        score = 87,
        band = RiskBand.HIGH,
        subScores = SubScores(mlPts = 42, urlPts = 25, rulePts = 20),
        evidence = listOf(
            Evidence("1", SubEngine.RULES, "False Urgency", 10, "\"account blocked today\""),
            Evidence("2", SubEngine.RULES, "Unauthorized Payment Ask", 10),
            Evidence("3", SubEngine.URL, "Lookalike Domain", 25, "sbi-kyc-update.xyz"),
            Evidence("4", SubEngine.ML, "Matches known scam patterns", 42, confidence = 0.84),
        ),
        explanation = "This message creates false urgency, asks for payment, and links to a fake bank domain.",
        recommendedActions = listOf("Do not click the link.", "Do not pay or share OTP/PIN."),
        modelVersions = ModelVersions(),
    )
    val case = SafetyCase("case_1", "sms", "sms_demo", 0L, "SMS from +91••••", result)
    SafeCheckTheme {
        RiskResultScreen(RiskResultState.Loaded(case), onAskSafetyCircle = {}, onOpenRecovery = {})
    }
}

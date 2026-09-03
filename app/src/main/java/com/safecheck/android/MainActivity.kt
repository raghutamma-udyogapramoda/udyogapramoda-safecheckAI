package com.safecheck.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SafeCheckApp() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeCheckApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("safecheck", 0) }
    var selected by remember { mutableStateOf(CheckType.SMS) }
    var input by remember { mutableStateOf("") }
    var backend by remember { mutableStateOf(prefs.getString("backend", "") ?: "") }
    var showSettings by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AnalysisResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedMedia by remember { mutableStateOf<android.net.Uri?>(null) }
    val scope = rememberCoroutineScope()
    val analyzer = remember { ThreatAnalyzer(context) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedMedia = it }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF066B4B), secondary = Color(0xFF406B5A))) {
        Scaffold(topBar = { TopAppBar(title = { Text("SafeCheck AI") }, actions = { TextButton(onClick = { showSettings = true }) { Text("Settings") } }) }) { padding ->
            Column(Modifier.padding(padding).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Check a suspicious message, link, image, or video", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TypePicker(selected) { selected = it; result = null; selectedMedia = null }
                if (selected == CheckType.IMAGE || selected == CheckType.VIDEO) {
                    OutlinedButton(onClick = { picker.launch(if (selected == CheckType.IMAGE) "image/*" else "video/*") }) { Text(if (selectedMedia == null) "Choose ${selected.label.lowercase()}" else "Change ${selected.label.lowercase()}") }
                    Text("Media is uploaded only after you tap Analyze.", style = MaterialTheme.typography.bodySmall)
                } else {
                    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("Paste ${selected.label.lowercase()} here") })
                }
                Button(enabled = !busy && ((selectedMedia != null) || input.isNotBlank()), modifier = Modifier.fillMaxWidth(), onClick = {
                    busy = true; result = null
                    scope.launch {
                        result = try {
                            if (selectedMedia != null && (selected == CheckType.IMAGE || selected == CheckType.VIDEO)) analyzer.analyzeMedia(selectedMedia!!, selected, backend)
                            else analyzer.analyzeText(selected, input, backend)
                        } catch (e: Exception) { AnalysisResult("inconclusive", "Analysis failed: ${e.message ?: "unknown error"}") }
                        busy = false
                    }
                }) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Analyze safely") }
                result?.let { ResultCard(it) }
                Text("SafeCheck never opens submitted links. A clean reputation lookup is not a guarantee of safety.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, title = { Text("Analysis backend") }, text = { Column { Text("Optional HTTPS endpoint for message classification") } }, confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Close") } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypePicker(selected: CheckType, onSelect: (CheckType) -> Unit) {
    val chunks = listOf(CheckType.entries.take(4), CheckType.entries.drop(4))
    chunks.forEach { types ->
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            types.forEachIndexed { index, type -> SegmentedButton(selected = selected == type, onClick = { onSelect(type) }, shape = SegmentedButtonDefaults.itemShape(index, types.size)) { Text(type.label) } }
        }
    }
}

@Composable
private fun ResultCard(result: AnalysisResult) {
    val color = when (result.verdict.lowercase()) { "malicious" -> Color(0xFFB3261E); "suspicious" -> Color(0xFF8A5300); "safe" -> Color(0xFF146C2E); else -> Color(0xFF455A64) }
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .10f))) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(result.verdict.uppercase(), color = color, fontWeight = FontWeight.Bold); Text(result.summary)
        result.confidence?.let { Text("Confidence: ${(it * 100).toInt()}%") }
        result.evidence.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    } }
}

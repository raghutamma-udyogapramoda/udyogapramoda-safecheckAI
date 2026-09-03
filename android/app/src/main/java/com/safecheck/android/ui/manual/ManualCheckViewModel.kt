package com.safecheck.android.ui.manual

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.extract.OcrExtractor
import com.safecheck.android.domain.usecase.AnalyzeContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manual Check state holder. Wires submit -> RedactionEngine -> AnalyzeContentUseCase ->
 * mock API and emits [AnalysisPhase.Done] with the resulting case id (T3.5). Also handles
 * QR (decoded on-device) and screenshot OCR inputs (Phase 4).
 */
class ManualCheckViewModel(
    private val analyze: AnalyzeContentUseCase,
    private val ocrExtractor: OcrExtractor,
) : ViewModel() {

    private val _state = MutableStateFlow(ManualCheckUiState())
    val state: StateFlow<ManualCheckUiState> = _state.asStateFlow()

    fun select(type: ManualInputType) = _state.update { it.copy(selected = type, phase = AnalysisPhase.Idle) }
    fun onText(value: String) = _state.update { it.copy(text = value) }
    fun onUrl(value: String) = _state.update { it.copy(url = value) }
    fun onEmailSender(value: String) = _state.update { it.copy(emailSender = value) }
    fun onEmailBody(value: String) = _state.update { it.copy(emailBody = value) }

    fun consumeResult() = _state.update { it.copy(phase = AnalysisPhase.Idle) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return

        val (rawContent, title) = when (s.selected) {
            ManualInputType.TEXT -> s.text to "Pasted text"
            ManualInputType.URL -> s.url to "URL check"
            ManualInputType.EMAIL -> buildString {
                if (s.emailSender.isNotBlank()) append("From: ").append(s.emailSender).append('\n')
                append(s.emailBody)
            } to "Email check"
            else -> return // QR/Screenshot/Document handled by dedicated methods
        }
        runAnalysis(s.selected.apiType, rawContent, title)
    }

    /** Called with the on-device-decoded QR payload (never auto-opened). */
    fun analyzeQr(decoded: String) {
        if (decoded.isBlank()) return
        runAnalysis(ManualInputType.QR.apiType, decoded, "QR check")
    }

    /** Called with a picked image; runs OCR on-device, then analyzes the extracted text. */
    fun analyzeImage(uri: Uri) {
        _state.update { it.copy(phase = AnalysisPhase.Analyzing) }
        viewModelScope.launch {
            try {
                val text = ocrExtractor.extract(uri)
                if (text.isBlank()) {
                    _state.update {
                        it.copy(phase = AnalysisPhase.Error("Couldn't read text from that image. Try a clearer screenshot or paste the text."))
                    }
                    return@launch
                }
                val case = analyze(
                    inputType = ManualInputType.SCREENSHOT.apiType,
                    rawContent = text,
                    sourceType = "manual",
                    title = "Screenshot check",
                )
                _state.update { it.copy(phase = AnalysisPhase.Done(case.caseId)) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(phase = AnalysisPhase.Error("Couldn't read that image. Try a clearer screenshot or paste the text."))
                }
            }
        }
    }

    private fun runAnalysis(inputType: String, rawContent: String, title: String) {
        _state.update { it.copy(phase = AnalysisPhase.Analyzing) }
        viewModelScope.launch {
            try {
                val case = analyze(
                    inputType = inputType,
                    rawContent = rawContent,
                    sourceType = "manual",
                    title = title,
                )
                _state.update { it.copy(phase = AnalysisPhase.Done(case.caseId)) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(phase = AnalysisPhase.Error("Could not analyze right now. Please try again."))
                }
            }
        }
    }

    /** Manual-DI factory (design.md §1 — no Hilt). */
    class Factory(
        private val analyze: AnalyzeContentUseCase,
        private val ocrExtractor: OcrExtractor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ManualCheckViewModel(analyze, ocrExtractor) as T
    }
}

package com.safecheck.android.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.domain.usecase.AnalyzeContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Analyzes an on-device-decoded QR payload through the same pipeline as any other input
 * (design.md §6). The payload is never opened; it is only submitted for analysis.
 */
class QrScanViewModel(
    private val analyze: AnalyzeContentUseCase,
) : ViewModel() {

    private val _phase = MutableStateFlow<AnalysisPhase>(AnalysisPhase.Idle)
    val phase: StateFlow<AnalysisPhase> = _phase.asStateFlow()

    private var handled = false

    fun onDecoded(decoded: String) {
        if (handled || decoded.isBlank()) return
        handled = true
        _phase.update { AnalysisPhase.Analyzing }
        viewModelScope.launch {
            try {
                val case = analyze(
                    inputType = ManualInputType.QR.apiType,
                    rawContent = decoded,
                    sourceType = "manual",
                    title = "QR check",
                )
                _phase.update { AnalysisPhase.Done(case.caseId) }
            } catch (t: Throwable) {
                handled = false
                _phase.update { AnalysisPhase.Error("Could not analyze the QR. Please try again.") }
            }
        }
    }

    class Factory(private val analyze: AnalyzeContentUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QrScanViewModel(analyze) as T
    }
}

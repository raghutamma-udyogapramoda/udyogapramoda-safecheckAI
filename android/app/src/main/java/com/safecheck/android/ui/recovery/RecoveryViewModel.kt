package com.safecheck.android.ui.recovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.domain.usecase.RecordRecoveryIncidentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecoveryUiState(
    val currentIndex: Int = 0,
    val completedActionsByStage: Map<Int, Set<Int>> = emptyMap(),
    val completed: Boolean = false,
)

/**
 * Recovery wizard state (R-9). Walks STOP -> SECURE -> REPORT -> DOCUMENT -> LEARN/PREVENT,
 * recording each stage to the shared API via [RecordRecoveryIncidentUseCase]. No secrets are
 * ever collected or stored (R-9.1.4).
 */
class RecoveryViewModel(
    private val recordIncident: RecordRecoveryIncidentUseCase,
    private val caseId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(RecoveryUiState())
    val state: StateFlow<RecoveryUiState> = _state.asStateFlow()

    private val stages = RecoveryContent.stages

    fun toggleAction(actionIndex: Int) {
        val idx = _state.value.currentIndex
        val current = _state.value.completedActionsByStage[idx].orEmpty().toMutableSet()
        if (!current.add(actionIndex)) current.remove(actionIndex)
        _state.update {
            it.copy(completedActionsByStage = it.completedActionsByStage + (idx to current))
        }
    }

    fun next() {
        val idx = _state.value.currentIndex
        val stage = stages[idx]
        val doneLabels = _state.value.completedActionsByStage[idx].orEmpty()
            .map { stage.actions[it] }

        // Record this stage (governance-only content; no secrets).
        viewModelScope.launch {
            runCatching {
                recordIncident(
                    caseId = caseId,
                    incidentState = stage.state.name,
                    recoveryActions = doneLabels,
                )
            }
        }

        if (idx < stages.lastIndex) {
            _state.update { it.copy(currentIndex = idx + 1) }
        } else {
            _state.update { it.copy(completed = true) }
        }
    }

    fun back() {
        val idx = _state.value.currentIndex
        if (idx > 0) _state.update { it.copy(currentIndex = idx - 1) }
    }

    class Factory(
        private val recordIncident: RecordRecoveryIncidentUseCase,
        private val caseId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecoveryViewModel(recordIncident, caseId) as T
    }
}

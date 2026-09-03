package com.safecheck.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.domain.model.SafetyCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Home state holder. Observes the CaseStore so completed checks appear in recent activity
 * (requirements R-2.1.3). Automatic-protection status is refined in Phase 9.
 */
class HomeViewModel(
    private val caseStore: CaseStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            caseStore.observeRecent(limit = 10).collect { cases ->
                _state.value = _state.value.copy(recent = cases.map { it.toRecentItem() })
            }
        }
    }

    fun setProtectionSummary(summary: String) {
        _state.value = _state.value.copy(protectionSummary = summary)
    }

    fun clearHistory() {
        viewModelScope.launch {
            caseStore.clearAll()
        }
    }

    fun deleteCase(caseId: String) {
        viewModelScope.launch {
            caseStore.delete(caseId)
        }
    }

    private fun SafetyCase.toRecentItem() = RecentCaseItem(
        caseId = caseId,
        title = title,
        subtitle = shortReason(),
        band = band,
        score = score,
    )

    private fun SafetyCase.shortReason(): String =
        result.evidence.firstOrNull()?.label ?: result.band.label + " risk"

    class Factory(private val caseStore: CaseStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(caseStore) as T
    }
}

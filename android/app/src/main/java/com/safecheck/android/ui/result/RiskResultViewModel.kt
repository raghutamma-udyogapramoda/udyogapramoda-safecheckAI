package com.safecheck.android.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.domain.model.SafetyCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RiskResultState {
    data object Loading : RiskResultState
    data class Loaded(val case: SafetyCase) : RiskResultState
    data object NotFound : RiskResultState
}

class RiskResultViewModel(
    private val caseStore: CaseStore,
    private val caseId: String,
) : ViewModel() {

    private val _state = MutableStateFlow<RiskResultState>(RiskResultState.Loading)
    val state: StateFlow<RiskResultState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val case = caseStore.get(caseId)
            _state.value = if (case != null) RiskResultState.Loaded(case) else RiskResultState.NotFound
        }
    }

    class Factory(
        private val caseStore: CaseStore,
        private val caseId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RiskResultViewModel(caseStore, caseId) as T
    }
}

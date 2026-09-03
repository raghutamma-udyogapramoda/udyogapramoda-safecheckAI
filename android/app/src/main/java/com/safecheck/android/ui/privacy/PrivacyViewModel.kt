package com.safecheck.android.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.store.SettingsStore
import com.safecheck.android.domain.model.AuditLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PrivacyUiState(
    val largeText: Boolean = false,
    val language: String = "en",
    val audit: List<AuditLogEntry> = emptyList(),
)

class PrivacyViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(PrivacyUiState())
    val state: StateFlow<PrivacyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsStore.largeTextEnabled.collect { v ->
                _state.value = _state.value.copy(largeText = v)
            }
        }
        viewModelScope.launch {
            settingsStore.selectedLanguage.collect { lang ->
                _state.value = _state.value.copy(language = lang)
            }
        }
        viewModelScope.launch {
            settingsStore.auditEntries.collect { entries ->
                _state.value = _state.value.copy(audit = entries)
            }
        }
    }

    fun setLargeText(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setLargeTextEnabled(enabled) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsStore.setSelectedLanguage(lang) }
    }

    class Factory(private val settingsStore: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PrivacyViewModel(settingsStore) as T
    }
}

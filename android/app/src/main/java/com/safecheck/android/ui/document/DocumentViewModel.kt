package com.safecheck.android.ui.document

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.domain.model.DocumentAnalysis
import com.safecheck.android.domain.usecase.SubmitDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DocumentState {
    data object Idle : DocumentState
    data object Analyzing : DocumentState
    data class Loaded(val analysis: DocumentAnalysis) : DocumentState
    data class Error(val message: String) : DocumentState
}

class DocumentViewModel(
    private val submitDocument: SubmitDocumentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DocumentState>(DocumentState.Idle)
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    /** @param uri picked PDF, or null to run the bundled sample document. */
    fun analyze(uri: Uri?) {
        _state.value = DocumentState.Analyzing
        viewModelScope.launch {
            try {
                val analysis = submitDocument(uri)
                _state.value = DocumentState.Loaded(analysis)
            } catch (t: Throwable) {
                _state.value = DocumentState.Error("Could not read that document. Try the sample document instead.")
            }
        }
    }

    class Factory(private val submitDocument: SubmitDocumentUseCase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DocumentViewModel(submitDocument) as T
    }
}

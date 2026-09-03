package com.safecheck.android.ui.circle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.store.CaseStore
import com.safecheck.android.data.store.ContactStore
import com.safecheck.android.domain.model.ReviewDecision
import com.safecheck.android.domain.model.SafetyCase
import com.safecheck.android.domain.model.TrustedContact
import com.safecheck.android.domain.usecase.ShareToSafetyCircleUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Advisory(
    val reviewerName: String,
    val decision: ReviewDecision,
    val note: String,
    val simulated: Boolean = true,
)

data class SafetyCircleUiState(
    val case: SafetyCase? = null,
    val contacts: List<TrustedContact> = emptyList(),
    val sanitizedSummary: String = "",
    val reviewLink: String? = null,
    val expiresInMinutes: Int = 0,
    val sharing: Boolean = false,
    val awaitingResponse: Boolean = false,
    val advisory: Advisory? = null,
    val noResponse: Boolean = false,
)

/**
 * Safety Circle (R-7). Shares a sanitized summary with a trusted contact and shows an
 * advisory opinion beside the immutable machine score. The advisory is simulated for the
 * hackathon and clearly labeled. If no response arrives, the app defaults to the safe
 * recommendation (R-7.1.4) — it never implies approval.
 */
class SafetyCircleViewModel(
    private val caseStore: CaseStore,
    private val contactStore: ContactStore,
    private val shareUseCase: ShareToSafetyCircleUseCase,
    private val caseId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(SafetyCircleUiState())
    val state: StateFlow<SafetyCircleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            contactStore.contacts.collect { list ->
                _state.update { it.copy(contacts = list) }
            }
        }
        if (caseId != null) {
            viewModelScope.launch {
                val case = caseStore.get(caseId)
                if (case != null) {
                    _state.update {
                        it.copy(case = case, sanitizedSummary = shareUseCase.buildSanitizedSummary(case))
                    }
                }
            }
        }
    }

    fun addContact(name: String, relationship: String, channel: String, phoneNumber: String = "", isPrimary: Boolean = false) {
        contactStore.add(name, relationship, channel, phoneNumber, isPrimary)
    }

    fun updateContact(contactId: String, name: String, relationship: String, channel: String, phoneNumber: String, isPrimary: Boolean) {
        contactStore.update(contactId, name, relationship, channel, phoneNumber, isPrimary)
    }

    fun deleteContact(contactId: String) {
        contactStore.delete(contactId)
    }

    fun setPrimaryContact(contactId: String) {
        contactStore.setPrimary(contactId)
    }

    fun share(contact: TrustedContact) {
        val case = _state.value.case ?: return
        _state.update { it.copy(sharing = true, advisory = null, noResponse = false) }
        viewModelScope.launch {
            val outcome = shareUseCase(case, contact.contactId)
            _state.update {
                it.copy(
                    sharing = false,
                    awaitingResponse = true,
                    reviewLink = outcome.reviewLink,
                    expiresInMinutes = outcome.expiresInMinutes,
                )
            }
        }
    }

    /** Simulate the trusted contact replying (clearly labeled as simulated in the UI). */
    fun simulateAdvisory(contact: TrustedContact) {
        val case = _state.value.case ?: return
        viewModelScope.launch {
            delay(900)
            val advisory = advisoryFor(case, contact.name)
            _state.update { it.copy(awaitingResponse = false, advisory = advisory, noResponse = false) }
        }
    }

    /** Simulate no response — the app must default to the safe recommendation (R-7.1.4). */
    fun simulateNoResponse() {
        _state.update { it.copy(awaitingResponse = false, advisory = null, noResponse = true) }
    }

    private fun advisoryFor(case: SafetyCase, reviewerName: String): Advisory {
        return when (case.result.band) {
            com.safecheck.android.ui.theme.RiskBand.HIGH -> Advisory(
                reviewerName = reviewerName,
                decision = ReviewDecision.LOOKS_SUSPICIOUS,
                note = "This looks completely fake — real banks don't use odd domains or ask for a fee. Do not click or pay.",
            )
            com.safecheck.android.ui.theme.RiskBand.MEDIUM -> Advisory(
                reviewerName = reviewerName,
                decision = ReviewDecision.UNSURE,
                note = "I'm not sure. Better to verify with the official app before doing anything.",
            )
            com.safecheck.android.ui.theme.RiskBand.UNCERTAIN -> Advisory(
                reviewerName = reviewerName,
                decision = ReviewDecision.UNSURE,
                note = "I can't tell for sure. Do not click or pay until we verify directly through a known channel.",
            )
            com.safecheck.android.ui.theme.RiskBand.LOW -> Advisory(
                reviewerName = reviewerName,
                decision = ReviewDecision.LOOKS_SAFE,
                note = "Looks okay to me, but stay careful if it later asks for money or an OTP.",
            )
        }
    }

    class Factory(
        private val caseStore: CaseStore,
        private val contactStore: ContactStore,
        private val shareUseCase: ShareToSafetyCircleUseCase,
        private val caseId: String?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SafetyCircleViewModel(caseStore, contactStore, shareUseCase, caseId) as T
    }
}

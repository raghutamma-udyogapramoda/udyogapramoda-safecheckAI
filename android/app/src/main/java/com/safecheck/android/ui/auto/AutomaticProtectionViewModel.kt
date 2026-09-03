package com.safecheck.android.ui.auto

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Telephony
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.safecheck.android.data.store.SettingsStore
import com.safecheck.android.sms.DemoSmsTrigger
import com.safecheck.android.sms.SmsBroadcastReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AutoProtectionUiState(
    val smsEnabled: Boolean = false,
    val lastDemoCaseId: String? = null,
    val demoRunning: Boolean = false,
)

/**
 * Automatic Protection (R-5.1, R-5.2). Manages per-channel opt-in, dynamically registers the
 * real SMS receiver only after consent + permission, and hosts the Demo Simulation trigger.
 * Notifications/Calls channels remain P1 (shown disabled).
 */
class AutomaticProtectionViewModel(
    private val appContext: Context,
    private val settingsStore: SettingsStore,
    private val demoSmsTrigger: DemoSmsTrigger,
) : ViewModel() {

    val smsEnabled: StateFlow<Boolean> =
        settingsStore.smsChannelEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _demo = MutableStateFlow(AutoProtectionUiState())
    val demoState: StateFlow<AutoProtectionUiState> = _demo

    private var receiver: SmsBroadcastReceiver? = null

    /** Called after the user has granted RECEIVE_SMS (or to turn the channel off). */
    fun setSmsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSmsChannelEnabled(enabled) }
        if (enabled) registerReceiver() else unregisterReceiver()
    }

    private fun registerReceiver() {
        if (receiver != null) return
        val r = SmsBroadcastReceiver()
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        // Priority helps ensure delivery; still best-effort by design.
        filter.priority = 999
        // API 33+ requires an explicit export flag. SMS_RECEIVED is a system broadcast, so the
        // receiver must be exported to receive it.
        ContextCompat.registerReceiver(
            appContext, r, filter, ContextCompat.RECEIVER_EXPORTED
        )
        receiver = r
    }

    private fun unregisterReceiver() {
        receiver?.let {
            runCatching { appContext.unregisterReceiver(it) }
            receiver = null
        }
    }

    /** Fire the demo simulation — same pipeline, same notification, same Risk Result. */
    fun fireDemo() {
        _demo.value = _demo.value.copy(demoRunning = true)
        viewModelScope.launch {
            val case = demoSmsTrigger.fire()
            _demo.value = _demo.value.copy(demoRunning = false, lastDemoCaseId = case.caseId)
        }
    }

    fun consumeDemoResult() {
        _demo.value = _demo.value.copy(lastDemoCaseId = null)
    }

    override fun onCleared() {
        super.onCleared()
        // Note: for a hackathon MVP the receiver lifecycle is tied to this VM; a production
        // build would host it in a foreground service. Kept simple per design.md §1.
    }

    class Factory(
        private val appContext: Context,
        private val settingsStore: SettingsStore,
        private val demoSmsTrigger: DemoSmsTrigger,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AutomaticProtectionViewModel(appContext, settingsStore, demoSmsTrigger) as T
    }
}

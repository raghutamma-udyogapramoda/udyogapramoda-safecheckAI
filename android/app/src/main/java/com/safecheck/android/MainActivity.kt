package com.safecheck.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.safecheck.android.notify.RiskNotifier
import com.safecheck.android.ui.LocalAppContainer
import com.safecheck.android.ui.nav.SafeCheckNavHost
import com.safecheck.android.ui.theme.SafeCheckTheme

/**
 * Single-Activity host (design.md §1, §8). Provides the shared [AppContainer] to the
 * composition, applies the SafeCheck theme, and renders the navigation shell.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as SafeCheckApp).container
        val initialCaseId = intent?.getStringExtra(RiskNotifier.EXTRA_CASE_ID)
        setContent {
            val largeText by container.settingsStore.largeTextEnabled.collectAsState(initial = false)
            CompositionLocalProvider(LocalAppContainer provides container) {
                SafeCheckTheme(textScale = if (largeText) 1.3f else 1.0f) {
                    SafeCheckNavHost(initialCaseId = initialCaseId)
                }
            }
        }
    }
}

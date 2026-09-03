package com.safecheck.android.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.safecheck.android.di.AppContainer

/**
 * Provides the [AppContainer] to composables so screens can obtain use cases/stores for
 * their manual-DI ViewModel factories (design.md §1). Set once in MainActivity.
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

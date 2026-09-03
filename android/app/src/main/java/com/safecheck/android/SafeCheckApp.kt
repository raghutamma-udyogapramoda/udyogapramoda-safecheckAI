package com.safecheck.android

import android.app.Application
import com.safecheck.android.di.AppContainer

/**
 * Application entry point. Owns the [AppContainer] composition root so the whole
 * app shares one set of dependencies (design.md §1).
 */
class SafeCheckApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

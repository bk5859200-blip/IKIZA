package com.ikiza.companion

import android.app.Application

/**
 * Application class for IKIZA Companion.
 * Runs strictly offline with zero network or external telemetry libraries.
 */
class IkizaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

package com.camara.app

import android.app.Application
import com.camara.app.diagnostics.DiagnosticsLogger

/**
 * Application entry point.
 *
 * Diagnostics logging is started here and is **on by default** (App Factory Rule 7): it
 * records the app's own log plus uncaught crashes, so a problem can be diagnosed without
 * asking the user to reproduce it first. The user can turn it off in Settings.
 */
class CamaraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DiagnosticsLogger.init(this)
        DiagnosticsLogger.installCrashHandler()
    }
}

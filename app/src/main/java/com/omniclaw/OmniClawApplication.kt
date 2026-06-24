package com.omniclaw

import android.app.Application
import com.omniclaw.core.di.AppContainer
import com.omniclaw.core.di.DefaultAppContainer
import com.omniclaw.core.logging.FileLogger

private const val TAG = "OmniClawApp"

class OmniClawApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        FileLogger.init(this)
        FileLogger.i(TAG, "App starting...")
        try {
            container = DefaultAppContainer(this)
            FileLogger.i(TAG, "AppContainer initialized successfully")
        } catch (e: Exception) {
            FileLogger.e(TAG, "FAILED to initialize AppContainer", e)
            throw e
        }
    }
}

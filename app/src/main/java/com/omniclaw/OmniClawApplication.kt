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
        // FileLogger.init() logs the startup context (version, device, session ID)
        FileLogger.init(this)
        FileLogger.i(TAG, "App init start")
        try {
            container = DefaultAppContainer(this)
            FileLogger.i(TAG, "App init success", "container=DefaultAppContainer")
        } catch (e: Exception) {
            FileLogger.e(TAG, "App init failed", e, "container=DefaultAppContainer")
            throw e
        }
    }
}

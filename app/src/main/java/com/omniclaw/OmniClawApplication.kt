package com.omniclaw

import android.app.Application
import com.omniclaw.core.di.AppContainer
import com.omniclaw.core.di.DefaultAppContainer

class OmniClawApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}

package com.omniclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.core.logging.FileLogger
import com.omniclaw.ui.navigation.AppShell
import com.omniclaw.ui.navigation.Routes
import com.omniclaw.ui.screens.SetupWizardScreen
import com.omniclaw.ui.viewmodels.MainViewModel
import com.omniclaw.ui.theme.OmniClawTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileLogger.i(TAG, "onCreate start")
        try {
            enableEdgeToEdge()
            setContent {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val themeMode by viewModel.themeMode.collectAsState()
                OmniClawTheme(themeMode = themeMode) {
                    val destination by viewModel.startDestination.collectAsState()
                    destination?.let { dest ->
                        FileLogger.d(TAG, "Navigation", "destination=$dest")
                        if (dest == Routes.SETUP) {
                            SetupWizardScreen(onFinishSetup = { })
                        } else {
                            AppShell()
                        }
                    }
                }
            }
            FileLogger.i(TAG, "onCreate success")
        } catch (e: Exception) {
            FileLogger.e(TAG, "onCreate failed", e)
            throw e
        }
    }
}

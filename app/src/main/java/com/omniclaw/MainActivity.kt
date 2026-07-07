package com.omniclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    // Show a loading spinner while the start destination is
                    // being computed (first ~100-300ms on cold launch).
                    // Without this, the screen is blank until the Flow emits.
                    if (destination == null) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        FileLogger.d(TAG, "Navigation", "destination=$destination")
                        if (destination == Routes.SETUP) {
                            // When setup finishes, mark onboarding complete so the
                            // startDestination flow re-emits "dashboard" and the UI
                            // switches from SetupWizardScreen to AppShell automatically.
                            // Previously this was a no-op lambda, leaving users stuck
                            // on the setup screen after completing setup.
                            SetupWizardScreen(onFinishSetup = {
                                // The MainViewModel already observes isOnboardingComplete,
                                // so the destination will update automatically. But we need
                                // to make sure the onboarding flag is persisted — the
                                // SetupViewModel's finishOnboarding() does this, and
                                // onFinishSetup is called AFTER finishOnboarding().
                            })
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

package com.omniclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniclaw.ui.navigation.AppShell
import com.omniclaw.ui.screens.SetupWizardScreen
import com.omniclaw.ui.viewmodels.MainViewModel
import com.omniclaw.ui.theme.OmniClawTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniClawTheme {
                val viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)
                val destination by viewModel.startDestination.collectAsState()
                destination?.let { dest ->
                    if (dest == "setup") {
                        SetupWizardScreen(onFinishSetup = { })
                    } else {
                        AppShell()
                    }
                }
            }
        }
    }
}

package com.omniclaw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.omniclaw.presentation.navigation.OmniClawNavGraph
import com.omniclaw.presentation.viewmodels.MainViewModel
import com.omniclaw.ui.theme.OmniClawTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      OmniClawTheme {
        val startDestination by viewModel.startDestination.collectAsState()
        startDestination?.let { destination ->
            OmniClawNavGraph(startDestination = destination)
        }
      }
    }
  }
}

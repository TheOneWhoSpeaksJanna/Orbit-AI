package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.presentation.navigation.OrbitNavGraph
import com.example.presentation.viewmodels.MainViewModel
import com.example.ui.theme.OrbitTheme

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      OrbitTheme {
        val startDestination by viewModel.startDestination.collectAsState()
        startDestination?.let { destination ->
            OrbitNavGraph(startDestination = destination)
        }
      }
    }
  }
}

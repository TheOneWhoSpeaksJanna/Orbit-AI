package com.example.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.OrbitApplication
import com.example.domain.model.ChatSession
import com.example.domain.model.Project
import com.example.domain.repository.OrbitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DashboardViewModel(
    private val repository: OrbitRepository,
    private val appContainer: com.example.core.di.AppContainer
) : ViewModel() {

    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessions: StateFlow<List<ChatSession>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeAgent = appContainer.prefsManager.selectedAgent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Hermes"
        )
        
    val activeProvider = appContainer.prefsManager.selectedProvider
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Claude"
        )
    
    val shizukuEnabled = appContainer.prefsManager.shizukuEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun createNewProject(name: String, description: String) {
        viewModelScope.launch {
            val project = Project(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertProject(project)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OrbitApplication
                return DashboardViewModel(application.container.repository, application.container) as T
            }
        }
    }
}

package com.omniclaw.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.omniclaw.OmniClawApplication
import com.omniclaw.domain.models.ChatSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(
    sessionsFlow: Flow<List<ChatSession>>
) : ViewModel() {

    private val _state = MutableStateFlow<List<ChatSession>>(emptyList())
    val state: StateFlow<List<ChatSession>> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            sessionsFlow.collect { sessions ->
                _state.value = sessions
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as OmniClawApplication
                return HistoryViewModel(application.container.repository.getAllSessions()) as T
            }
        }
    }
}

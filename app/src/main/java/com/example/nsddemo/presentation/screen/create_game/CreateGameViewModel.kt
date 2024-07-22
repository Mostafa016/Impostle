package com.example.nsddemo.presentation.screen.create_game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.presentation.ServerViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CreateGameViewModel(serverViewModel: ServerViewModel, nsdHelper: NSDHelper) : ViewModel() {
    val isGameCreated = nsdHelper.isServiceRegistered

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        serverViewModel.createServer()
    }

    fun onEvent(event: CreateGameEvent) {
        when (event) {
            CreateGameEvent.GameCreated -> {
                navigateToLobby()
            }
        }
    }

    private fun navigateToLobby() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class CreateGameViewModelFactory(
            private val serverViewModel: ServerViewModel,
            private val nsdHelper: NSDHelper,
        ) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CreateGameViewModel(serverViewModel, nsdHelper) as T
        }
    }
}
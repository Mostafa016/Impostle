package com.example.nsddemo.presentation.screen.join_game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.R
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.presentation.ClientViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class JoinGameViewModel(
    private val clientViewModel: ClientViewModel,
    private val gameRepository: GameRepository,
    private val nsdHelper: NSDHelper
) : ViewModel() {
    val hasFoundGame = gameRepository.clientGameState.map { it is GameState.ClientFoundGame }
    val hasJoinedGame = gameRepository.clientGameState.map { it is GameState.ClientGameStarted }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            nsdHelper.isServiceResolved.first { it }
            Log.d(TAG, "Service resolved")
            stopSearchingForGame()
        }
    }

    private lateinit var gameCode: String

    private val _state = MutableStateFlow(JoinGameState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "JoinGameViewModel onCleared() called")
    }

    fun onEvent(event: JoinGameEvent) {
        when (event) {
            is JoinGameEvent.GameCodeTextFieldValueChange -> onGameCodeTextFieldValueChange(event.text)
            JoinGameEvent.JoinGame -> onJoinGamePressed()
            JoinGameEvent.GoBackToMainMenu -> onGoBackToMainMenu()
            JoinGameEvent.GameSearchTimedOut -> onGameSearchTimedOut()
            JoinGameEvent.GameFound -> onGameFound()
            JoinGameEvent.GameStarted -> onGameStarted()
        }
    }

    private fun stopSearchingForGame() {
        nsdHelper.stopServiceDiscovery()
        _state.value = state.value.copy(
            gameCodeTextFieldText = ""
        )
    }

    private fun onGameCodeTextFieldValueChange(text: String) {
        if (text.length > GameConstants.CODE_LENGTH) {
            return
        }
        _state.value = state.value.copy(
            gameCodeTextFieldText = text.uppercase()
        )
    }

    private fun onJoinGamePressed() {
        _state.value = state.value.copy(
            gameCodeTextFieldEnabled = false
        )
        discoverAndResolveService()
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                gameCode = gameCode.uppercase()
            )
        )
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.JoinGameLoading.route))
        }
    }

    private fun discoverAndResolveService() {
        gameCode = _state.value.gameCodeTextFieldText
        Log.d(TAG, "Game Code: $gameCode")
        clientViewModel.discoverServiceWithGameCode(gameCode)
    }

    private fun onGoBackToMainMenu() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
        }
    }

    private fun onGameSearchTimedOut() {
        stopSearchingForGame()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackBar(R.string.game_not_found))
            delay(200L)
            _eventFlow.emit(UiEvent.NavigateTo(Routes.GameSession.route))
        }
    }

    private fun onGameFound() {
        Log.d(TAG, "Game found")
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowSnackBar(R.string.found_game_waiting_for_host_to_start_game))
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Lobby.route))
        }
    }

    private fun onGameStarted() {
        Log.d(TAG, "Game started")
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.CategoryAndWord.route))
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class JoinGameViewModelFactory(
            private val clientViewModel: ClientViewModel,
            private val gameRepository: GameRepository,
            private val nsdHelper: NSDHelper
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                JoinGameViewModel(clientViewModel, gameRepository, nsdHelper) as T
        }
    }
}
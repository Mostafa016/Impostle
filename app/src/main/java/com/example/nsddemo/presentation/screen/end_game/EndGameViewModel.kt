package com.example.nsddemo.presentation.screen.end_game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.local.network.NSDHelper
import com.example.nsddemo.data.local.network.socket.Server
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.legacy.GameData
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EndGameViewModel(
    private val gameRepository: GameRepository, private val nsdHelper: NSDHelper
) : ViewModel() {
    private val isHost = gameRepository.gameData.value.isHost!!

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun onEvent(event: EndGameEvent) {
        when (event) {
            EndGameEvent.EndGame -> endGame()
        }
    }

    private fun endGame() {
        resetGameParameters()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.MainMenu.route))
        }
    }

    private fun resetGameParameters() {
        if (isHost) {
            nsdHelper.unregisterService()
            Server.clients.clear()
        }
        gameRepository.updateGameData(
            GameData(
                currentPlayer = gameRepository.gameData.value.currentPlayer!!.copy(color = GameConstants.DEFAULT_PLAYER_COLOR),
            )
        )
        gameRepository.updateGameState(
            GameState.Transitioning(gameRepository.gameState.value, GameState.StartGame)
        )
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class EndGameViewModelFactory(
            private val gameRepository: GameRepository, private val nsdHelper: NSDHelper
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EndGameViewModel(gameRepository, nsdHelper) as T
        }
    }
}
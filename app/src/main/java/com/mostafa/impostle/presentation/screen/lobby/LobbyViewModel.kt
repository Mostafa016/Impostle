package com.mostafa.impostle.presentation.screen.lobby

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mostafa.impostle.core.util.Debugging.TAG
import com.mostafa.impostle.domain.engine.GameSession
import com.mostafa.impostle.presentation.util.BaseGameViewModel
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LobbyViewModel
    @Inject
    constructor(
        gameSession: GameSession,
    ) : BaseGameViewModel(gameSession) {
        // Derived UI State
        val gameCode = gameData.value.gameCode
        val isHost = gameData.value.isHost
        val localPlayerId = gameData.value.localPlayerId

        val state =
            gameSession.gameData
                .map {
                    LobbyState(
                        chosenCategory = it.category,
                        players = it.players.values.toList(),
                        isStartRoundButtonEnabled = it.players.size >= 2 && it.category != null,
                    )
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LobbyState())

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        fun onEvent(event: LobbyEvent) {
            when (event) {
                is LobbyEvent.ChooseCategoryButtonClick -> {
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.NavigateTo(Routes.ChooseCategory.route))
                    }
                }

                is LobbyEvent.StartRound -> {
                    viewModelScope.launch {
                        activeClient?.startGame()
                    }
                }

                is LobbyEvent.KickPlayer ->
                    viewModelScope.launch {
                        activeClient?.kickPlayer(event.playerId)
                    }
            }
        }

        override fun onCleared() {
            super.onCleared()
            Log.i(TAG, "LobbyViewModel: Cleared!")
        }
    }

package com.example.nsddemo.presentation.screen.role_reveal

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.uiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleRevealViewModel @Inject constructor(
    gameSession: GameSession
) : BaseGameViewModel(gameSession) {

    private val _isConfirmPressed = MutableStateFlow(false)
    private val playersWithReadyState =
        gameData.map { gameData ->
            val readyPlayersMap = gameData.readyPlayers.associateBy { it.id }
            gameData.players.map {
                val isPlayerReady = it.key in readyPlayersMap.keys
                it.value.withReadyState(isPlayerReady)
            }
        }
    val state =
        combine(
            _isConfirmPressed,
            playersWithReadyState
        ) { isConfirmedPressed, playersWithReadyState ->
            RoleRevealState(
                isConfirmPressed = isConfirmedPressed,
                playersWithReadyState = playersWithReadyState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), RoleRevealState())

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // --- Static Data (Fetched Synchronously) ---
    val categoryNameResId: Int = gameData.value.category!!.uiCategory.nameResId
    val word: String? = gameData.value.word?.lowercase()
    val isImposter: Boolean = gameData.value.isImposter
    val localPlayerId = gameData.value.localPlayerId

    fun onEvent(event: RoleRevealEvent) {
        when (event) {
            RoleRevealEvent.ConfirmRoleReveal -> onConfirmClick()
        }
    }

    private fun onConfirmClick() {
        _isConfirmPressed.value = true
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "RoleRevealViewModel: Sending confirmation to server")
            activeClient?.confirmRole()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "RoleRevealViewModel: Cleared!")
    }
}
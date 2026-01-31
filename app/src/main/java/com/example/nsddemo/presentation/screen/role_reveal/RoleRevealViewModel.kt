package com.example.nsddemo.presentation.screen.role_reveal

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.GamePhase
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.uiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleRevealViewModel @Inject constructor(
    gameSession: GameSession
) : BaseGameViewModel(gameSession) {

    private val _state = MutableStateFlow(RoleRevealState())
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // --- Static Data (Fetched Synchronously) ---
    val categoryNameResId: Int = gameData.value.category!!.uiCategory.nameResId
    val word: String? = gameData.value.word?.lowercase()
    val isImposter: Boolean = gameData.value.isImposter

    init {
        // Navigation Logic
        viewModelScope.launch(Dispatchers.IO) {
            gamePhase.collectLatest { phase ->
                if (phase is GamePhase.InRound) {
                    _eventFlow.emit(UiEvent.NavigateTo(Routes.Question.route))
                }
            }
        }
    }

    fun onEvent(event: RoleRevealEvent) {
        when (event) {
            RoleRevealEvent.ConfirmRoleReveal -> onConfirmClick()
        }
    }

    private fun onConfirmClick() {
        _state.value = _state.value.copy(isConfirmPressed = true)
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "RoleRevealViewModel: Sending confirmation to server")
            activeClient?.confirmRole()
        }
    }
}
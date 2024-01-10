package com.example.nsddemo.ui.main_menu

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nsddemo.GameConstants
import com.example.nsddemo.Player
import com.example.nsddemo.ui.GameViewModel

class MainMenuViewModel(private val gameViewModel: GameViewModel) : ViewModel() {
    private val _playerNameDialogVisibilityState = mutableStateOf(false)
    val playerNameDialogVisibilityState: State<Boolean> = _playerNameDialogVisibilityState

    private val _playerNameTextFieldState = mutableStateOf("")
    val playerNameTextFieldState: State<String> = _playerNameTextFieldState

    private var onPlayerNameSave: () -> Unit = {}

    fun onPlayerNameChange(playerName: String) {
        _playerNameTextFieldState.value = playerName
    }

    fun savePlayerName() {
        val playerName = playerNameTextFieldState.value
        gameViewModel.updateCurrentPlayer(Player(playerName, GameConstants.DEFAULT_PLAYER_COLOR))
        _playerNameDialogVisibilityState.value = false
        onPlayerNameSave()
    }

    fun onCancelPlayerNameClick() {
        _playerNameDialogVisibilityState.value = false
    }

    fun showPlayerNameDialog(onPlayerNameSave: () -> Unit = {}) {
        _playerNameDialogVisibilityState.value = true
        this.onPlayerNameSave = onPlayerNameSave
    }

    fun onPlayerNameClick() {
        _playerNameDialogVisibilityState.value = true
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class MainMenuViewModelFactory(private val gameViewModel: GameViewModel) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainMenuViewModel(gameViewModel) as T
        }
    }
}
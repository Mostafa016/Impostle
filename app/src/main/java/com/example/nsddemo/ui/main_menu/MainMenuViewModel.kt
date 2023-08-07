package com.example.nsddemo.ui.main_menu

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.nsddemo.Player
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.category_and_word.ChooseCategoryViewModel

class MainMenuViewModel(private val gameViewModel: GameViewModel) : ViewModel() {
    private val _playerNameDialogVisibilityState = mutableStateOf(false)
    val playerNameDialogVisibilityState: State<Boolean> = _playerNameDialogVisibilityState

    private val _playerNameTextFieldState = mutableStateOf("")
    val playerNameTextFieldState: State<String> = _playerNameTextFieldState

    fun onPlayerNameChange(playerName: String) {
        _playerNameTextFieldState.value = playerName
    }

    fun savePlayerName() {
        val playerName = playerNameTextFieldState.value
        if (playerName.matches(Regex("^[A-Za-z_]+$"))) {
            gameViewModel.updateCurrentPlayer(Player(playerName, "FF000000"))
            _playerNameDialogVisibilityState.value = false
        }
    }

    fun onCancelPlayerNameClick() {
        _playerNameDialogVisibilityState.value = false
    }

    fun showPlayerNameDialog() {
        _playerNameDialogVisibilityState.value = true
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
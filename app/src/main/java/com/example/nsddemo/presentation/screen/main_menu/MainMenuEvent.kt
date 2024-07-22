package com.example.nsddemo.presentation.screen.main_menu

sealed interface MainMenuEvent {
    object SettingsClick : MainMenuEvent
    object PlayerNameClick : MainMenuEvent
    data class PlayerNameDialogTextChange(val playerName: String) : MainMenuEvent
    data class PlayerNameDialogSave(val playerName: String) : MainMenuEvent

    object PlayerNameDialogCancel : MainMenuEvent
    object CreateGameClick : MainMenuEvent
    object JoinGameClick : MainMenuEvent

}
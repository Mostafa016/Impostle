package com.mostafa.impostle.presentation.screen.mainmenu

data class MainMenuState(
    val isPlayerNameDialogVisible: Boolean = false,
    val playerNameTextFieldText: String = "",
    val playerName: String? = null,
    val isCreateGameButtonEnabled: Boolean = true,
    val isJoinGameButtonEnabled: Boolean = true,
)

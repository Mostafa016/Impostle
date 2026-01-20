package com.example.nsddemo.presentation.screen.lobby

import com.example.nsddemo.domain.model.Categories

sealed interface LobbyEvent {
    data object ChooseCategoryButtonClick : LobbyEvent
    data class ChooseCategory(val chosenCategory: Categories) : LobbyEvent
    data object StartRound : LobbyEvent
}
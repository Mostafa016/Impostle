package com.example.nsddemo.presentation.screen.choose_category

import com.example.nsddemo.domain.model.GameCategory

sealed interface ChooseCategoryEvent {
    data class CategoryChosen(val category: GameCategory) : ChooseCategoryEvent
    data object ConfirmSelection : ChooseCategoryEvent
}
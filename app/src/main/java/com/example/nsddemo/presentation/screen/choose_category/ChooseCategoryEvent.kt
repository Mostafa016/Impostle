package com.example.nsddemo.presentation.screen.choose_category

import com.example.nsddemo.domain.util.Categories

sealed interface ChooseCategoryEvent {
    data class CategoryChosen(val category: Categories) : ChooseCategoryEvent
}
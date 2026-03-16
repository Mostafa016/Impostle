package com.example.nsddemo.presentation.util

sealed interface UiEvent {
    data class ShowSnackBar(
        val messageResId: Int,
    ) : UiEvent

    data class NavigateTo(
        val destination: String,
        val popPrevious: Boolean = true,
    ) : UiEvent
}

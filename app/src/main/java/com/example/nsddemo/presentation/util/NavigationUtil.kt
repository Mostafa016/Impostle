package com.example.nsddemo.presentation.util

import androidx.navigation.NavHostController

object NavigationUtil {
    fun NavHostController.popBackStackAndNavigateTo(route: String) {
        navigate(route) {
            popUpTo(route) {
                inclusive
            }
        }
    }
}
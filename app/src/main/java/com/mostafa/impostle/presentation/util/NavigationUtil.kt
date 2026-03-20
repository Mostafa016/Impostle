package com.mostafa.impostle.presentation.util

import androidx.navigation.NavHostController

object NavigationUtil {
    fun NavHostController.popBackStackAndNavigateTo(
        route: String,
        popUpToRoute: String? = null,
    ) {
        navigate(route) {
            popUpTo(popUpToRoute ?: Routes.RootGraph.route)
        }
    }
}

package com.example.nsddemo.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController

object NavigationUtil {
    fun NavHostController.popBackStackAndNavigateTo(
        route: String,
        dynamicStartDestination: String? = null,
        graphWithDynamicDestinationRoute: String? = null,
        isPopInclusive: Boolean = true,
    ) {
        dynamicStartDestination?.let {
            val dynamicDestinationNavGraph = graph.findNode(graphWithDynamicDestinationRoute)!!
            (dynamicDestinationNavGraph as NavGraph).setStartDestination(it)
        }
        navigate(route) {
            popUpTo(currentDestination!!.route!!) {
                inclusive = isPopInclusive
            }
        }
    }

    // This is from Phlipp Lackner's video on nested navigation graphs
    @Composable
    inline fun <reified T : ViewModel> NavBackStackEntry.sharedViewModel(
        navController: NavController, factory: ViewModelProvider.Factory?
    ): T {
        val navGraphRoute = destination.parent?.route ?: return viewModel()
        val parentEntry = remember(this) {
            navController.getBackStackEntry(navGraphRoute)
        }
        return viewModel(viewModelStoreOwner = parentEntry, factory = factory)
    }
}
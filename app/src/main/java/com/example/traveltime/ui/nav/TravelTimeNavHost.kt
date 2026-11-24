package com.example.traveltime.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.traveltime.ui.screens.SettingsScreen
import com.example.traveltime.ui.screens.TripEditScreen
import com.example.traveltime.ui.screens.TripListScreen

object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{tripId}"
    fun edit(tripId: Long) = "edit/$tripId"
    const val SETTINGS = "settings"
}

@Composable
fun TravelTimeNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            TripListScreen(
                onOpen = { id -> nav.navigate(Routes.edit(id)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
        ) { entry ->
            TripEditScreen(
                tripId = entry.arguments?.getLong("tripId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}

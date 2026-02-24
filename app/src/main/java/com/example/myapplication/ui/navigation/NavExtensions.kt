package com.example.myapplication.ui.navigation

import androidx.navigation.NavController

/** Type-safe навигация (маршруты из Routes.kt). */
fun NavController.navigateToWelcome() {
    navigate(WelcomeRoute) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

fun NavController.navigateToHome() {
    navigate(HomeRoute) {
        popUpTo(WelcomeRoute) { inclusive = true }
    }
}

fun NavController.navigateToAddEdit(animeId: String? = null) {
    navigate(AddEditRoute(animeId = animeId))
}

fun NavController.navigateToSettings() {
    navigate(SettingsRoute)
}

fun NavController.navigateToDetails(animeId: String) {
    navigate(DetailsRoute(animeId = animeId))
}

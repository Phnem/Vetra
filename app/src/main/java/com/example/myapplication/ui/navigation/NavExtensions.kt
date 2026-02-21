package com.example.myapplication.ui.navigation

import androidx.navigation.NavController

/**
 * Extension функции для type-safe навигации
 */
fun NavController.navigateToWelcome() {
    navigate(AppRoute.Welcome.route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

fun NavController.navigateToHome() {
    navigate(AppRoute.Home.route) {
        popUpTo(AppRoute.Welcome.route) { inclusive = true }
    }
}

fun NavController.navigateToAddEdit(animeId: String? = null) {
    navigate(AppRoute.AddEdit.createRoute(animeId))
}

fun NavController.navigateToSettings() {
    navigate(AppRoute.Settings.route)
}

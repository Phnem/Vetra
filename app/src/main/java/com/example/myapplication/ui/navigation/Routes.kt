package com.example.myapplication.ui.navigation

/**
 * Type-safe navigation routes
 * Использует sealed class для compile-time проверки маршрутов
 */
sealed class AppRoute(val route: String) {
    object Splash : AppRoute("splash")
    object Welcome : AppRoute("welcome")
    object Home : AppRoute("home")

    object AddEdit : AppRoute("add_edit") {
        const val ROUTE = "add_edit?animeId={animeId}"
        const val ARG_ANIME_ID = "animeId"

        fun createRoute(animeId: String?): String {
            return if (animeId != null) "add_edit?animeId=$animeId" else "add_edit"
        }
    }

    object Settings : AppRoute("settings")

    object Details : AppRoute("details") {
        const val ARG_ANIME_ID = "animeId"
        const val ROUTE = "details?animeId={animeId}"
        fun createRoute(animeId: String) = "details?animeId=$animeId"
    }
}

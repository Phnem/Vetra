package com.example.myapplication.ui.navigation

import kotlinx.serialization.Serializable

/** Type-safe маршруты (Navigation Compose 2.8+). ID аниме — String. */
@Serializable
data object SplashRoute

@Serializable
data object WelcomeRoute

@Serializable
data object HomeRoute

@Serializable
data class DetailsRoute(val animeId: String)

@Serializable
data class AddEditRoute(val animeId: String? = null)

@Serializable
data object SettingsRoute

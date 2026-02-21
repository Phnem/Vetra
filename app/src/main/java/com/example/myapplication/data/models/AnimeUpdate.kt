package com.example.myapplication.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AnimeUpdate(
    val animeId: String,
    val title: String,
    val currentEpisodes: Int,
    val newEpisodes: Int,
    val source: String
)

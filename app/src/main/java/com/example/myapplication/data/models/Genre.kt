package com.example.myapplication.data.models

enum class GenreCategory { ANIME, MOVIE, SERIES }

data class GenreDefinition(
    val id: String,
    val ru: String,
    val en: String,
    val categories: List<GenreCategory>
)

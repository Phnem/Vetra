package com.example.myapplication.data.models

data class Anime(
    val id: String,
    val title: String,
    val episodes: Int,
    val rating: Int,
    val imageFileName: String?,
    val orderIndex: Int,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val categoryType: String = ""
)

data class RankedAnime(val anime: Anime, val score: Int)

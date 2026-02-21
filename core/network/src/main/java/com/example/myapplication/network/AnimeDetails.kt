package com.example.myapplication.network

data class AnimeDetails(
    val title: String,
    val altTitle: String?,
    val description: String,
    val type: String,
    val status: String,
    val episodesAired: Int,
    val episodesTotal: Int?,
    val nextEpisode: String?,
    val genres: List<String>,
    val rating: Int?,
    val posterUrl: String?,
    val source: String
)

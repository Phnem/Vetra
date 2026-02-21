package com.example.myapplication.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShikimoriSearchItemDto(
    val id: Int,
    val name: String? = null,
    val russian: String? = null,
    val kind: String? = null,
    val status: String? = null,
    val episodes: Int? = null,
    @SerialName("episodes_aired") val episodesAired: Int? = null,
    val score: String? = null,
    val description: String? = null,
    val image: ShikimoriImageDto? = null,
    val genres: List<ShikimoriGenreDto>? = null
)

@Serializable
data class ShikimoriImageDto(
    val original: String? = null,
    val preview: String? = null
)

@Serializable
data class ShikimoriGenreDto(
    val id: Int,
    val name: String? = null,
    val russian: String? = null
)

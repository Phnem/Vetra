package com.example.myapplication.ui.home

import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.models.SortOption

/**
 * Immutable UI state для HomeScreen.
 * Список аниме реактивно берётся из animeListFlow (БД + фильтр/сортировка в памяти).
 */
data class HomeUiState(
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RATING,
    val sortAscending: Boolean = false,
    val filterTags: List<String> = emptyList(),
    val filterCategory: String = "",
    val isGenreFilterVisible: Boolean = false,
    val isCheckingUpdates: Boolean = false,
    val updates: List<AnimeUpdate> = emptyList(),
    val selectedAnimeForDetails: Anime? = null,
    val statsAnimeList: List<Anime> = emptyList(),
    val isRestoringFromCloud: Boolean = false
)

package com.example.myapplication.ui.home

import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.models.SortOption
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable UI state для HomeScreen.
 * Список аниме реактивно берётся из animeListFlow (БД + фильтр/сортировка в памяти).
 * ImmutableList даёт Compose стабильность и убирает микрофризы при скролле.
 */
data class HomeUiState(
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RATING,
    val sortAscending: Boolean = false,
    val filterTags: ImmutableList<String> = persistentListOf(),
    val filterCategory: String = "",
    val isGenreFilterVisible: Boolean = false,
    val isCheckingUpdates: Boolean = false,
    val updates: ImmutableList<AnimeUpdate> = persistentListOf(),
    val selectedAnimeForDetails: Anime? = null,
    val statsAnimeList: ImmutableList<Anime> = persistentListOf(),
    val isRestoringFromCloud: Boolean = false
)

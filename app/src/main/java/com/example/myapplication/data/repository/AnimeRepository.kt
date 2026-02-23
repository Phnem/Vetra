package com.example.myapplication.data.repository

import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.SortOption
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.AnimeDetails
import com.example.myapplication.network.AppContentType
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.network.GithubReleaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Single source of truth for anime data. Network calls return Result;
 * no try/catch in ViewModel — use result.fold(onSuccess, onFailure).
 *
 * Список аниме — реактивный пайплайн: БД (asFlow) + фильтр/сортировка в памяти (до ~3–5k записей).
 */
class AnimeRepository(
    private val apiService: ApiService,
    private val localDataSource: AnimeLocalDataSource
) {

    /**
     * Реактивный поток списка: БД подписывается один раз, фильтрация/сортировка в памяти.
     * При изменении БД или параметров (поиск, сортировка, фильтр) пересчитывается только фильтрация.
     */
    fun observeAnimeList(
        searchQuery: Flow<String>,
        sortOption: Flow<SortOption>,
        sortAscending: Flow<Boolean>,
        filterTags: Flow<List<String>>
    ): Flow<List<Anime>> = combine(
        localDataSource.observeAllAnime(), // БД читается один раз при старте
        searchQuery,
        sortOption,
        sortAscending,
        filterTags
    ) { list, q, sort, asc, tags ->
        // Фильтрация/сортировка в памяти (~0.1 мс) при любом изменении параметров
        filterAndSortInMemory(list, q, sort, asc, tags)
    }
        .flowOn(Dispatchers.Default)

    private fun filterAndSortInMemory(
        list: List<Anime>,
        searchQuery: String,
        sortOption: SortOption,
        sortAscending: Boolean,
        filterTags: List<String>
    ): List<Anime> {
        var result = list
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            val lower = trimmed.lowercase()
            result = result.filter { it.title.lowercase().contains(lower) }
        }
        if (filterTags.isNotEmpty()) {
            result = result.filter { it.tags.containsAll(filterTags) }
        }
        result = when (sortOption) {
            SortOption.RATING -> if (sortAscending) result.sortedBy { it.rating } else result.sortedByDescending { it.rating }
            SortOption.EPISODES -> if (sortAscending) result.sortedBy { it.episodes } else result.sortedByDescending { it.episodes }
            SortOption.TITLE -> if (sortAscending) result.sortedBy { it.title } else result.sortedByDescending { it.title }
        }
        return result
    }

    fun getAnimeById(id: String): Anime? = localDataSource.getAnimeById(id)

    suspend fun fetchDetails(title: String, language: AppLanguage): Result<AnimeDetails?> {
        return apiService.fetchDetails(title, language)
    }

    suspend fun findTotalEpisodes(
        title: String,
        categoryType: String,
        appContentType: AppContentType
    ): Result<Pair<Int, String>?> {
        return apiService.findTotalEpisodes(title, categoryType, appContentType)
    }

    suspend fun checkGithubUpdate(): Result<GithubReleaseInfo?> {
        return apiService.checkGithubUpdate()
    }
}

package com.example.myapplication.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.repository.AnimeRepository
import com.example.myapplication.data.repository.ImageStorageRepository
import com.example.myapplication.network.AppContentType
import com.example.myapplication.data.models.SortOption
import com.example.myapplication.notifications.AnimeNotifier
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.work.*
import com.example.myapplication.worker.AnimeUpdateWorker

class HomeViewModel(
    private val repository: AnimeRepository,
    private val localDataSource: AnimeLocalDataSource,
    private val notifier: AnimeNotifier,
    private val dropboxSyncManager: DropboxSyncManager,
    private val imageStorage: ImageStorageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Реактивный список (ImmutableList для Zero Jank); при reconnectDatabase() переподписывается на новое подключение (hot swap).
     * WhileSubscribed(5000): данные живут в памяти 5 сек после отписки — при возврате с AddEdit список есть в кадре 0, Shared Transition не сбрасывается. */
    val animeListFlow: StateFlow<kotlinx.collections.immutable.ImmutableList<Anime>> = repository.observeAnimeList(
        searchQuery = _uiState.map { it.searchQuery },
        sortOption = _uiState.map { it.sortOption },
        sortAscending = _uiState.map { it.sortAscending },
        filterTags = _uiState.map { it.filterTags }
    ).map { it.toImmutableList() }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = persistentListOf()
    )

    private var ignoredUpdatesMap = mutableMapOf<String, Int>()
    private var hasCheckedForUpdatesThisSession = false

    init {
        viewModelScope.launch {
            ignoredUpdatesMap.putAll(localDataSource.getIgnoredMap())
            _uiState.update { it.copy(updates = localDataSource.getUpdates().toImmutableList()) }
            checkForUpdates()
        }
        viewModelScope.launch {
            try {
                dropboxSyncManager.syncState.collect { state ->
                    _uiState.update { it.copy(isRestoringFromCloud = state == SyncState.SYNCING) }
                }
            } catch (_: Exception) { }
        }
    }

    fun loadAnime() {}

    /** Hot swap: закрывает старый коннект, открывает новый (после .copyTo миграции), UI переподписывается. */
    fun refreshList() {
        viewModelScope.launch {
            _isRefreshing.value = true
            localDataSource.reconnectDatabase()
            delay(500)
            _isRefreshing.value = false
        }
    }

    fun scheduleBackgroundWork(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val updateRequest = PeriodicWorkRequestBuilder<AnimeUpdateWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AnimeUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            updateRequest
        )
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateSortOption(option: SortOption) {
        _uiState.update {
            if (it.sortOption == option) {
                it.copy(sortAscending = !it.sortAscending)
            } else {
                it.copy(sortOption = option, sortAscending = false)
            }
        }
    }

    fun toggleGenreFilter() {
        _uiState.update { it.copy(isGenreFilterVisible = !it.isGenreFilterVisible) }
    }

    fun setGenreFilterVisible(visible: Boolean) {
        _uiState.update { it.copy(isGenreFilterVisible = visible) }
    }

    fun updateFilterTags(tags: List<String>, category: String) {
        _uiState.update {
            it.copy(filterTags = tags.toImmutableList(), filterCategory = category)
        }
    }

    fun selectAnimeForDetails(anime: Anime) {
        _uiState.update { it.copy(selectedAnimeForDetails = anime) }
    }

    fun clearSelectedAnime() {
        _uiState.update { it.copy(selectedAnimeForDetails = null) }
    }

    fun deleteAnime(id: String) {
        viewModelScope.launch {
            runCatching {
                val anime = localDataSource.getAnimeById(id) ?: return@launch
                anime.imageFileName?.let { imageStorage.deleteImage(it) }
                localDataSource.deleteAnime(id)
            }.onFailure { it.printStackTrace() }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            runCatching {
                val anime = localDataSource.getAnimeById(id) ?: return@launch
                localDataSource.updateAnime(anime.copy(isFavorite = !anime.isFavorite))
            }.onFailure { it.printStackTrace() }
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        if (!force && hasCheckedForUpdatesThisSession) return
        if (_uiState.value.isCheckingUpdates) return
        hasCheckedForUpdatesThisSession = true
        _uiState.update { it.copy(isCheckingUpdates = true) }
        viewModelScope.launch {
            runCatching {
                val appContentType = AppContentType.ANIME
                val newUpdates = mutableListOf<AnimeUpdate>()
                localDataSource.getAllAnimeList().forEach { anime ->
                    val result = repository.findTotalEpisodes(anime.title, anime.categoryType, appContentType).getOrNull()
                    if (result != null) {
                        val (remoteEps, source) = result
                        val isIgnored = ignoredUpdatesMap[anime.id] == remoteEps
                        if (remoteEps > anime.episodes && !isIgnored) {
                            val newUpdate = AnimeUpdate(anime.id, anime.title, anime.episodes, remoteEps, source)
                            if (!newUpdates.contains(newUpdate)) newUpdates.add(newUpdate)
                        }
                    }
                }
                _uiState.update { it.copy(updates = newUpdates.toImmutableList(), isCheckingUpdates = false) }
                localDataSource.setUpdates(newUpdates)
                if (newUpdates.isNotEmpty()) {
                    newUpdates.forEach { update -> notifier.showUpdateNotification(update) }
                }
            }.onFailure {
                it.printStackTrace()
                _uiState.update { it.copy(isCheckingUpdates = false) }
            }
        }
    }

    fun acceptUpdate(update: AnimeUpdate, ctx: Context) {
        val anime = getAnimeById(update.animeId) ?: return
        viewModelScope.launch {
            localDataSource.updateAnime(anime.copy(episodes = update.newEpisodes))
            localDataSource.removeUpdate(update.animeId)
            _uiState.update { state ->
                state.copy(updates = state.updates.filter { it.animeId != update.animeId }.toImmutableList())
            }
        }
    }

    fun dismissUpdate(update: AnimeUpdate) {
        viewModelScope.launch {
            localDataSource.addIgnored(update.animeId, update.newEpisodes)
            ignoredUpdatesMap[update.animeId] = update.newEpisodes
            localDataSource.removeUpdate(update.animeId)
            _uiState.update { state ->
                state.copy(updates = state.updates.filter { it.animeId != update.animeId }.toImmutableList())
            }
        }
    }

    fun getAnimeById(id: String): Anime? {
        return localDataSource.getAnimeById(id)
    }

    fun getImgPath(name: String?): String? {
        if (name == null) return null
        return imageStorage.getImageFilePath(name)
    }

    fun loadStatsAnimeList() {
        viewModelScope.launch {
            _uiState.update { it.copy(statsAnimeList = localDataSource.getAllAnimeList().toImmutableList()) }
        }
    }
}

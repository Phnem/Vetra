package com.example.myapplication.ui.home

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.network.AppContentType
import com.example.myapplication.data.models.SortOption
import com.example.myapplication.data.repository.AnimeRepository
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
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.SyncState
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.work.*
import com.example.myapplication.worker.AnimeUpdateWorker

class HomeViewModel(
    private val repository: AnimeRepository,
    private val localDataSource: AnimeLocalDataSource
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

    private val ROOT = "Vetro"
    private val IMG_DIR = "collection"
    private var ignoredUpdatesMap = mutableMapOf<String, Int>()
    private var needsUpdateCheck = true

    init {
        viewModelScope.launch {
            ignoredUpdatesMap.putAll(localDataSource.getIgnoredMap())
            _uiState.update { it.copy(updates = localDataSource.getUpdates().toImmutableList()) }
            if (_uiState.value.updates.isEmpty()) {
                needsUpdateCheck = true
                checkForUpdates()
            }
        }
        viewModelScope.launch {
            try {
                DropboxSyncManager.syncState.collect { state ->
                    _uiState.update { it.copy(isRestoringFromCloud = state == SyncState.SYNCING) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun getRoot(): File {
        val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val f = File(d, ROOT)
        if (!f.exists()) f.mkdirs()
        return f
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
            try {
                val anime = localDataSource.getAnimeById(id) ?: return@launch
                val imgDir = File(getRoot(), IMG_DIR)
                anime.imageFileName?.let { File(imgDir, it).delete() }
                localDataSource.deleteAnime(id)
                needsUpdateCheck = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                val anime = localDataSource.getAnimeById(id) ?: return@launch
                localDataSource.updateAnime(anime.copy(isFavorite = !anime.isFavorite))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        if (!force && !needsUpdateCheck && _uiState.value.updates.isEmpty()) return
        if (_uiState.value.isCheckingUpdates) return
        _uiState.update { it.copy(isCheckingUpdates = true, updates = persistentListOf()) }
        viewModelScope.launch {
            try {
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
                if (newUpdates.isEmpty()) needsUpdateCheck = false
                localDataSource.setUpdates(newUpdates)
            } catch (e: Exception) {
                e.printStackTrace()
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
            if (_uiState.value.updates.isEmpty()) needsUpdateCheck = false
        }
    }

    fun getAnimeById(id: String): Anime? {
        return localDataSource.getAnimeById(id)
    }

    fun getImgPath(name: String?): File? {
        if (name == null) return null
        val imgDir = File(getRoot(), IMG_DIR)
        val file = File(imgDir, name)
        return if (file.exists()) file else null
    }

    fun loadStatsAnimeList() {
        viewModelScope.launch {
            _uiState.update { it.copy(statsAnimeList = localDataSource.getAllAnimeList().toImmutableList()) }
        }
    }
}

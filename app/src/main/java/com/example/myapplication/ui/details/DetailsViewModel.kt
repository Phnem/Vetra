package com.example.myapplication.ui.details

import android.content.Context
import android.os.Environment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.models.Anime
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.data.repository.AnimeRepository
import com.example.myapplication.ui.navigation.DetailsRoute
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File

class DetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val context: Context,
    private val repository: AnimeRepository,
    private val settingsDataStore: DataStore<Preferences>
) : ViewModel() {

    private val animeId: String = savedStateHandle.toRoute<DetailsRoute>().animeId

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Idle)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    // Инициализация синхронно: узел sharedBounds есть в дереве на 0-м кадре — Exit transition работает.
    private val _currentAnime = MutableStateFlow<Anime?>(repository.getAnimeById(animeId))
    val currentAnime: StateFlow<Anime?> = _currentAnime.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.EN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = settingsDataStore.data.first()
            val langKey = stringPreferencesKey("lang")
            _currentLanguage.value = AppLanguage.valueOf(prefs[langKey] ?: "EN")
            _currentAnime.value?.let { loadDetails(it, _currentLanguage.value) }
        }
    }

    fun getImgPath(name: String?): File? {
        if (name == null) return null
        val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Vetro")
        val imgDir = File(root, "collection")
        return File(imgDir, name)
    }

    private fun loadDetails(anime: Anime, language: AppLanguage) {
        viewModelScope.launch {
            _uiState.value = DetailsUiState.Loading
            val startTime = System.currentTimeMillis()

            repository.fetchDetails(anime.title, language)
                .fold(
                    onSuccess = { details ->
                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed < 500) delay(500 - elapsed)
                        _uiState.value = if (details != null) {
                            DetailsUiState.Success(details)
                        } else {
                            DetailsUiState.Error
                        }
                    },
                    onFailure = {
                        _uiState.value = DetailsUiState.Error
                    }
                )
        }
    }
}

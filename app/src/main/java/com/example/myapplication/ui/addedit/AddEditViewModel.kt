package com.example.myapplication.ui.addedit

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers

class AddEditViewModel(
    private val repository: AnimeRepository,
    private val localDataSource: AnimeLocalDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val ROOT = "Vetro"
    private val IMG_DIR = "collection"

    private fun getRoot(): File {
        val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val f = File(d, ROOT)
        if (!f.exists()) f.mkdirs()
        return f
    }
    
    private fun getImgDir(): File {
        val f = File(getRoot(), IMG_DIR)
        if (!f.exists()) f.mkdirs()
        return f
    }

    fun loadAnime(animeId: String?) {
        if (animeId == null) {
            _uiState.value = AddEditUiState()
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val anime = localDataSource.getAnimeById(animeId)
                anime?.let {
                    _uiState.update {
                        it.copy(
                            animeId = anime.id,
                            title = anime.title,
                            episodes = anime.episodes.toString(),
                            rating = anime.rating,
                            selectedTags = anime.tags,
                            categoryType = anime.categoryType,
                            isLoading = false
                        )
                    }
                } ?: run {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }
    
    fun updateEpisodes(episodes: String) {
        if (episodes.all { it.isDigit() }) {
            _uiState.update { it.copy(episodes = episodes) }
        }
    }
    
    fun updateRating(rating: Int) {
        _uiState.update { it.copy(rating = rating) }
    }
    
    fun updateImageUri(uri: Uri?) {
        _uiState.update { it.copy(imageUri = uri) }
    }
    
    fun updateTags(tags: List<String>, category: String) {
        _uiState.update { 
            it.copy(
                selectedTags = tags,
                categoryType = category
            )
        }
    }
    
    fun saveAnime(
        context: Context,
        onSuccess: () -> Unit
    ) {
        val state = _uiState.value
        if (!state.isValid) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val episodes = state.episodes.toIntOrNull() ?: 0
                val imageFileName = if (state.imageUri != null) {
                    saveImg(context, state.imageUri, state.animeId ?: UUID.randomUUID().toString())
                } else {
                    null
                }
                
                if (state.animeId == null) {
                    val newAnime = Anime(
                        id = UUID.randomUUID().toString(),
                        title = state.title,
                        episodes = episodes,
                        rating = state.rating,
                        imageFileName = imageFileName,
                        orderIndex = localDataSource.getMaxOrderIndex() + 1,
                        dateAdded = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = state.selectedTags,
                        categoryType = state.categoryType
                    )
                    localDataSource.insertAnime(newAnime)
                } else {
                    val anime = localDataSource.getAnimeById(state.animeId) ?: run {
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    val updatedAnime = anime.copy(
                        title = state.title,
                        episodes = episodes,
                        rating = state.rating,
                        imageFileName = imageFileName ?: anime.imageFileName,
                        tags = state.selectedTags,
                        categoryType = state.categoryType
                    )
                    localDataSource.updateAnime(updatedAnime)
                }
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
    
    private suspend fun saveImg(ctx: Context, uri: Uri, id: String): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val name = "img_${id}_${System.currentTimeMillis()}.jpg"
            ctx.contentResolver.openInputStream(uri)?.use { i -> 
                FileOutputStream(File(getImgDir(), name)).use { o -> 
                    i.copyTo(o) 
                } 
            }
            name
        } catch(e: Exception) {
            null
        }
    }
}

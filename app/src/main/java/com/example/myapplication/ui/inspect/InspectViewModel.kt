package com.example.myapplication.ui.inspect

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime
import com.example.myapplication.domain.inspect.InspectContentMode
import com.example.myapplication.domain.inspect.InspectGeminiRequiredException
import com.example.myapplication.domain.inspect.InspectGeminiRequirement
import com.example.myapplication.domain.inspect.InspectImageUseCase
import com.example.myapplication.domain.search.AddFromApiUseCase
import com.example.myapplication.network.ApiSearchResult
import com.example.myapplication.network.AppContentType
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.ui.home.ApiSearchUiModel
import io.ktor.http.ContentType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val KEY_LANG = stringPreferencesKey("lang")
private val KEY_CONTENT_TYPE = stringPreferencesKey("contentType")
private val SEARCH_NORMALIZE_REGEX = Regex("[^\\p{L}\\p{N}]")

private fun String.normalizeForSearch(): String =
    lowercase().replace(SEARCH_NORMALIZE_REGEX, "")

sealed interface InspectUiState {
    data object Idle : InspectUiState
    data class Loading(val message: String) : InspectUiState
    data class Success(val results: ImmutableList<ApiSearchUiModel>) : InspectUiState
    data class Error(val message: String) : InspectUiState
}

class InspectViewModel(
    private val inspectImageUseCase: InspectImageUseCase,
    private val localDataSource: AnimeLocalDataSource,
    private val addFromApiUseCase: AddFromApiUseCase,
    settingsDataStore: DataStore<Preferences>
) : ViewModel() {

    val uiLanguage: StateFlow<AppLanguage> = settingsDataStore.data
        .map { prefs ->
            runCatching { AppLanguage.valueOf(prefs[KEY_LANG] ?: "EN") }
                .getOrElse { AppLanguage.EN }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.EN)

    val contentMode = MutableStateFlow(InspectContentMode.Anime)

    private val _rawResults = MutableStateFlow<List<ApiSearchResult>>(emptyList())
    private val _loadingMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _addingFromApiId = MutableStateFlow<String?>(null)
    val addingFromApiId: StateFlow<String?> = _addingFromApiId

    val selectedImageUri = MutableStateFlow<Uri?>(null)

    val uiState: StateFlow<InspectUiState> = combine(
        _rawResults,
        _loadingMessage,
        _errorMessage,
        localDataSource.observeAllAnime()
    ) { raw, loading, err, localList ->
        when {
            loading != null -> InspectUiState.Loading(loading)
            err != null -> InspectUiState.Error(err)
            raw.isEmpty() -> InspectUiState.Idle
            else -> InspectUiState.Success(
                raw.map { r ->
                    ApiSearchUiModel(
                        result = r,
                        isAdded = isAddedInMemory(r, localList)
                    )
                }.toImmutableList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InspectUiState.Idle)

    init {
        viewModelScope.launch {
            val prefs = settingsDataStore.data.first()
            val type = runCatching { AppContentType.valueOf(prefs[KEY_CONTENT_TYPE] ?: "ANIME") }
                .getOrElse { AppContentType.ANIME }
            contentMode.value = when (type) {
                AppContentType.ANIME -> InspectContentMode.Anime
                AppContentType.MOVIE, AppContentType.SERIES -> InspectContentMode.MoviesSeries
            }
        }
    }

    fun setContentMode(mode: InspectContentMode) {
        contentMode.value = mode
        _rawResults.value = emptyList()
        _loadingMessage.value = null
        _errorMessage.value = null
    }

    fun analyzeImage(context: Context, uri: Uri) {
        selectedImageUri.value = uri
        viewModelScope.launch {
            _errorMessage.value = null
            _rawResults.value = emptyList()
            _loadingMessage.value = context.getString(com.example.myapplication.R.string.inspect_loading_analyzing)
            val lang = uiLanguage.value
            val outcome = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: error(context.getString(com.example.myapplication.R.string.inspect_read_image_failed))
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val traceCt = mimeToKtorContentType(mime)
                inspectImageUseCase(
                    imageBytes = bytes,
                    mimeTypeForTrace = traceCt,
                    mimeTypeForGemini = mime,
                    contentMode = contentMode.value,
                    appLanguage = lang
                ).getOrThrow()
            }
            _loadingMessage.value = null
            outcome.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _errorMessage.value = context.getString(com.example.myapplication.R.string.inspect_no_results)
                    } else {
                        _rawResults.value = list
                    }
                },
                onFailure = { e ->
                    _errorMessage.value = when (e) {
                        is InspectGeminiRequiredException -> when (e.requirement) {
                            InspectGeminiRequirement.RU_ANIME_PATH ->
                                context.getString(com.example.myapplication.R.string.inspect_gemini_required_ru_anime)
                            InspectGeminiRequirement.MOVIES_TV ->
                                context.getString(com.example.myapplication.R.string.inspect_gemini_required_movies)
                        }
                        else -> e.message
                            ?: context.getString(com.example.myapplication.R.string.inspect_error_generic)
                    }
                }
            )
        }
    }

    fun addFromApi(result: ApiSearchResult) {
        val key = "${result.source}_${result.externalId ?: result.title}"
        viewModelScope.launch {
            _addingFromApiId.value = key
            addFromApiUseCase(result).onFailure { it.printStackTrace() }
            _addingFromApiId.value = null
        }
    }

    fun clearResults() {
        _rawResults.value = emptyList()
        _errorMessage.value = null
    }

    fun clearPreviewAndResults() {
        selectedImageUri.value = null
        clearResults()
    }

    private fun isAddedInMemory(result: ApiSearchResult, localList: List<Anime>): Boolean {
        val q = result.title.normalizeForSearch()
        if (q.isEmpty()) return false
        return localList.any { anime ->
            val t = anime.title.normalizeForSearch()
            t.isNotEmpty() && (t.contains(q) || q.contains(t))
        }
    }
}

private fun mimeToKtorContentType(mime: String): ContentType = when {
    mime.contains("png", ignoreCase = true) -> ContentType.Image.PNG
    mime.contains("webp", ignoreCase = true) -> ContentType("image", "webp")
    mime.contains("gif", ignoreCase = true) -> ContentType.parse("image/gif")
    else -> ContentType.Image.JPEG
}

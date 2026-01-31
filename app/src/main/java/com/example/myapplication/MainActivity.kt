package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

// ==========================================
// GENRE SYSTEM (LOCALIZATION & DATA)
// ==========================================

enum class GenreCategory { ANIME, MOVIE, SERIES }

data class GenreDefinition(
    val id: String,
    val ru: String,
    val en: String,
    val categories: List<GenreCategory>
)

object GenreRepository {
    val allGenres = listOf(
        GenreDefinition("Isekai", "Исекай", "Isekai", listOf(GenreCategory.ANIME)),
        GenreDefinition("Games", "Игры", "Games", listOf(GenreCategory.ANIME)),
        GenreDefinition("Martial Arts", "Боевые искусства", "Martial Arts", listOf(GenreCategory.ANIME)),
        GenreDefinition("Magic", "Магия", "Magic", listOf(GenreCategory.ANIME)),
        GenreDefinition("Mecha", "Мехи", "Mecha", listOf(GenreCategory.ANIME)),
        GenreDefinition("Shounen", "Сёнен", "Shounen", listOf(GenreCategory.ANIME)),
        GenreDefinition("Shoujo", "Сёдзё", "Shoujo", listOf(GenreCategory.ANIME)),
        GenreDefinition("Seinen", "Сэйнен", "Seinen", listOf(GenreCategory.ANIME)),
        GenreDefinition("Slice of Life", "Повседневность", "Slice of Life", listOf(GenreCategory.ANIME)),
        GenreDefinition("Ecchi", "Этти", "Ecchi", listOf(GenreCategory.ANIME)),
        GenreDefinition("Hentai", "Хентай", "Hentai", listOf(GenreCategory.ANIME)),
        GenreDefinition("School", "Школа", "School", listOf(GenreCategory.ANIME)),
        GenreDefinition("Detective", "Детектив", "Detective", listOf(GenreCategory.ANIME, GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Sports", "Спорт", "Sports", listOf(GenreCategory.ANIME, GenreCategory.MOVIE)),
        GenreDefinition("Action", "Экшен", "Action", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Adventure", "Приключения", "Adventure", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Comedy", "Комедия", "Comedy", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Drama", "Драма", "Drama", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Fantasy", "Фэнтези", "Fantasy", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Romance", "Романтика", "Romance", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Horror", "Ужасы", "Horror", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Sci-Fi", "Фантастика", "Sci-Fi", listOf(GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Thriller", "Триллер", "Thriller", listOf(GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Mystery", "Мистика", "Mystery", listOf(GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Crime", "Криминал", "Crime", listOf(GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Western", "Вестерн", "Western", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Animation", "Анимация", "Animation", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Documentary", "Документальный", "Documentary", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Sitcom", "Ситком", "Sitcom", listOf(GenreCategory.SERIES)),
        GenreDefinition("Reality", "Реалити-шоу", "Reality", listOf(GenreCategory.SERIES))
    )

    fun getGenresForCategory(category: GenreCategory): List<GenreDefinition> {
        return allGenres.filter { it.categories.contains(category) }.sortedBy { it.id }
    }

    fun getLabel(id: String, lang: AppLanguage): String {
        val def = allGenres.find { it.id.equals(id, ignoreCase = true) }
        return when (lang) {
            AppLanguage.RU -> def?.ru ?: id
            AppLanguage.EN -> def?.en ?: id
        }
    }
}


// ==========================================
// LOCALIZATION & THEME ENUMS
// ==========================================

enum class AppLanguage { RU, EN }
enum class AppTheme { LIGHT, DARK, SYSTEM }

data class UiStrings(
    val appName: String,
    val searchPlaceholder: String,
    val newestFirst: String,
    val highestRated: String,
    val nameAZ: String,
    val favorites: String,
    val settings: String,
    val emptyTitle: String,
    val emptySubtitle: String,
    val noResults: String,
    val noFavorites: String,
    val statsTitle: String,
    val statsSubtitle: String,
    val episodesWatched: String,
    val timeSpent: String,
    val avgRating: String,
    val rankTitle: String,
    val updatesTitle: String,
    val updatesChecking: String,
    val updatesUpToDate: String,
    val deleteTitle: String,
    val deleteSubtitle: String,
    val deleteConfirm: String,
    val favTitle: String,
    val favSubtitle: String,
    val favConfirm: String,
    val cancel: String,
    val addTitle: String,
    val editTitle: String,
    val animeTitleHint: String,
    val episodesHint: String,
    val addPhoto: String,
    val enterTitleToast: String,
    val updatedToast: String,
    val languageName: String,
    val settingsScreenTitle: String,
    val languageCardTitle: String,
    val langRu: String,
    val langEn: String,
    val themeTitle: String,
    val themeLight: String,
    val themeDark: String,
    val themeSystem: String,
    val checkForUpdateTitle: String,
    val checkButtonText: String,
    val contactTitle: String,
    val contactSubtitle: String,
    val genreAnime: String,
    val genreMovies: String,
    val genreSeries: String,
    val filterByGenre: String
)

val RussianStrings = UiStrings(
    appName = "MAList", searchPlaceholder = "Поиск в коллекции...", newestFirst = "Сначала новые", highestRated = "Высокий рейтинг", nameAZ = "По названию (А-Я)", favorites = "Избранное", settings = "Настройки", emptyTitle = "В папке пусто", emptySubtitle = "Кажется, здесь еще ничего нет.", noResults = "Ничего не найдено", noFavorites = "Нет избранного", statsTitle = "Ваша статистика", statsSubtitle = "Все, что вы посмотрели", episodesWatched = "Эпизодов просмотрено", timeSpent = "Затраченное время", avgRating = "Средний рейтинг", rankTitle = "Ваш ранг:", updatesTitle = "Обновления", updatesChecking = "Проверка API (Shikimori, Jikan, TMDB)...\nЭто может занять некоторое время.", updatesUpToDate = "У вас все актуально!", deleteTitle = "Удалить тайтл?", deleteSubtitle = "Это действие нельзя будет отменить.", deleteConfirm = "Удалить", favTitle = "Добавить в избранное?", favSubtitle = "Будущий вы скажет вам «спасибо» :)", favConfirm = "В избранное", cancel = "Отмена", addTitle = "Добавить тайтл", editTitle = "Изменить", animeTitleHint = "Название аниме", episodesHint = "Просмотрено серий", addPhoto = "Добавить фото", enterTitleToast = "Введите название", updatedToast = "Обновлено: ", languageName = "RU", settingsScreenTitle = "Настройки", languageCardTitle = "Язык", langRu = "Русский", langEn = "English", themeTitle = "Тема оформления", themeLight = "Светлая", themeDark = "Тёмная", themeSystem = "Системная", checkForUpdateTitle = "Обновление приложения", checkButtonText = "Проверить версию", contactTitle = "Связь со мной", contactSubtitle = "Нашли баг или есть идея?", genreAnime = "Аниме", genreMovies = "Фильмы", genreSeries = "Сериалы", filterByGenre = "По жанрам"
)

val EnglishStrings = UiStrings(
    appName = "MAList", searchPlaceholder = "Search collection...", newestFirst = "Newest First", highestRated = "Highest Rated", nameAZ = "Name (A-Z)", favorites = "Favorites", settings = "Settings", emptyTitle = "Nothing in folder", emptySubtitle = "Looks empty over here.", noResults = "No results found", noFavorites = "No favorites yet", statsTitle = "Your Watch Stats", statsSubtitle = "Everything you’ve watched so far", episodesWatched = "Episodes watched", timeSpent = "Time spent watching", avgRating = "Average rating", rankTitle = "Your rank:", updatesTitle = "Updates", updatesChecking = "Checking APIs (Shikimori, Jikan, TMDB)...\nThis will take a moment.", updatesUpToDate = "You are up to date!", deleteTitle = "Delete title?", deleteSubtitle = "There’s no “undo”, no “Ctrl+Z”...", deleteConfirm = "Delete", favTitle = "Add to favorites?", favSubtitle = "Future you will thank you)", favConfirm = "Set Favorite", cancel = "Cancel", addTitle = "Add title", editTitle = "Edit title", animeTitleHint = "Anime Title", episodesHint = "Episodes Watched", addPhoto = "Add Photo", enterTitleToast = "Enter title", updatedToast = "Updated: ", languageName = "EN", settingsScreenTitle = "Settings", languageCardTitle = "Language", langRu = "Russian", langEn = "English", themeTitle = "Themes", themeLight = "Light", themeDark = "Dark", themeSystem = "System", checkForUpdateTitle = "App Update", checkButtonText = "Check Version", contactTitle = "Contact me", contactSubtitle = "Found a bug or have an idea?", genreAnime = "Anime", genreMovies = "Movies", genreSeries = "TV Series", filterByGenre = "By genres"
)

fun getStrings(lang: AppLanguage): UiStrings = when(lang) {
    AppLanguage.RU -> RussianStrings
    AppLanguage.EN -> EnglishStrings
}

// ==========================================
// OVERSCROLL UTILS
// ==========================================

val CustomEasing: Easing = CubicBezierEasing(0.5f, 0.5f, 1.0f, 0.25f)

@Composable
fun Modifier.customOverscroll(
    listState: LazyListState,
    onNewOverscrollAmount: (Float) -> Unit,
    animationSpec: SpringSpec<Float> = spring(stiffness = Spring.StiffnessLow)
) = customOverscroll(
    orientation = remember { listState.layoutInfo.orientation },
    onNewOverscrollAmount = onNewOverscrollAmount,
    animationSpec = animationSpec
)

@Composable
fun Modifier.customOverscroll(
    orientation: Orientation,
    onNewOverscrollAmount: (Float) -> Unit,
    animationSpec: SpringSpec<Float> = spring(stiffness = Spring.StiffnessLow)
): Modifier {
    val overscrollAmountAnimatable = remember { Animatable(0f) }
    var length by remember { mutableFloatStateOf(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        snapshotFlow { overscrollAmountAnimatable.value }.collect {
            onNewOverscrollAmount(
                CustomEasing.transform(it / (length * 1.5f)) * length
            )
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private fun calculateOverscroll(available: Offset): Float {
                val previous = overscrollAmountAnimatable.value
                val newValue = previous + when (orientation) {
                    Orientation.Vertical -> available.y
                    Orientation.Horizontal -> available.x
                }
                return when {
                    previous > 0 -> newValue.coerceAtLeast(0f)
                    previous < 0 -> newValue.coerceAtMost(0f)
                    else -> newValue
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollAmountAnimatable.value != 0f && source != NestedScrollSource.SideEffect) {
                    scope.launch { overscrollAmountAnimatable.snapTo(calculateOverscroll(available)) }
                    return available
                }
                return super.onPreScroll(available, source)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scope.launch { overscrollAmountAnimatable.snapTo(calculateOverscroll(available)) }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val availableVelocity = when (orientation) {
                    Orientation.Vertical -> available.y
                    Orientation.Horizontal -> available.x
                }
                if (overscrollAmountAnimatable.value != 0f && availableVelocity != 0f) {
                    val previousSign = overscrollAmountAnimatable.value.sign
                    var consumedVelocity = availableVelocity
                    val predictedEndValue = exponentialDecay<Float>().calculateTargetValue(initialValue = overscrollAmountAnimatable.value, initialVelocity = availableVelocity)
                    if (predictedEndValue.sign == previousSign) {
                        overscrollAmountAnimatable.animateTo(targetValue = 0f, initialVelocity = availableVelocity, animationSpec = animationSpec)
                    } else {
                        try {
                            overscrollAmountAnimatable.animateDecay(initialVelocity = availableVelocity, animationSpec = exponentialDecay()) {
                                if (value.sign != previousSign) { consumedVelocity -= velocity; scope.launch { overscrollAmountAnimatable.snapTo(0f) } }
                            }
                        } catch (e: Exception) { /* ignore interruption */ }
                    }
                    return when (orientation) {
                        Orientation.Vertical -> Velocity(0f, consumedVelocity)
                        Orientation.Horizontal -> Velocity(consumedVelocity, 0f)
                    }
                }
                return super.onPreFling(available)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val availableVelocity = when (orientation) {
                    Orientation.Vertical -> available.y
                    Orientation.Horizontal -> available.x
                }
                overscrollAmountAnimatable.animateTo(targetValue = 0f, initialVelocity = availableVelocity, animationSpec = animationSpec)
                return available
            }
        }
    }

    return this.onSizeChanged { length = when (orientation) { Orientation.Vertical -> it.height.toFloat(); Orientation.Horizontal -> it.width.toFloat() } }.nestedScroll(nestedScrollConnection)
}

// ==========================================
// NETWORK & DATA LAYER
// ==========================================

data class AnimeUpdate(val animeId: String, val title: String, val currentEpisodes: Int, val newEpisodes: Int, val source: String)
data class GithubReleaseInfo(val tagName: String, val htmlUrl: String, val downloadUrl: String)

// --- DETAILS DATA MODEL ---
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
    val rating: Double?,
    val posterUrl: String?,
    val source: String
)

sealed class DetailsState {
    object Idle : DetailsState()
    object Loading : DetailsState()
    data class Success(val data: AnimeDetails) : DetailsState()
    object Error : DetailsState()
}

object ApiRateLimiter {
    private val mutex = Mutex()
    suspend fun <T> executeSafe(block: suspend () -> T): T {
        mutex.withLock { delay(1200); return block() }
    }
}

class AnimeRepository {
    private val tmdbKey = "4f4dc3cd35d58a551162eefe92ff549c"

    // --- DETAILS FETCHING LOGIC ---

    suspend fun fetchDetails(title: String, lang: AppLanguage): AnimeDetails? = withContext(Dispatchers.IO) {
        return@withContext when (lang) {
            AppLanguage.RU -> fetchDetailsRu(title)
            AppLanguage.EN -> fetchDetailsEn(title)
        }
    }

    // RU STRATEGY: Shikimori Primary
    private suspend fun fetchDetailsRu(query: String): AnimeDetails? {
        try {
            // 1. Search to get ID
            val searchUrl = "https://shikimori.one/api/animes?search=${enc(query)}&limit=1"
            val searchJson = getJson(searchUrl).asJsonArray
            if (searchJson.size() == 0) return null

            val id = searchJson[0].asJsonObject.get("id").asInt

            // 2. Get Full Details
            val detailsUrl = "https://shikimori.one/api/animes/$id"
            val json = getJson(detailsUrl).asJsonObject

            // 3. Map to Unified Model
            val totalEps = if (json.get("episodes").isJsonNull) 0 else json.get("episodes").asInt
            val airedEps = if (json.get("episodes_aired").isJsonNull) 0 else json.get("episodes_aired").asInt
            val status = json.get("status").asString.replace("_", " ").capitalize()
            val score = if (json.get("score").isJsonNull) 0.0 else json.get("score").asDouble

            // Description processing
            var desc = if (json.get("description").isJsonNull) "Описание отсутствует." else json.get("description").asString
            desc = desc.replace(Regex("\\[.*?\\]"), "") // Basic cleanup of Shikimori tags

            val genres = mutableListOf<String>()
            if (json.has("genres")) {
                json.getAsJsonArray("genres").forEach { genres.add(it.asJsonObject.get("russian").asString) }
            }

            val poster = if (json.has("image") && !json.get("image").isJsonNull) {
                "https://shikimori.one" + json.get("image").asJsonObject.get("original").asString
            } else null

            return AnimeDetails(
                title = json.get("russian").asString.ifEmpty { json.get("name").asString },
                altTitle = json.get("name").asString,
                description = desc,
                type = json.get("kind").asString.uppercase(),
                status = status,
                episodesAired = airedEps,
                episodesTotal = if (totalEps > 0) totalEps else null,
                nextEpisode = null,
                genres = genres,
                rating = score,
                posterUrl = poster,
                source = "Shikimori"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // EN STRATEGY: AniList GraphQL
    private suspend fun fetchDetailsEn(query: String): AnimeDetails? {
        try {
            val url = URL("https://graphql.anilist.co")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true

            // ВАЖНО:
            // 1. Мы используем ${'$'}q, чтобы Kotlin вставил просто символ $ перед q.
            // 2. Мы убрали обратные слэши.
            // 3. $query (в variables) — это обычная Kotlin-переменная из аргументов функции, она подставится сама.
            val gql = """
            {
              "query": "query (${'$'}q: String) { Media (search: ${'$'}q, type: ANIME) { title { romaji english } description status episodes nextAiringEpisode { episode timeUntilAiring } genres averageScore coverImage { large } format } }",
              "variables": { "q": "$query" }
            }
            """.trimIndent()

            conn.outputStream.write(gql.toByteArray())

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JsonParser.parseString(resp).asJsonObject
                if (!json.has("data") || json.get("data").isJsonNull) return null
                val data = json.getAsJsonObject("data").getAsJsonObject("Media") ?: return null

                val titleObj = data.getAsJsonObject("title")
                val titleEn = if (titleObj.get("english").isJsonNull) titleObj.get("romaji").asString else titleObj.get("english").asString

                var desc = if (data.get("description").isJsonNull) "No description." else data.get("description").asString
                desc = desc.replace("<br>", "\n").replace(Regex("<.*?>"), "") // Remove HTML

                val epsTotal = if (data.get("episodes").isJsonNull) null else data.get("episodes").asInt
                val status = data.get("status").asString.replace("_", " ").capitalize()
                val score = if (data.get("averageScore").isJsonNull) 0.0 else (data.get("averageScore").asInt / 10.0)

                // Next Ep Logic
                var nextEpStr: String? = null
                if (data.has("nextAiringEpisode") && !data.get("nextAiringEpisode").isJsonNull) {
                    val nextObj = data.getAsJsonObject("nextAiringEpisode")
                    val epNum = nextObj.get("episode").asInt
                    val seconds = nextObj.get("timeUntilAiring").asInt
                    val days = seconds / 86400
                    nextEpStr = "Ep $epNum in $days days"
                }

                // Calc aired
                val aired = if (nextEpStr != null && !data.get("nextAiringEpisode").isJsonNull) {
                    data.getAsJsonObject("nextAiringEpisode").get("episode").asInt - 1
                } else {
                    epsTotal ?: 0
                }

                val genres = mutableListOf<String>()
                data.getAsJsonArray("genres").forEach { genres.add(it.asString) }

                return AnimeDetails(
                    title = titleEn,
                    altTitle = titleObj.get("romaji").asString,
                    description = desc,
                    type = data.get("format").asString,
                    status = status,
                    episodesAired = aired,
                    episodesTotal = epsTotal,
                    nextEpisode = nextEpStr,
                    genres = genres,
                    rating = score,
                    posterUrl = data.getAsJsonObject("coverImage").get("large").asString,
                    source = "AniList"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // --- EXISTING UPDATE CHECKER ---
    suspend fun findTotalEpisodes(title: String): Pair<Int, String>? = withContext(Dispatchers.IO) {
        val shikiResult = checkShikimori(title)
        if (shikiResult != null && shikiResult > 0) return@withContext shikiResult to "Shikimori"
        val jikanResult = checkJikan(title)
        if (jikanResult != null && jikanResult > 0) return@withContext jikanResult to "Jikan"
        val tmdbResult = checkTmdb(title)
        if (tmdbResult != null && tmdbResult > 0) return@withContext tmdbResult to "TMDB"
        return@withContext null
    }

    private suspend fun checkShikimori(query: String): Int? {
        return try {
            ApiRateLimiter.executeSafe {
                val searchUrl = "https://shikimori.one/api/animes?search=${enc(query)}&limit=1"
                val json = getJson(searchUrl).asJsonArray
                if (json.size() == 0) return@executeSafe null
                json[0].asJsonObject.get("episodes").let { if(it.isJsonNull) 0 else it.asInt }
            }
        } catch (e: Exception) { null }
    }

    private suspend fun checkJikan(query: String): Int? {
        return try {
            ApiRateLimiter.executeSafe {
                val searchUrl = "https://api.jikan.moe/v4/anime?q=${enc(query)}&limit=1"
                val resp = getJson(searchUrl).asJsonObject
                val data = resp.getAsJsonArray("data")
                if (data == null || data.size() == 0) return@executeSafe null
                val anime = data[0].asJsonObject
                if (anime.get("episodes").isJsonNull) 0 else anime.get("episodes").asInt
            }
        } catch (e: Exception) { null }
    }

    private suspend fun checkTmdb(query: String): Int? {
        return try {
            ApiRateLimiter.executeSafe {
                val searchUrl = "https://api.themoviedb.org/3/search/tv?api_key=$tmdbKey&query=${enc(query)}"
                val results = getJson(searchUrl).asJsonObject.getAsJsonArray("results")
                if (results != null && results.size() > 0) {
                    val id = results[0].asJsonObject.get("id").asInt
                    val detailsUrl = "https://api.themoviedb.org/3/tv/$id?api_key=$tmdbKey"
                    val details = getJson(detailsUrl).asJsonObject
                    val seasons = details.getAsJsonArray("seasons")
                    var total = 0
                    seasons.forEach { s ->
                        val seasonObj = s.asJsonObject
                        val sNum = seasonObj.get("season_number").asInt
                        if (sNum > 0) total += seasonObj.get("episode_count").asInt
                    }
                    return@executeSafe total
                }
                null
            }
        } catch (e: Exception) { null }
    }

    suspend fun checkGithubUpdate(): GithubReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/Phnem/MAList/releases/latest"
            val json = getJson(url).asJsonObject
            val tag = json.get("tag_name").asString
            val htmlUrl = json.get("html_url").asString
            var downloadUrl = htmlUrl
            val assets = json.getAsJsonArray("assets")
            if (assets != null && assets.size() > 0) {
                for (i in 0 until assets.size()) {
                    val asset = assets[i].asJsonObject
                    val name = asset.get("name").asString
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.get("browser_download_url").asString
                        break
                    }
                }
            }
            GithubReleaseInfo(tag, htmlUrl, downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getJson(urlString: String): com.google.gson.JsonElement {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "MAList-App-Updater")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (conn.responseCode in 200..299) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            return JsonParser.parseString(jsonStr)
        } else {
            throw Exception("HTTP ${conn.responseCode}")
        }
    }

    private fun postJson(urlString: String, jsonBody: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.outputStream.write(jsonBody.toByteArray())
        if (conn.responseCode in 200..299) {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            throw Exception("HTTP ${conn.responseCode}")
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

// ==========================================
// THEME & COLORS
// ==========================================

val BrandBlue = Color(0xFF3E82F7)
val BrandBlueSoft = Color(0xFF5E92F3)
val BrandRed = Color(0xFFFF453A)
val RateColor1 = Color(0xFFFF453A)
val RateColor2 = Color(0xFFFF9F0A)
val RateColor3 = Color(0xFFFFD60A)
val RateColor4 = Color(0xFFAEEA00)
val RateColor5 = Color(0xFF2E7D32)
val RateColorEmpty = Color(0xFF8E8E93)
val EpisodesColor = Color(0xFF0A84FF)
val TimeColor = Color(0xFF5E5CE6)
val RatingColor = Color(0xFFFFC400)
val RankColor = Color(0xFF43A047)
val DarkBackground = Color(0xFF111318)
val DarkSurface = Color(0xFF1F222B)
val DarkSurfaceVariant = Color(0xFF262A35)
val DarkTextPrimary = Color(0xFFF2F2F7)
val DarkTextSecondary = Color(0xFF9898A0)
val DarkBorder = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val LightBackground = Color(0xFFF0F2F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF9F9FB)
val LightTextPrimary = Color(0xFF1C1C1E)
val LightTextSecondary = Color(0xFF8E8E93)
val LightBorder = Color(0xFF000000).copy(alpha = 0.04f)

fun getRatingColor(rating: Int): Color {
    return when (rating) {
        1 -> RateColor1
        2 -> RateColor2
        3 -> RateColor3
        4 -> RateColor4
        5 -> RateColor5
        else -> Color.Gray
    }
}

val SnProFamily = FontFamily(
    Font(R.font.snpro_bold, FontWeight.Bold),
    Font(R.font.snpro_mediumitalic, FontWeight.Normal),
    Font(R.font.snpro_mediumitalic, FontWeight.Medium),
    Font(R.font.snpro_lightitalic, FontWeight.Light)
)

@Composable
fun OneUiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorScheme(background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant, primary = BrandBlueSoft, onBackground = DarkTextPrimary, onSurface = DarkTextPrimary, secondary = DarkTextSecondary, outline = DarkBorder, error = BrandRed, surfaceContainer = Color(0xFF2C2C2E))
    } else {
        lightColorScheme(background = LightBackground, surface = LightSurface, surfaceVariant = LightSurfaceVariant, primary = BrandBlue, onBackground = LightTextPrimary, onSurface = LightTextPrimary, secondary = LightTextSecondary, outline = LightBorder, error = BrandRed, surfaceContainer = Color(0xFFFFFFFF))
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as ComponentActivity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val wic = androidx.core.view.WindowCompat.getInsetsController(window, view)
            wic.isAppearanceLightStatusBars = !darkTheme
            wic.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = colors, typography = Typography(headlineMedium = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = colors.onBackground, letterSpacing = (-0.5).sp), headlineSmall = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.onBackground), titleMedium = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = 0.2.sp), titleLarge = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp), bodyMedium = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, color = colors.secondary, letterSpacing = 0.1.sp), bodyLarge = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp), bodySmall = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Light, fontSize = 12.sp, color = colors.secondary), labelLarge = TextStyle(fontFamily = SnProFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)), content = content)
}

// ==========================================
// DATA & VIEWMODEL
// ==========================================

data class Anime(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val episodes: Int,
    val rating: Int,
    val imageFileName: String?,
    val orderIndex: Int,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val categoryType: String = ""
)

data class RankedAnime(val anime: Anime, val score: Int)
enum class SortOption { DATE_NEWEST, RATING_HIGH, AZ, FAVORITES; fun getLabel(strings: UiStrings): String = when(this) { DATE_NEWEST -> strings.newestFirst; RATING_HIGH -> strings.highestRated; AZ -> strings.nameAZ; FAVORITES -> strings.favorites } }
enum class AppUpdateStatus { IDLE, LOADING, NO_UPDATE, UPDATE_AVAILABLE, ERROR }
data class SemanticVersion(val major: Int, val minor: Int, val patch: Int, val suffix: String) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch
        if (suffix.isEmpty() && other.suffix.isNotEmpty()) return 1
        if (suffix.isNotEmpty() && other.suffix.isEmpty()) return -1
        if (suffix.isEmpty() && other.suffix.isEmpty()) return 0
        return suffix.compareTo(other.suffix, ignoreCase = true)
    }
}

class AnimeViewModel : ViewModel() {
    var currentLanguage by mutableStateOf(AppLanguage.EN); private set
    var currentTheme by mutableStateOf(AppTheme.SYSTEM); private set
    val strings: UiStrings get() = getStrings(currentLanguage)
    var updateStatus by mutableStateOf(AppUpdateStatus.IDLE); private set
    var latestDownloadUrl by mutableStateOf<String?>(null); private set
    var currentVersionName by mutableStateOf("v1.0.0"); private set
    var userAvatarPath by mutableStateOf<String?>(null); private set
    var filterSelectedTags by mutableStateOf<List<String>>(emptyList())
    var filterCategoryType by mutableStateOf("")
    var isGenreFilterVisible by mutableStateOf(false)

    // --- DETAILS STATE ---
    var detailsState by mutableStateOf<DetailsState>(DetailsState.Idle); private set
    var selectedAnimeForDetails by mutableStateOf<Anime?>(null)

    private val SETTINGS_FILE = "settings.json"
    private val USER_AVATAR_FILE = "user_avatar.jpg"

    fun loadAnimeDetails(anime: Anime) {
        selectedAnimeForDetails = anime
        detailsState = DetailsState.Loading
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val details = repository.fetchDetails(anime.title, currentLanguage)
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 500) delay(500 - elapsed)
            detailsState = if (details != null) DetailsState.Success(details) else DetailsState.Error
        }
    }

    fun closeDetails() {
        selectedAnimeForDetails = null
        detailsState = DetailsState.Idle
    }

    private fun getSettingsFile(): File = File(getRoot(), SETTINGS_FILE)
    fun getGenreName(genreId: String): String = GenreRepository.getLabel(genreId, currentLanguage)

    fun setLanguage(lang: AppLanguage) { currentLanguage = lang; saveSettings() }
    fun setAppTheme(theme: AppTheme) { currentTheme = theme; saveSettings() }

    fun saveUserAvatar(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(getImgDir(), USER_AVATAR_FILE)
                context.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(destFile).use { output -> input.copyTo(output) } }
                withContext(Dispatchers.Main) { userAvatarPath = destFile.absolutePath }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadUserAvatar() { val f = File(getImgDir(), USER_AVATAR_FILE); if (f.exists()) userAvatarPath = f.absolutePath }

    fun initAppVersion(context: Context) {
        try { val pInfo = context.packageManager.getPackageInfo(context.packageName, 0); currentVersionName = pInfo.versionName ?: "v1.0.0" } catch (e: Exception) { currentVersionName = "v1.0.0" }
    }

    fun checkAppUpdate(context: Context) {
        if (updateStatus == AppUpdateStatus.LOADING) return
        updateStatus = AppUpdateStatus.LOADING
        if (currentVersionName == "v1.0.0") initAppVersion(context)
        val localVer = currentVersionName
        viewModelScope.launch {
            try {
                val release = repository.checkGithubUpdate()
                if (release != null) {
                    if (isNewerVersion(localVer, release.tagName)) { latestDownloadUrl = release.downloadUrl; updateStatus = AppUpdateStatus.UPDATE_AVAILABLE } else { updateStatus = AppUpdateStatus.NO_UPDATE }
                } else { updateStatus = AppUpdateStatus.ERROR; delay(2000); updateStatus = AppUpdateStatus.IDLE }
            } catch (e: Exception) { e.printStackTrace(); updateStatus = AppUpdateStatus.ERROR; delay(2000); updateStatus = AppUpdateStatus.IDLE }
        }
    }

    private fun isNewerVersion(local: String, remote: String): Boolean {
        try { val localSem = parseVersion(local); val remoteSem = parseVersion(remote); return remoteSem > localSem } catch (e: Exception) { return false }
    }

    private fun parseVersion(versionStr: String): SemanticVersion {
        val clean = versionStr.removePrefix("v").trim()
        val dashSplit = clean.split("-", limit = 2)
        val dots = dashSplit[0].split(".").map { it.toIntOrNull() ?: 0 }
        return SemanticVersion(dots.getOrElse(0) { 0 }, dots.getOrElse(1) { 0 }, dots.getOrElse(2) { 0 }, if (dashSplit.size > 1) dashSplit[1] else "")
    }

    private fun saveSettings() { try { getSettingsFile().writeText(Gson().toJson(mapOf("lang" to currentLanguage.name, "theme" to currentTheme.name))) } catch(e: Exception) { e.printStackTrace() } }
    fun loadSettings() {
        val f = getSettingsFile()
        if (f.exists()) {
            try {
                val map: Map<String, String> = Gson().fromJson(f.readText(), object : TypeToken<Map<String, String>>() {}.type)
                currentLanguage = AppLanguage.valueOf(map["lang"] ?: "EN")
                currentTheme = try { AppTheme.valueOf(map["theme"] ?: "SYSTEM") } catch (e: Exception) { AppTheme.SYSTEM }
            } catch(e: Exception) { e.printStackTrace() }
        }
    }

    private val _animeList = mutableStateListOf<Anime>()
    val animeList: List<Anime> get() = _animeList
    var searchQuery by mutableStateOf("")
    var sortOption by mutableStateOf(SortOption.DATE_NEWEST)
    private val _updates = mutableStateListOf<AnimeUpdate>()
    val updates: List<AnimeUpdate> get() = _updates
    var isCheckingUpdates by mutableStateOf(false)
    var needsUpdateCheck by mutableStateOf(true)
    private val repository = AnimeRepository()
    private val ROOT = "MyAnimeList"
    private val IMG_DIR = "collection"
    private val FILE_NAME = "list.json"
    private val IGNORED_FILE_NAME = "ignored.json"
    private var ignoredUpdatesMap = mutableMapOf<String, Int>()

    private fun getRoot(): File { val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS); val f = File(d, ROOT); if (!f.exists()) f.mkdirs(); return f }
    private fun getImgDir(): File { val f = File(getRoot(), IMG_DIR); if(!f.exists()) f.mkdirs(); return f }
    private fun getDataFile(): File = File(getRoot(), FILE_NAME)
    private fun getIgnoredFile(): File = File(getRoot(), IGNORED_FILE_NAME)

    fun loadAnime() {
        val f = getDataFile()
        if (!f.exists()) return
        try {
            val json = f.readText()
            if (json.isBlank()) return
            val jsonElement = JsonParser.parseString(json)
            val restoredList = mutableListOf<Anime>()
            if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray.forEach { element ->
                    try {
                        val obj = element.asJsonObject
                        val id = if (obj.has("id")) obj.get("id").asString else UUID.randomUUID().toString()
                        val title = if (obj.has("title")) obj.get("title").asString else "Unknown Title"
                        val episodes = if (obj.has("episodes") && !obj.get("episodes").isJsonNull) obj.get("episodes").asInt else 0
                        val rating = if (obj.has("rating") && !obj.get("rating").isJsonNull) obj.get("rating").asInt else 0
                        val orderIndex = if (obj.has("orderIndex") && !obj.get("orderIndex").isJsonNull) obj.get("orderIndex").asInt else 0
                        val dateAdded = if (obj.has("dateAdded") && !obj.get("dateAdded").isJsonNull) obj.get("dateAdded").asLong else System.currentTimeMillis()
                        val imageFileName = if (obj.has("imageFileName") && !obj.get("imageFileName").isJsonNull) obj.get("imageFileName").asString else null
                        val isFavorite = if (obj.has("isFavorite") && !obj.get("isFavorite").isJsonNull) obj.get("isFavorite").asBoolean else false
                        val tags = mutableListOf<String>(); if (obj.has("tags") && obj.get("tags").isJsonArray) { obj.get("tags").asJsonArray.forEach { tags.add(it.asString) } }
                        val categoryType = if (obj.has("categoryType") && !obj.get("categoryType").isJsonNull) obj.get("categoryType").asString else ""
                        restoredList.add(Anime(id, title, episodes, rating, imageFileName, orderIndex, dateAdded, isFavorite, tags, categoryType))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            _animeList.clear(); _animeList.addAll(restoredList.sortedBy { it.orderIndex })
            if (_animeList.isNotEmpty()) save()
        } catch (e: Exception) { e.printStackTrace() }
        loadUserAvatar()
        val fIgnored = getIgnoredFile()
        if (fIgnored.exists()) { try { val map: Map<String, Int> = Gson().fromJson(fIgnored.readText(), object : TypeToken<Map<String, Int>>() {}.type) ?: emptyMap(); ignoredUpdatesMap.putAll(map) } catch (e: Exception) { e.printStackTrace() } }
        needsUpdateCheck = true
        checkForUpdates()
    }

    fun toggleFavorite(id: String) { val idx = _animeList.indexOfFirst { it.id == id }; if (idx != -1) { val item = _animeList[idx]; _animeList[idx] = item.copy(isFavorite = !item.isFavorite); save() } }

    fun checkForUpdates(force: Boolean = false) {
        if (!force && !needsUpdateCheck && _updates.isEmpty()) return
        if (isCheckingUpdates) return
        isCheckingUpdates = true
        viewModelScope.launch {
            try {
                val newUpdates = mutableListOf<AnimeUpdate>()
                _animeList.forEach { anime ->
                    val result = repository.findTotalEpisodes(anime.title)
                    if (result != null) {
                        val (remoteEps, source) = result
                        val isIgnored = ignoredUpdatesMap[anime.id] == remoteEps
                        if (remoteEps > anime.episodes && !isIgnored) newUpdates.add(AnimeUpdate(anime.id, anime.title, anime.episodes, remoteEps, source))
                    }
                }
                _updates.clear(); _updates.addAll(newUpdates)
                if (_updates.isEmpty()) needsUpdateCheck = false
            } catch (e: Exception) { e.printStackTrace() } finally { isCheckingUpdates = false }
        }
    }

    fun acceptUpdate(update: AnimeUpdate, ctx: Context) {
        val anime = getAnimeById(update.animeId) ?: return
        updateAnime(ctx, anime.id, anime.title, update.newEpisodes, anime.rating, null, anime.tags, anime.categoryType)
        _updates.remove(update)
    }

    fun dismissUpdate(update: AnimeUpdate) { ignoredUpdatesMap[update.animeId] = update.newEpisodes; saveIgnored(); _updates.remove(update); if (_updates.isEmpty()) needsUpdateCheck = false }
    fun getAnimeById(id: String): Anime? = _animeList.find { it.id == id }

    fun addAnime(ctx: Context, title: String, ep: Int, rate: Int, uri: Uri?, tags: List<String>, categoryType: String) {
        val id = UUID.randomUUID().toString()
        val img = if (uri != null) saveImg(ctx, uri, id) else null
        _animeList.add(0, Anime(id, title, ep, rate, img, (_animeList.maxOfOrNull { it.orderIndex }?:0)+1, tags = tags, categoryType = categoryType))
        save(); needsUpdateCheck = true
    }

    fun updateAnime(ctx: Context, id: String, title: String, ep: Int, rate: Int, uri: Uri?, tags: List<String>, categoryType: String) {
        val idx = _animeList.indexOfFirst { it.id == id }; if (idx == -1) return
        var img = _animeList[idx].imageFileName
        if (uri != null) { val newImg = saveImg(ctx, uri, id); if (newImg != null) { _animeList[idx].imageFileName?.let { File(getImgDir(), it).delete() }; img = newImg } }
        _animeList[idx] = _animeList[idx].copy(title=title, episodes=ep, rating=rate, imageFileName=img, tags=tags, categoryType=categoryType)
        save(); needsUpdateCheck = true; if (ignoredUpdatesMap.containsKey(id)) { ignoredUpdatesMap.remove(id); saveIgnored() }
    }

    fun deleteAnime(id: String) { val anime = _animeList.find { it.id == id } ?: return; anime.imageFileName?.let { File(getImgDir(), it).delete() }; _animeList.remove(anime); save(); needsUpdateCheck = true }

    private fun save() { try { getDataFile().writeText(Gson().toJson(_animeList)) } catch(e:Exception){e.printStackTrace()} }
    private fun saveIgnored() { try { getIgnoredFile().writeText(Gson().toJson(ignoredUpdatesMap)) } catch(e:Exception){e.printStackTrace()} }
    private fun saveImg(ctx: Context, uri: Uri, id: String): String? { return try { val name = "img_${id}_${System.currentTimeMillis()}.jpg"; ctx.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(File(getImgDir(), name)).use { o -> i.copyTo(o) } }; name } catch(e: Exception) { null } }
    fun getImgPath(name: String?): File? = if(name!=null) File(getImgDir(), name).let { if(it.exists()) it else null } else null

    fun getDisplayList(): List<Anime> {
        val rawQuery = searchQuery.trim()
        val filteredByGenre = if (filterSelectedTags.isEmpty()) _animeList else _animeList.filter { anime -> anime.tags.containsAll(filterSelectedTags) }
        if (rawQuery.isBlank()) return sortList(filteredByGenre)
        val normalizedQuery = rawQuery.lowercase()
        return filteredByGenre.mapNotNull { anime -> val score = calculateRelevanceScore(normalizedQuery, anime.title.lowercase()); if (score > 0) RankedAnime(anime, score) else null }.sortedWith(compareByDescending<RankedAnime> { it.score }.thenBy { it.anime.title }).map { it.anime }
    }

    private fun calculateRelevanceScore(query: String, title: String): Int {
        if (title == query) return 100
        if (title.startsWith(query)) return 90
        val words = title.split(" ", "-", ":"); if (words.any { it.startsWith(query) }) return 80
        if (title.contains(query)) return 60
        if (query.length > 2) { val dist = levenshtein(query, title); val maxEdits = if (query.length < 6) 1 else 2; if (dist <= maxEdits) return 50 }
        return 0
    }

    private fun sortList(list: List<Anime>): List<Anime> {
        return when(sortOption) { SortOption.DATE_NEWEST -> list.sortedByDescending { it.dateAdded }; SortOption.RATING_HIGH -> list.sortedByDescending { it.rating }; SortOption.AZ -> list.sortedBy { it.title }; SortOption.FAVORITES -> list.filter { it.isFavorite }.sortedBy { it.title } }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) return 0; if (lhs.isEmpty()) return rhs.length; if (rhs.isEmpty()) return lhs.length
        var cost = IntArray(lhs.length + 1) { it }; var newCost = IntArray(lhs.length + 1) { 0 }
        for (i in 1..rhs.length) { newCost[0] = i; for (j in 1..lhs.length) { val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1; newCost[j] = min(min(cost[j] + 1, newCost[j - 1] + 1), cost[j - 1] + match) }; val swap = cost; cost = newCost; newCost = swap }
        return cost[lhs.length]
    }
}

// ==========================================
// UI COMPONENTS
// ==========================================

fun performHaptic(view: View, type: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when(type) { "success" -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); "light" -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); "warning" -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }
    } else { view.performHapticFeedback(if (type == "success") HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.VIRTUAL_KEY) }
}
fun performHaptic(view: View, isStrong: Boolean) { performHaptic(view, if (isStrong) "warning" else "light") }

val StarIconVector: ImageVector get() = Icons.Default.Star

@Composable
fun StarRatingBar(rating: Int, onRatingChanged: (Int) -> Unit) {
    val colors = listOf(RateColor1, RateColor2, RateColor3, RateColor4, RateColor5)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val isSelected = i <= rating
            val animatedColor by animateColorAsState(targetValue = if (isSelected) colors[rating - 1] else RateColorEmpty, animationSpec = tween(300), label = "color")
            val scale by animateFloatAsState(targetValue = if (isSelected) 1.2f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
            Icon(imageVector = StarIconVector, contentDescription = "Rate $i", tint = animatedColor, modifier = Modifier.size(54.dp).scale(scale).padding(4.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRatingChanged(i) })
        }
    }
}

@Composable
fun EpisodeSuggestions(onSelect: (String) -> Unit) {
    val suggestions = listOf("12", "13", "24", "36", "48", "100")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        suggestions.forEach { num ->
            Box(modifier = Modifier.weight(1f).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape).clickable { onSelect(num) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(text = num, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AnimatedSaveFab(isEnabled: Boolean, onClick: () -> Unit) {
    var isSaved by remember { mutableStateOf(false) }
    val view = LocalView.current
    FloatingActionButton(onClick = { if (isEnabled && !isSaved) { isSaved = true; performHaptic(view, "success"); onClick() } }, containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color(0xFF424242), contentColor = if (isEnabled) Color.White else Color.Gray, shape = CircleShape, modifier = Modifier.size(64.dp)) {
        AnimatedContent(targetState = isSaved, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "save_animation") { saved -> if (saved) Icon(imageVector = Icons.Default.Check, contentDescription = "Saved", modifier = Modifier.size(28.dp)) else Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(28.dp)) }
    }
}

@Composable
fun AnimatedCopyButton(textToCopy: String) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val view = LocalView.current
    Box(contentAlignment = Alignment.Center) {
        androidx.compose.animation.AnimatedVisibility(visible = isCopied, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(), modifier = Modifier.align(Alignment.TopCenter).offset(y = (-40).dp).zIndex(10f)) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("Copied!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
        IconButton(onClick = { if (textToCopy.isNotEmpty()) { performHaptic(view, "light"); clipboardManager.setText(AnnotatedString(textToCopy)); isCopied = true } }) {
            AnimatedContent(targetState = isCopied, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "copy_icon") { copied -> if (copied) { LaunchedEffect(Unit) { delay(2000); isCopied = false }; Icon(Icons.Default.Check, null, tint = BrandBlue, modifier = Modifier.size(24.dp)) } else Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp)) }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TextAsIndividualLetters(animatedContentScope: AnimatedContentScope, text: String, modifier: Modifier = Modifier, style: TextStyle = TextStyle(), textColor: Color) {
    Row(modifier) {
        text.forEachIndexed { index, letter ->
            Text(text = "$letter", modifier = Modifier.sharedBounds(sharedContentState = rememberSharedContentState(key = "hint_$index"), animatedVisibilityScope = animatedContentScope, boundsTransform = { _, _ -> spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 1000f) }), style = style, color = textColor)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AnimatedOneUiTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, maxLines: Int = 1) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val showHintAbove = isFocused || value.isNotEmpty()
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.secondary
    val textStyle = TextStyle(fontSize = 18.sp, color = textColor, fontFamily = SnProFamily)
    val hintStyleInner = TextStyle(fontSize = 18.sp, fontFamily = SnProFamily)
    val hintStyleOuter = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SnProFamily)
    BasicTextField(value = value, onValueChange = onValueChange, interactionSource = interactionSource, textStyle = textStyle, singleLine = singleLine, maxLines = maxLines, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), decorationBox = { innerTextField ->
        SharedTransitionLayout {
            AnimatedContent(targetState = showHintAbove, transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }, label = "hintAnimation") { targetShowAbove ->
                if (targetShowAbove) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp).height(16.dp)) { TextAsIndividualLetters(animatedContentScope = this@AnimatedContent, text = placeholder, style = hintStyleOuter, textColor = MaterialTheme.colorScheme.primary) }
                        Box(modifier = Modifier.fillMaxWidth().sharedElement(rememberSharedContentState(key = "container"), animatedVisibilityScope = this@AnimatedContent).clip(RoundedCornerShape(20.dp)).background(containerColor).padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) { innerTextField() }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().sharedElement(rememberSharedContentState(key = "container"), animatedVisibilityScope = this@AnimatedContent).clip(RoundedCornerShape(20.dp)).background(containerColor).padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) { TextAsIndividualLetters(animatedContentScope = this@AnimatedContent, text = placeholder, style = hintStyleInner, textColor = hintColor); innerTextField() }
                    }
                }
            }
        }
    })
}

@Composable
fun RatingChip(rating: Int) {
    val starTint = getRatingColor(rating)
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    // Фон:
    // Dark: Полупрозрачный темный (в стиле Glass)
    // Light: SurfaceVariant (чуть темнее фона, чтобы выделялось)
    val bgColor = if (isDark) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    // Базовый модификатор (форма + фон)
    var modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(bgColor)

    // ЛОГИКА: Добавляем рамку ТОЛЬКО в темной теме
    if (isDark) {
        modifier = modifier.border(
            width = 0.5.dp,
            color = Color.White.copy(alpha = 0.1f), // Та самая "красивая белая рамочка"
            shape = RoundedCornerShape(8.dp)
        )
    }
    // В светлой теме border не добавляем -> нет серой окантовки.
    // shadow() не используем вообще -> нет артефактов "halo".

    Box(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = starTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = rating.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}


// ==========================================
// THEME SELECTION UI
// ==========================================

@Composable
fun ThemeOptionItem(label: String, isSelected: Boolean, themeType: AppTheme, onClick: () -> Unit) {
    val borderColor = if (isSelected) BrandBlue else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val textColor = if (isSelected) BrandBlue else MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }) {
        Box(modifier = Modifier.size(width = 80.dp, height = 56.dp).clip(RoundedCornerShape(12.dp)).border(borderWidth, borderColor, RoundedCornerShape(12.dp))) {
            ThemePreviewCanvas(themeType)
            if (isSelected) { Box(modifier = Modifier.fillMaxSize().background(BrandBlue.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp)) } }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = textColor, textAlign = TextAlign.Center)
    }
}

@Composable
fun ThemePreviewCanvas(type: AppTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (type) {
            AppTheme.LIGHT -> { drawRect(color = LightBackground); drawRoundRect(color = LightSurface, topLeft = Offset(0f, size.height * 0.6f), size = size.copy(height = size.height * 0.4f), cornerRadius = CornerRadius(8f, 8f)) }
            AppTheme.DARK -> { drawRect(color = DarkBackground); drawRoundRect(color = DarkSurfaceVariant, topLeft = Offset(0f, size.height * 0.6f), size = size.copy(height = size.height * 0.4f), cornerRadius = CornerRadius(8f, 8f)) }
            AppTheme.SYSTEM -> {
                drawRect(color = Color(0xFFB0B0B0))
                val stripeWidth = 10f; val gap = 10f; var x = -size.height
                while (x < size.width + size.height) { drawLine(color = Color(0xFFE0E0E0), start = Offset(x, size.height), end = Offset(x + size.height, 0f), strokeWidth = stripeWidth); x += (stripeWidth + gap) }
            }
        }
        drawRoundRect(color = Color.Gray.copy(alpha = 0.3f), style = Stroke(width = 1f), cornerRadius = CornerRadius(30f, 30f))
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun OneUiAnimeCard(
    anime: Anime,
    viewModel: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit = {}
) {
    val imageFile = remember(anime.imageFileName) { viewModel.getImgPath(anime.imageFileName) }
    val view = LocalView.current
    val cardShape = RoundedCornerShape(24.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val infoIconColor = if (isDark) {
        Color.White
    } else {
        DarkBackground
    }

    // Цвета для чипов жанров
    val chipBgColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF303030)
    val chipTextColor = if (isDark) Color.Black else Color.White


    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .sharedBounds(rememberSharedContentState(key = "container_${anime.id}"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize)
                .shadow(elevation = 12.dp, shape = cardShape, spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(brush = Brush.verticalGradient(colors = listOf(surfaceColor, surfaceVariant)), shape = cardShape)
                .border(width = 1.dp, color = borderColor, shape = cardShape)
                .clip(cardShape)
                .clickable { performHaptic(view, "light"); onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. ИЗОБРАЖЕНИЕ
                Box(modifier = Modifier.aspectRatio(1f).fillMaxHeight().sharedElement(rememberSharedContentState(key = "image_${anime.id}"), animatedVisibilityScope = animatedVisibilityScope).clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                    if (imageFile != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageFile).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E))) { Text(text = anime.title.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray) }
                }

                Spacer(Modifier.width(16.dp))

                // 2. ЦЕНТРАЛЬНАЯ ЧАСТЬ (Текст)
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Название и звезда
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = anime.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp, modifier = Modifier.weight(1f, false))
                        if (anime.isFavorite) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Star, contentDescription = "Favorite", tint = RateColor3, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(text = "${anime.episodes} episodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                    // Жанры
                    if (anime.tags.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 2) {
                            anime.tags.take(2).forEach { tag ->
                                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(chipBgColor).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text(text = viewModel.getGenreName(tag), style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = SnProFamily, color = chipTextColor))
                                }
                            }
                        }
                    }
                }

                // 3. ПРАВАЯ КОЛОНКА
                Column(
                    modifier = Modifier.fillMaxHeight().padding(start = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    // РЕЙТИНГ
                    if (anime.rating > 0) {
                        RatingChip(rating = anime.rating)
                        Spacer(Modifier.height(12.dp))
                    }

                    // INFO КНОПКА
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = infoIconColor, // ← ВОТ ТУТ
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                performHaptic(view, "light")
                                onDetailsClick()
                            }
                    )

                }
            }
        }
    }
}

@Composable
fun AnimatedTrashButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "scale")
    Box(modifier = Modifier.size(70.dp).scale(scale).shadow(12.dp, CircleShape, spotColor = BrandRed).clip(CircleShape).background(BrandRed).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isPressed = true; onClick() }, contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

// ==========================================
// DETAILS SHEET (NEW)
// ==========================================
@Composable
fun AnimeDetailsSheet(
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val detailsState = viewModel.detailsState
    val view = LocalView.current

    LaunchedEffect(Unit) { visible = true }

    fun triggerDismiss() {
        visible = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onDismiss()
        }, 300)
    }

    BackHandler { triggerDismiss() }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { triggerDismiss() }
            )
        }

        // Content
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
                    .animateContentSize()
            ) {
                when (detailsState) {
                    is DetailsState.Idle, DetailsState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BrandBlue)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (viewModel.currentLanguage == AppLanguage.RU) "Поиск информации..." else "Fetching details...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    is DetailsState.Error -> {
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.ErrorOutline, null, tint = BrandRed, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Failed to load details", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    is DetailsState.Success -> {
                        DetailsContent(details = detailsState.data, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailsContent(details: AnimeDetails, viewModel: AnimeViewModel) {
    // Header
    Row(verticalAlignment = Alignment.Top) {
        // Poster
        if (details.posterUrl != null) {
            AsyncImage(
                model = details.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(80.dp)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(16.dp))
        }

        Column {
            Text(
                text = details.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // <--- ИСПРАВЛЕНИЕ: Читаемый цвет
            )
            if (details.altTitle != null && details.altTitle != details.title) {
                Text(
                    text = details.altTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(Modifier.height(8.dp))

            // Source Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrandBlue.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Source: ${details.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    // Grid Stats
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailStatCard(
            label = if (viewModel.currentLanguage == AppLanguage.RU) "Рейтинг" else "Score",
            value = if (details.rating != null && details.rating > 0) details.rating.toString() else "N/A",
            icon = Icons.Default.Star,
            color = RatingColor,
            modifier = Modifier.weight(1f)
        )
        DetailStatCard(
            label = if (viewModel.currentLanguage == AppLanguage.RU) "Эпизоды" else "Eps",
            value = "${details.episodesAired}${if (details.episodesTotal != null) "/${details.episodesTotal}" else ""}",
            icon = Icons.Default.PlayArrow,
            color = EpisodesColor,
            modifier = Modifier.weight(1f)
        )
        DetailStatCard(
            label = if (viewModel.currentLanguage == AppLanguage.RU) "Тип" else "Type",
            value = details.type,
            icon = Icons.Default.Movie,
            color = Color(0xFFE91E63),
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(Modifier.height(16.dp))

    // Status & Next Ep
    Row(verticalAlignment = Alignment.CenterVertically) {
        val statusColor = if (details.status.contains("Ongoing", ignoreCase = true) || details.status.contains("онгоинг", ignoreCase = true)) RateColor5 else MaterialTheme.colorScheme.secondary
        Icon(Icons.Default.Info, null, tint = statusColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(details.status, style = MaterialTheme.typography.labelLarge, color = statusColor)

        if (details.nextEpisode != null) {
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(details.nextEpisode, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = BrandBlue)
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Description
    Text(
        text = details.description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        lineHeight = 20.sp,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp).fillMaxWidth()
    )

    Spacer(Modifier.height(16.dp))

    // Genres
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        details.genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(genre, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun DetailStatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun SpringBottomDialog(
    title: String,
    subtitle: String,
    confirmText: String,
    icon: ImageVector,
    accentColor: Color,
    imageFile: File? = null,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancelText: String = "Cancel"
) {
    var visible by remember { mutableStateOf(false) }
    val view = LocalView.current
    LaunchedEffect(Unit) { visible = true }
    fun dismiss() { visible = false; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onCancel() }, 300) }
    BackHandler { dismiss() }
    Box(modifier = Modifier.fillMaxSize().zIndex(100f), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(300)), exit = fadeOut(animationSpec = tween(300))) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dismiss() })
        }
        AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.surface).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (imageFile != null) {
                    Box(modifier = Modifier.height(280.dp).aspectRatio(0.7f).shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = accentColor).clip(RoundedCornerShape(20.dp)).background(Color.Gray)) {
                        AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageFile).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)))))
                    }
                    Spacer(Modifier.height(24.dp))
                } else {
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.height(20.dp))
                }
                Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { performHaptic(view, "success"); onConfirm() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White), shape = RoundedCornerShape(18.dp)) { Text(text = confirmText, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { performHaptic(view, "light"); dismiss() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text(text = cancelText, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) }
            }
        }
    }
}

@Composable
fun GenreFilterSheet(viewModel: AnimeViewModel, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val view = LocalView.current
    LaunchedEffect(Unit) { visible = true }
    fun triggerDismiss() { visible = false; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onDismiss() }, 300) }
    val onTagToggle: (String, String) -> Unit = { tag, categoryType ->
        val currentTags = viewModel.filterSelectedTags.toMutableList()
        if (currentTags.contains(tag)) { currentTags.remove(tag); if (currentTags.isEmpty()) { viewModel.filterCategoryType = "" } } else { if (currentTags.size < 3 && (viewModel.filterCategoryType.isEmpty() || viewModel.filterCategoryType == categoryType)) { currentTags.add(tag); viewModel.filterCategoryType = categoryType } }
        viewModel.filterSelectedTags = currentTags; performHaptic(view, "light")
    }
    BackHandler { triggerDismiss() }
    Box(modifier = Modifier.fillMaxSize().zIndex(100f), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(300)), exit = fadeOut(animationSpec = tween(300))) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { triggerDismiss() }) }
        AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.background).padding(24.dp)) {
                Text(text = viewModel.strings.filterByGenre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 16.dp))
                GenreSelectionSection(viewModel = viewModel, selectedTags = viewModel.filterSelectedTags, activeCategory = viewModel.filterCategoryType, onTagToggle = onTagToggle)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { triggerDismiss() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) { Text("Apply", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
            }
        }
    }
}

@Composable
fun StatsOverlay(viewModel: AnimeViewModel, onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    fun triggerDismiss() { visible = false; android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ onDismiss() }, 300) }
    BackHandler { triggerDismiss() }
    Box(modifier = Modifier.fillMaxSize().zIndex(100f), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = tween(300)), exit = fadeOut(animationSpec = tween(300))) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { triggerDismiss() }) }
        AnimatedVisibility(visible = visible, enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f, stiffness = 450f)) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(MaterialTheme.colorScheme.background).padding(top = 24.dp)) { WatchStatsContent(animeList = viewModel.animeList, viewModel = viewModel) }
        }
    }
}


@Composable
fun MalistWorkspaceTopBar(viewModel: AnimeViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia(), onResult = { uri -> if (uri != null) { viewModel.saveUserAvatar(context, uri) } })
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { singlePhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
            if (viewModel.userAvatarPath != null) { AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(File(viewModel.userAvatarPath!!)).crossfade(true).build(), contentDescription = "User Avatar", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) } else { Text("M", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.width(12.dp))
        Column { Row(verticalAlignment = Alignment.CenterVertically) { Text(text = viewModel.strings.appName, style = MaterialTheme.typography.headlineMedium) } }
    }
}

@Composable
fun StatsCard(title: String, icon: ImageVector, isExpanded: Boolean, onClick: () -> Unit, iconTint: Color? = null, iconBg: Color? = null, content: @Composable () -> Unit) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(20.dp)
    val finalIconTint = iconTint ?: MaterialTheme.colorScheme.onSecondaryContainer
    val finalIconBg = iconBg ?: MaterialTheme.colorScheme.secondaryContainer
    Box(modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.08f)).clip(shape).background(brush = Brush.verticalGradient(colors = listOf(surfaceColor, surfaceVariant))).border(width = 1.dp, color = borderColor, shape = shape).clickable { onClick() }.animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(finalIconBg), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = finalIconTint, modifier = Modifier.size(24.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor, fontWeight = FontWeight.SemiBold)
            }
            if (isExpanded) { Spacer(modifier = Modifier.height(16.dp)); Box(modifier = Modifier.fillMaxWidth()) { content() } }
        }
    }
}

@Composable
fun WatchStatsContent(animeList: List<Anime>, viewModel: AnimeViewModel) {
    val totalEpisodes = remember(animeList) { animeList.sumOf { it.episodes } }
    val totalMinutes = totalEpisodes * 22L
    val formatter = DecimalFormat("#,###")
    val formattedMinutes = formatter.format(totalMinutes).replace(",", " ")
    val formattedHours = formatter.format(totalMinutes / 60).replace(",", " ")
    val formattedDays = formatter.format(totalMinutes / (60 * 24)).replace(",", " ")
    val avgRating = remember(animeList) { if (animeList.isEmpty()) 0.0 else animeList.map { it.rating }.average() }
    val (rankName, rankColor) = remember(totalEpisodes) { when { totalEpisodes >= 1200 -> "Legend" to RateColor5; totalEpisodes >= 800 -> "Veteran" to RateColor4; totalEpisodes >= 500 -> "Dedicated" to RateColor3; totalEpisodes >= 300 -> "Casual" to RateColor2; else -> "Rookie" to RateColor1 } }
    var expandedIndex by remember { mutableIntStateOf(-1) }
    val textColor = MaterialTheme.colorScheme.onBackground
    val secColor = MaterialTheme.colorScheme.secondary
    val s = viewModel.strings

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.statsTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textColor); Spacer(modifier = Modifier.height(4.dp)); Text(s.statsSubtitle, style = MaterialTheme.typography.bodyMedium, color = secColor); Spacer(modifier = Modifier.height(24.dp))
        StatsCard(title = s.episodesWatched, icon = Icons.Default.Visibility, isExpanded = expandedIndex == 0, iconTint = EpisodesColor, iconBg = EpisodesColor.copy(alpha = 0.15f), onClick = { expandedIndex = if (expandedIndex == 0) -1 else 0 }) { Text(text = "$totalEpisodes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor) }
        Spacer(modifier = Modifier.height(12.dp))
        StatsCard(title = s.timeSpent, icon = Icons.Default.Schedule, isExpanded = expandedIndex == 1, iconTint = TimeColor, iconBg = TimeColor.copy(alpha = 0.15f), onClick = { expandedIndex = if (expandedIndex == 1) -1 else 1 }) { Column { Text(text = "$formattedMinutes min", style = MaterialTheme.typography.titleLarge, color = textColor); Text(text = "$formattedHours h", style = MaterialTheme.typography.titleLarge, color = secColor); Text(text = "$formattedDays days", style = MaterialTheme.typography.titleLarge, color = BrandBlue.copy(alpha = 0.6f)) } }
        Spacer(modifier = Modifier.height(12.dp))
        StatsCard(title = s.avgRating, icon = Icons.Default.Star, isExpanded = expandedIndex == 2, iconTint = RatingColor, iconBg = RatingColor.copy(alpha = 0.18f), onClick = { expandedIndex = if (expandedIndex == 2) -1 else 2 }) { Text(text = String.format("%.1f / 5⭐", avgRating), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor) }
        Spacer(modifier = Modifier.height(12.dp))
        StatsCard(title = s.rankTitle, icon = Icons.Default.MilitaryTech, isExpanded = expandedIndex == 3, iconTint = RankColor, iconBg = RankColor.copy(alpha = 0.16f), onClick = { expandedIndex = if (expandedIndex == 3) -1 else 3 }) { Text(text = rankName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = rankColor) }
    }
}

// ==========================================
// SWIPE LOGIC & COMPOSABLES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection
    val color by animateColorAsState(when (direction) { SwipeToDismissBoxValue.EndToStart -> BrandRed; SwipeToDismissBoxValue.StartToEnd -> RateColor3; else -> Color.Transparent }, label = "swipeColor")
    val alignment = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart; SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd; else -> Alignment.Center }
    val icon = when (direction) { SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Star; SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete; else -> Icons.Default.Delete }
    val scale by animateFloatAsState(if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) 1.2f else 0.8f, label = "iconScale")
    Box(Modifier.fillMaxSize().background(color, RoundedCornerShape(24.dp)).padding(horizontal = 24.dp), contentAlignment = alignment) { if (color != Color.Transparent) { Icon(icon, contentDescription = null, modifier = Modifier.scale(scale), tint = Color.White) } }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    nav: NavController,
    vm: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val kbd = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val hazeState = remember { HazeState() }
    var showCSheet by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var animeToDelete by remember { mutableStateOf<Anime?>(null) }
    var animeToFavorite by remember { mutableStateOf<Anime?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.checkForUpdates() }
    var isDockVisible by remember { mutableStateOf(true) }
    val finalDockVisible = isDockVisible || isSearchVisible
    val nestedScrollConnection = remember { object : NestedScrollConnection { override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset { val threshold = 10f; if (available.y < -threshold) { if (isDockVisible) isDockVisible = false } else if (available.y > threshold) { if (!isDockVisible) isDockVisible = true }; return Offset.Zero } } }
    BackHandler(enabled = isSearchVisible || vm.searchQuery.isNotEmpty()) { if (isSearchVisible) { performHaptic(view, "light"); isSearchVisible = false; vm.searchQuery = ""; focusManager.clearFocus(); kbd?.hide() } }
    val listState = rememberLazyListState()
    var overscrollAmount by remember { mutableFloatStateOf(0f) }
    val isHeaderFloating by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 } }
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val bgColor = MaterialTheme.colorScheme.background
    val shouldBlur = (isSearchVisible && vm.searchQuery.isBlank()) || showCSheet || animeToDelete != null || animeToFavorite != null || vm.isGenreFilterVisible
    val blurAmount by animateDpAsState(targetValue = if (shouldBlur) 10.dp else 0.dp, label = "blur")

    // --- DETAILS LOGIC ---
    if (vm.selectedAnimeForDetails != null) {
        AnimeDetailsSheet(
            viewModel = vm,
            onDismiss = { vm.closeDetails() }
        )
    }

    Scaffold(containerColor = Color.Transparent, bottomBar = {}, floatingActionButton = {}) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(bgColor))
            Box(modifier = Modifier.zIndex(6f).align(Alignment.TopEnd).padding(end = 16.dp)) { GlassActionDock(hazeState = hazeState, isFloating = isHeaderFloating, sortOption = vm.sortOption, viewModel = vm, onSortSelected = { sort -> performHaptic(view, "light"); vm.sortOption = sort }, modifier = Modifier.padding(top = 12.dp)) }
            Column(modifier = Modifier.fillMaxSize().blur(blurAmount)) {
                Box(modifier = Modifier.fillMaxSize().weight(1f).background(bgColor)) {
                    val list = vm.getDisplayList()
                    if (list.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) { MalistWorkspaceTopBar(viewModel = vm); Box(modifier = Modifier.weight(1f)) { EmptyStateView(title = if (vm.searchQuery.isNotEmpty()) vm.strings.noResults else if (vm.sortOption == SortOption.FAVORITES) vm.strings.noFavorites else vm.strings.emptyTitle, subtitle = if (vm.searchQuery.isNotEmpty()) "" else if (vm.sortOption == SortOption.FAVORITES) "" else vm.strings.emptySubtitle) } }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedScrollConnection).customOverscroll(listState = listState, onNewOverscrollAmount = { overscrollAmount = it }).offset { IntOffset(0, overscrollAmount.roundToInt()) }) {
                            LazyColumn(state = listState, contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp, start = 0.dp, end = 0.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.haze(state = hazeState)) {
                                item { MalistWorkspaceTopBar(viewModel = vm) }
                                items(list, key = { it.id }) { anime ->
                                    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { when(it) { SwipeToDismissBoxValue.StartToEnd -> { performHaptic(view, "success"); animeToFavorite = anime; false }; SwipeToDismissBoxValue.EndToStart -> { performHaptic(view, "warning"); animeToDelete = anime; false }; else -> false } }, positionalThreshold = { totalDistance -> totalDistance * 0.5f })
                                    SwipeToDismissBox(state = dismissState, backgroundContent = { SwipeBackground(dismissState) }, modifier = Modifier.padding(horizontal = 16.dp).animateItem()) {
                                        OneUiAnimeCard(
                                            anime = anime,
                                            viewModel = vm,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onClick = { nav.navigate("add_anime?animeId=${anime.id}") },
                                            onDetailsClick = { performHaptic(view, "light"); vm.loadAnimeDetails(anime) } // Trigger Details
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (!isSearchVisible && animeToDelete == null && animeToFavorite == null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f).navigationBarsPadding()) {
                    AnimatedVisibility(visible = finalDockVisible, enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)), exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))) {
                        GlassBottomNavigation(hazeState = hazeState, nav = nav, viewModel = vm, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, onShowStats = { showCSheet = true }, onShowNotifs = {}, onSearchClick = { performHaptic(view, "light"); isSearchVisible = !isSearchVisible; if (!isSearchVisible) { vm.searchQuery = ""; focusManager.clearFocus(); kbd?.hide() } }, isSearchActive = isSearchVisible, modifier = Modifier)
                    }
                }
            }
            AnimatedVisibility(visible = isSearchVisible, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).windowInsetsPadding(WindowInsets.ime).padding(bottom = 16.dp).zIndex(10f)) {
                SimpGlassCard(hazeState = hazeState, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    BasicTextField(value = vm.searchQuery, onValueChange = { vm.searchQuery = it; if (it.isNotEmpty()) performHaptic(view, "light") }, modifier = Modifier.fillMaxSize().focusRequester(searchFocusRequester).padding(horizontal = 20.dp), singleLine = true, textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SnProFamily), cursorBrush = SolidColor(BrandBlue), decorationBox = { innerTextField -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = BrandBlue); Spacer(Modifier.width(12.dp)); Box { if (vm.searchQuery.isEmpty()) { Text(vm.strings.searchPlaceholder, color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp, fontFamily = SnProFamily) }; innerTextField() } } }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { kbd?.hide() }))
                }
                LaunchedEffect(Unit) { searchFocusRequester.requestFocus(); kbd?.show() }
            }
            if (shouldBlur && animeToDelete == null && animeToFavorite == null && !vm.isGenreFilterVisible && vm.selectedAnimeForDetails == null) { Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus(); isSearchVisible = false; kbd?.hide() }.zIndex(2f)) }
            if (animeToDelete != null) { SpringBottomDialog(title = vm.strings.deleteTitle, subtitle = vm.strings.deleteSubtitle, confirmText = vm.strings.deleteConfirm, cancelText = vm.strings.cancel, icon = Icons.Default.Delete, accentColor = BrandRed, imageFile = vm.getImgPath(animeToDelete?.imageFileName), onConfirm = { vm.deleteAnime(animeToDelete!!.id); animeToDelete = null }, onCancel = { animeToDelete = null }) }
            if (animeToFavorite != null) { SpringBottomDialog(title = vm.strings.favTitle, subtitle = vm.strings.favSubtitle, confirmText = vm.strings.favConfirm, cancelText = vm.strings.cancel, icon = Icons.Default.Star, accentColor = RateColor3, imageFile = vm.getImgPath(animeToFavorite?.imageFileName), onConfirm = { vm.toggleFavorite(animeToFavorite!!.id); animeToFavorite = null }, onCancel = { animeToFavorite = null }) }
            AnimatedVisibility(visible = showScrollToTop && !isSearchVisible && animeToDelete == null && animeToFavorite == null, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(), modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 160.dp, end = 24.dp).zIndex(1f)) { SimpGlassCard(hazeState = hazeState, shape = CircleShape, modifier = Modifier.size(44.dp).clickable { performHaptic(view, "light"); scope.launch { listState.animateScrollToItem(0) } }) { Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = MaterialTheme.colorScheme.onSurface) } }
        }
        if (showCSheet) { StatsOverlay(viewModel = vm, onDismiss = { showCSheet = false }) }
        if (vm.isGenreFilterVisible) { GenreFilterSheet(viewModel = vm, onDismiss = { vm.isGenreFilterVisible = false }) }
    }
}

// ==========================================
// НОВЫЕ КОМПОНЕНТЫ ВЫБОРА ЖАНРОВ
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreSelectionSection(viewModel: AnimeViewModel, selectedTags: List<String>, activeCategory: String, onTagToggle: (String, String) -> Unit) {
    val animeGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.ANIME) }
    val movieGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.MOVIE) }
    val seriesGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.SERIES) }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExpandableCategoryCard(title = viewModel.strings.genreAnime, icon = Icons.Outlined.Animation, genres = animeGenres, viewModel = viewModel, categoryType = "Anime", activeCategory = activeCategory, selectedTags = selectedTags, isExpanded = expandedCategory == "Anime", onExpandToggle = { expandedCategory = if (expandedCategory == "Anime") null else "Anime" }, onTagToggle = onTagToggle)
        ExpandableCategoryCard(title = viewModel.strings.genreMovies, icon = Icons.Outlined.Movie, genres = movieGenres, viewModel = viewModel, categoryType = "Movies", activeCategory = activeCategory, selectedTags = selectedTags, isExpanded = expandedCategory == "Movies", onExpandToggle = { expandedCategory = if (expandedCategory == "Movies") null else "Movies" }, onTagToggle = onTagToggle)
        ExpandableCategoryCard(title = viewModel.strings.genreSeries, icon = Icons.Outlined.Tv, genres = seriesGenres, viewModel = viewModel, categoryType = "Series", activeCategory = activeCategory, selectedTags = selectedTags, isExpanded = expandedCategory == "Series", onExpandToggle = { expandedCategory = if (expandedCategory == "Series") null else "Series" }, onTagToggle = onTagToggle)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableCategoryCard(title: String, icon: ImageVector, genres: List<GenreDefinition>, viewModel: AnimeViewModel, categoryType: String, activeCategory: String, selectedTags: List<String>, isExpanded: Boolean, onExpandToggle: () -> Unit, onTagToggle: (String, String) -> Unit) {
    val isLocked = activeCategory.isNotEmpty() && activeCategory != categoryType
    val alpha = if (isLocked) 0.5f else 1f
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    Box(modifier = Modifier.fillMaxWidth().alpha(alpha).clip(RoundedCornerShape(16.dp)).background(surfaceColor).border(1.dp, if(isExpanded) BrandBlue else borderColor, RoundedCornerShape(16.dp)).clickable(enabled = !isLocked) { onExpandToggle() }.animateContentSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = if(isExpanded) BrandBlue else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Icon(imageVector = if(isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.forEach { genreDef ->
                        val isSelected = selectedTags.contains(genreDef.id)
                        val displayName = if (viewModel.currentLanguage == AppLanguage.RU) genreDef.ru else genreDef.en
                        GenreChip(text = displayName, isSelected = isSelected, onClick = { onTagToggle(genreDef.id, categoryType) })
                    }
                }
            }
        }
    }
}

@Composable
fun GenreChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) BrandBlue else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderStroke = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(bgColor).then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(50)) else Modifier).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)) { Text(text = text, color = contentColor, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AddEditScreen(nav: NavController, vm: AnimeViewModel, id: String?, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    val anime = remember(id) { id?.let { vm.getAnimeById(it) } }
    var title by remember { mutableStateOf(anime?.title ?: "") }
    var ep by remember { mutableStateOf(anime?.episodes?.toString() ?: "") }
    var rate by remember { mutableIntStateOf(anime?.rating ?: 0) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    var selectedTags by remember { mutableStateOf(anime?.tags ?: emptyList()) }
    var activeCategory by remember { mutableStateOf(anime?.categoryType ?: "") }
    val scope = rememberCoroutineScope()
    val hasChanges by remember(title, ep, rate, uri, selectedTags) { derivedStateOf { if (id == null) { title.isNotBlank() } else { (anime != null) && (title != anime.title || ep != anime.episodes.toString() || rate != anime.rating || uri != null || selectedTags != anime.tags) } } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val ctx = LocalContext.current
    val view = LocalView.current
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val s = vm.strings
    val onTagToggle: (String, String) -> Unit = { tag, categoryType -> val currentTags = selectedTags.toMutableList(); if (currentTags.contains(tag)) { currentTags.remove(tag); if (currentTags.isEmpty()) { activeCategory = "" } } else { if (currentTags.size < 3 && (activeCategory.isEmpty() || activeCategory == categoryType)) { currentTags.add(tag); activeCategory = categoryType } }; selectedTags = currentTags; performHaptic(view, "light") }
    with(sharedTransitionScope) {
        val sharedModifier = if (id == null) { Modifier.sharedBounds(rememberSharedContentState(key = "fab_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds) } else { Modifier.sharedBounds(rememberSharedContentState(key = "card_${id}"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds) }
        Scaffold(modifier = Modifier.fillMaxSize().then(sharedModifier), containerColor = Color.Transparent, topBar = { Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textC, modifier = if (id == null) Modifier.sharedElement(rememberSharedContentState(key = "fab_icon"), animatedVisibilityScope = animatedVisibilityScope) else Modifier) }; Spacer(Modifier.width(16.dp)); Text(text = if (id == null) s.addTitle else s.editTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textC) } }, floatingActionButton = { AnimatedSaveFab(isEnabled = hasChanges, onClick = { performHaptic(view, "success"); if (title.isNotEmpty()) { scope.launch { delay(600); if (id != null) vm.updateAnime(ctx, id, title, ep.toIntOrNull()?:0, rate, uri, selectedTags, activeCategory) else vm.addAnime(ctx, title, ep.toIntOrNull()?:0, rate, uri, selectedTags, activeCategory); nav.popBackStack() } } else { Toast.makeText(ctx, s.enterTitleToast, Toast.LENGTH_SHORT).show() } }) }) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(bg))
                Column(modifier = Modifier.padding(innerPadding).imePadding().padding(horizontal = 24.dp).fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.width(180.dp).aspectRatio(0.7f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp)).clickable { performHaptic(view, "light"); launcher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        val imageModifier = if (id != null) Modifier.sharedElement(rememberSharedContentState(key = "image_${id}"), animatedVisibilityScope = animatedVisibilityScope) else Modifier
                        Box(modifier = Modifier.fillMaxSize().then(imageModifier).clip(RoundedCornerShape(32.dp))) {
                            if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else if (anime?.imageFileName != null) AsyncImage(vm.getImgPath(anime.imageFileName), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            else Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary); Text(s.addPhoto, color = MaterialTheme.colorScheme.secondary) }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) { AnimatedOneUiTextField(value = title, onValueChange = { title = it }, placeholder = s.animeTitleHint, singleLine = false, maxLines = 4) }
                        if (title.isNotEmpty()) { Spacer(Modifier.width(8.dp)); AnimatedCopyButton(textToCopy = title) }
                    }
                    Spacer(Modifier.height(24.dp))
                    AnimatedOneUiTextField(value = ep, onValueChange = { if (it.all { c -> c.isDigit() }) ep = it }, placeholder = s.episodesHint, keyboardType = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp)); EpisodeSuggestions { selectedEp -> performHaptic(view, "light"); ep = selectedEp }
                    Spacer(Modifier.height(32.dp))
                    Text(text = "Category & Genres", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), textAlign = TextAlign.Start)
                    GenreSelectionSection(viewModel = vm, selectedTags = selectedTags, activeCategory = activeCategory, onTagToggle = onTagToggle)
                    Spacer(Modifier.height(32.dp))
                    StarRatingBar(rating = rate) { newRate -> performHaptic(view, "light"); rate = newRate }
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

// ==========================================
// ЭКРАН НАСТРОЕК
// ==========================================

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(nav: NavController, vm: AnimeViewModel, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val s = vm.strings
    val view = LocalView.current
    val context = LocalContext.current
    var expandedLang by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedUpdate by remember { mutableStateOf(false) }
    var expandedContact by remember { mutableStateOf(false) }
    with(sharedTransitionScope) {
        Box(modifier = Modifier.fillMaxSize().sharedBounds(rememberSharedContentState(key = "settings_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds).background(bg)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { performHaptic(view, "light"); nav.popBackStack() }) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textC, modifier = Modifier.sharedElement(rememberSharedContentState(key = "settings_icon"), animatedVisibilityScope = animatedVisibilityScope)) }; Spacer(Modifier.width(16.dp)); Text(text = s.settingsScreenTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textC) }
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                    item { StatsCard(title = s.languageCardTitle, icon = Icons.Outlined.Language, isExpanded = expandedLang, iconTint = EpisodesColor, iconBg = EpisodesColor.copy(alpha = 0.15f), onClick = { performHaptic(view, "light"); expandedLang = !expandedLang }) { Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { LanguageOption(text = s.langEn, isSelected = vm.currentLanguage == AppLanguage.EN, onClick = { vm.setLanguage(AppLanguage.EN) }); LanguageOption(text = s.langRu, isSelected = vm.currentLanguage == AppLanguage.RU, onClick = { vm.setLanguage(AppLanguage.RU) }) } } }
                    item { StatsCard(title = s.themeTitle, icon = Icons.Outlined.Settings, isExpanded = expandedTheme, iconTint = Color(0xFF9C27B0), iconBg = Color(0xFF9C27B0).copy(alpha = 0.15f), onClick = { performHaptic(view, "light"); expandedTheme = !expandedTheme }) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) { ThemeOptionItem(label = s.themeLight, isSelected = vm.currentTheme == AppTheme.LIGHT, themeType = AppTheme.LIGHT, onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.LIGHT) }); ThemeOptionItem(label = s.themeDark, isSelected = vm.currentTheme == AppTheme.DARK, themeType = AppTheme.DARK, onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.DARK) }); ThemeOptionItem(label = s.themeSystem, isSelected = vm.currentTheme == AppTheme.SYSTEM, themeType = AppTheme.SYSTEM, onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.SYSTEM) }) } } }
                    item { StatsCard(title = s.checkForUpdateTitle, icon = Icons.Outlined.SystemUpdate, isExpanded = expandedUpdate, iconTint = RateColor4, iconBg = RateColor4.copy(alpha = 0.15f), onClick = { performHaptic(view, "light"); expandedUpdate = !expandedUpdate }) { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = "Current version: ${vm.currentVersionName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp)); UpdateStateButton(status = vm.updateStatus, idleText = s.checkButtonText, onClick = { if (vm.updateStatus == AppUpdateStatus.IDLE || vm.updateStatus == AppUpdateStatus.ERROR) { vm.checkAppUpdate(context) } else if (vm.updateStatus == AppUpdateStatus.UPDATE_AVAILABLE) { vm.latestDownloadUrl?.let { url -> val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)); view.context.startActivity(intent) } } }) } } } }
                    item { StatsCard(title = s.contactTitle, icon = Icons.Outlined.Person, isExpanded = expandedContact, iconTint = BrandBlue, iconBg = BrandBlue.copy(alpha = 0.15f), onClick = { performHaptic(view, "light"); expandedContact = !expandedContact }) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) { Text(text = s.contactSubtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(64.dp).clip(CircleShape).clickable { performHaptic(view, "light"); val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Phnem/MAList")); view.context.startActivity(intent) }, contentAlignment = Alignment.Center) { Image(painter = painterResource(id = R.drawable.gh), contentDescription = "GitHub", modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit) }; Box(modifier = Modifier.size(64.dp).clip(CircleShape).clickable { performHaptic(view, "light"); val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/H415base")); view.context.startActivity(intent) }, contentAlignment = Alignment.Center) { Image(painter = painterResource(id = R.drawable.tg), contentDescription = "Telegram", modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit) } } } } }
                }
            }
        }
    }
}

// ==========================================
// КОМПОНЕНТЫ НАСТРОЕК
// ==========================================

@Composable
fun UpdateStateButton(status: AppUpdateStatus, idleText: String, onClick: () -> Unit) {
    val view = LocalView.current
    val backgroundColor by animateColorAsState(targetValue = when (status) { AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.secondary; AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f); AppUpdateStatus.NO_UPDATE -> RateColor4; AppUpdateStatus.UPDATE_AVAILABLE -> BrandBlue; AppUpdateStatus.ERROR -> BrandRed }, label = "btnBg")
    val contentColor by animateColorAsState(targetValue = when (status) { AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.onSecondary; AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f); else -> Color.White }, label = "btnContent")
    val widthFraction by animateFloatAsState(targetValue = if (status == AppUpdateStatus.IDLE) 1f else 0.6f, label = "btnWidth")
    Box(modifier = Modifier.fillMaxWidth(widthFraction).height(50.dp).clip(CircleShape).background(backgroundColor).clickable(enabled = status != AppUpdateStatus.LOADING, onClick = { performHaptic(view, "light"); onClick() }), contentAlignment = Alignment.Center) {
        AnimatedContent(targetState = status, transitionSpec = { (fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90))).togetherWith(fadeOut(animationSpec = tween(90))) }, label = "btnContentAnim") { targetStatus ->
            when (targetStatus) {
                AppUpdateStatus.IDLE -> { Text(text = idleText, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 16.sp) }
                AppUpdateStatus.LOADING -> { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp) }
                AppUpdateStatus.NO_UPDATE -> { Icon(imageVector = Icons.Default.Check, contentDescription = "Up to date", tint = contentColor, modifier = Modifier.size(28.dp)) }
                AppUpdateStatus.UPDATE_AVAILABLE -> { Row(verticalAlignment = Alignment.CenterVertically) { Icon(imageVector = Icons.Default.SystemUpdateAlt, contentDescription = "Update available", tint = contentColor, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text("Download", fontWeight = FontWeight.Bold, color = contentColor) } }
                AppUpdateStatus.ERROR -> { Icon(imageVector = Icons.Default.Close, contentDescription = "Error", tint = contentColor, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
fun LanguageOption(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) BrandBlue else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    Box(modifier = Modifier.fillMaxWidth().clip(CircleShape).background(bg).then(if (border != null) Modifier.border(border, CircleShape) else Modifier).clickable { onClick() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Text(text = text, fontWeight = FontWeight.Bold, color = contentColor) }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.attributes.preferredDisplayModeId = 0 }
        checkPerms()
        setContent {
            val viewModel: AnimeViewModel = viewModel()
            val context = LocalContext.current
            LaunchedEffect(Unit) { viewModel.loadAnime(); viewModel.loadSettings(); viewModel.initAppVersion(context) }
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (viewModel.currentTheme) { AppTheme.LIGHT -> false; AppTheme.DARK -> true; AppTheme.SYSTEM -> isSystemDark }
            OneUiTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                SharedTransitionLayout {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(nav = navController, vm = viewModel, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this) }
                        composable("add_anime?animeId={animeId}", arguments = listOf(navArgument("animeId") { nullable = true })) { AddEditScreen(navController, viewModel, it.arguments?.getString("animeId"), this@SharedTransitionLayout, this) }
                        composable("settings") { SettingsScreen(nav = navController, vm = viewModel, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this) }
                    }
                }
            }
        }
    }
    private fun checkPerms() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) { val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")); startActivity(intent); Toast.makeText(this, "Need file access", Toast.LENGTH_LONG).show() }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier, title: String = "Nothing in folder", subtitle: String = "Looks empty over here.") {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        ModernEmptyFolderIcon(modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun ModernEmptyFolderIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxSize()) }
}
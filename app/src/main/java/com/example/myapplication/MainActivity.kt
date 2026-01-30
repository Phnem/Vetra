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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.FilterList
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
// LOCALIZATION & THEME ENUMS
// ==========================================

enum class AppLanguage {
    RU, EN
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

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
    // SETTINGS SPECIFIC
    val settingsScreenTitle: String,
    val languageCardTitle: String,
    val langRu: String,
    val langEn: String,
    // THEME STRINGS
    val themeTitle: String,
    val themeLight: String,
    val themeDark: String,
    val themeSystem: String,

    val checkForUpdateTitle: String,
    val checkButtonText: String,
    // CONTACT ME
    val contactTitle: String,
    val contactSubtitle: String,

    // GENRES
    val genreAnime: String,
    val genreMovies: String,
    val genreSeries: String,
    val filterByGenre: String
)

val RussianStrings = UiStrings(
    appName = "MAList",
    searchPlaceholder = "Поиск в коллекции...",
    newestFirst = "Сначала новые",
    highestRated = "Высокий рейтинг",
    nameAZ = "По названию (А-Я)",
    favorites = "Избранное",
    settings = "Настройки",
    emptyTitle = "В папке пусто",
    emptySubtitle = "Кажется, здесь еще ничего нет.",
    noResults = "Ничего не найдено",
    noFavorites = "Нет избранного",
    statsTitle = "Ваша статистика",
    statsSubtitle = "Все, что вы посмотрели",
    episodesWatched = "Эпизодов просмотрено",
    timeSpent = "Затраченное время",
    avgRating = "Средний рейтинг",
    rankTitle = "Ваш ранг:",
    updatesTitle = "Обновления",
    updatesChecking = "Проверка API (Shikimori, Jikan, TMDB)...\nЭто может занять некоторое время.",
    updatesUpToDate = "У вас все актуально!",
    deleteTitle = "Удалить тайтл?",
    deleteSubtitle = "Это действие нельзя будет отменить.",
    deleteConfirm = "Удалить",
    favTitle = "Добавить в избранное?",
    favSubtitle = "Будущий вы скажет вам «спасибо» :)",
    favConfirm = "В избранное",
    cancel = "Отмена",
    addTitle = "Добавить тайтл",
    editTitle = "Изменить",
    animeTitleHint = "Название аниме",
    episodesHint = "Просмотрено серий",
    addPhoto = "Добавить фото",
    enterTitleToast = "Введите название",
    updatedToast = "Обновлено: ",
    languageName = "RU",
    settingsScreenTitle = "Настройки",
    languageCardTitle = "Язык",
    langRu = "Русский",
    langEn = "English",
    // THEME
    themeTitle = "Тема оформления",
    themeLight = "Светлая",
    themeDark = "Тёмная",
    themeSystem = "Системная",

    checkForUpdateTitle = "Обновление приложения",
    checkButtonText = "Проверить версию",
    contactTitle = "Связь со мной",
    contactSubtitle = "Нашли баг или есть идея?",

    genreAnime = "Аниме",
    genreMovies = "Фильмы",
    genreSeries = "Сериалы",
    filterByGenre = "По жанрам"
)

val EnglishStrings = UiStrings(
    appName = "MAList",
    searchPlaceholder = "Search collection...",
    newestFirst = "Newest First",
    highestRated = "Highest Rated",
    nameAZ = "Name (A-Z)",
    favorites = "Favorites",
    settings = "Settings",
    emptyTitle = "Nothing in folder",
    emptySubtitle = "Looks empty over here.",
    noResults = "No results found",
    noFavorites = "No favorites yet",
    statsTitle = "Your Watch Stats",
    statsSubtitle = "Everything you’ve watched so far",
    episodesWatched = "Episodes watched",
    timeSpent = "Time spent watching",
    avgRating = "Average rating",
    rankTitle = "Your rank:",
    updatesTitle = "Updates",
    updatesChecking = "Checking APIs (Shikimori, Jikan, TMDB)...\nThis will take a moment.",
    updatesUpToDate = "You are up to date!",
    deleteTitle = "Delete title?",
    deleteSubtitle = "There’s no “undo”, no “Ctrl+Z”...",
    deleteConfirm = "Delete",
    favTitle = "Add to favorites?",
    favSubtitle = "Future you will thank you)",
    favConfirm = "Set Favorite",
    cancel = "Cancel",
    addTitle = "Add title",
    editTitle = "Edit title",
    animeTitleHint = "Anime Title",
    episodesHint = "Episodes Watched",
    addPhoto = "Add Photo",
    enterTitleToast = "Enter title",
    updatedToast = "Updated: ",
    languageName = "EN",
    settingsScreenTitle = "Settings",
    languageCardTitle = "Language",
    langRu = "Russian",
    langEn = "English",
    // THEME
    themeTitle = "Themes",
    themeLight = "Light",
    themeDark = "Dark",
    themeSystem = "System",

    checkForUpdateTitle = "App Update",
    checkButtonText = "Check Version",
    contactTitle = "Contact me",
    contactSubtitle = "Found a bug or have an idea?",

    genreAnime = "Anime",
    genreMovies = "Movies",
    genreSeries = "TV Series",
    filterByGenre = "By genres"
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
                    scope.launch {
                        overscrollAmountAnimatable.snapTo(calculateOverscroll(available))
                    }
                    return available
                }
                return super.onPreScroll(available, source)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                scope.launch {
                    overscrollAmountAnimatable.snapTo(calculateOverscroll(available))
                }
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
                    val predictedEndValue = exponentialDecay<Float>().calculateTargetValue(
                        initialValue = overscrollAmountAnimatable.value,
                        initialVelocity = availableVelocity,
                    )
                    if (predictedEndValue.sign == previousSign) {
                        overscrollAmountAnimatable.animateTo(
                            targetValue = 0f,
                            initialVelocity = availableVelocity,
                            animationSpec = animationSpec,
                        )
                    } else {
                        try {
                            overscrollAmountAnimatable.animateDecay(
                                initialVelocity = availableVelocity,
                                animationSpec = exponentialDecay()
                            ) {
                                if (value.sign != previousSign) {
                                    consumedVelocity -= velocity
                                    scope.launch {
                                        overscrollAmountAnimatable.snapTo(0f)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore interruption
                        }
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

                overscrollAmountAnimatable.animateTo(
                    targetValue = 0f,
                    initialVelocity = availableVelocity,
                    animationSpec = animationSpec
                )

                return available
            }
        }
    }

    return this
        .onSizeChanged {
            length = when (orientation) {
                Orientation.Vertical -> it.height.toFloat()
                Orientation.Horizontal -> it.width.toFloat()
            }
        }
        .nestedScroll(nestedScrollConnection)
}

// ==========================================
// NETWORK & DATA LAYER
// ==========================================

data class AnimeUpdate(
    val animeId: String,
    val title: String,
    val currentEpisodes: Int,
    val newEpisodes: Int,
    val source: String
)

// ДАННЫЕ О РЕЛИЗЕ С GITHUB
data class GithubReleaseInfo(
    val tagName: String,
    val htmlUrl: String,
    val downloadUrl: String
)

object ApiRateLimiter {
    private val mutex = Mutex()
    suspend fun <T> executeSafe(block: suspend () -> T): T {
        mutex.withLock {
            delay(1200)
            return block()
        }
    }
}

class AnimeRepository {
    private val tmdbKey = "4f4dc3cd35d58a551162eefe92ff549c"

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
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
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

val RateColor4 = Color(0xFFAEEA00) // Светлый салатовый (Lime Accent)
val RateColor5 = Color(0xFF2E7D32) // Насыщенный зеленый
val RateColorEmpty = Color(0xFF8E8E93)

// IOS-LIKE STATS COLORS
val EpisodesColor = Color(0xFF0A84FF)
val TimeColor = Color(0xFF5E5CE6)
val RatingColor = Color(0xFFFFC400) // НЕ КИСЛОТНЫЙ ЖЕЛТЫЙ
val RankColor = Color(0xFF43A047)   // Зеленый для ранга (Progress/Achievement)

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

val RatingChipBg = Color.Black.copy(alpha = 0.4f)

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
        darkColorScheme(
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            primary = BrandBlueSoft,
            onBackground = DarkTextPrimary,
            onSurface = DarkTextPrimary,
            secondary = DarkTextSecondary,
            outline = DarkBorder,
            error = BrandRed,
            surfaceContainer = Color(0xFF2C2C2E)
        )
    } else {
        lightColorScheme(
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            primary = BrandBlue,
            onBackground = LightTextPrimary,
            onSurface = LightTextPrimary,
            secondary = LightTextSecondary,
            outline = LightBorder,
            error = BrandRed,
            surfaceContainer = Color(0xFFFFFFFF)
        )
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

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            headlineMedium = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.onBackground,
                letterSpacing = (-0.5).sp
            ),
            headlineSmall = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.onBackground
            ),
            titleMedium = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                letterSpacing = 0.2.sp
            ),
            titleLarge = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = colors.secondary,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            bodySmall = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Light,
                fontSize = 12.sp,
                color = colors.secondary
            ),
            labelLarge = TextStyle(
                fontFamily = SnProFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        ),
        content = content
    )
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
    // НОВЫЕ ПОЛЯ ДЛЯ ЖАНРОВ И КАТЕГОРИЙ
    val tags: List<String> = emptyList(),
    val categoryType: String = "" // "Anime", "Movies", "Series"
)

data class RankedAnime(
    val anime: Anime,
    val score: Int
)

enum class SortOption {
    DATE_NEWEST,
    RATING_HIGH,
    AZ,
    FAVORITES;

    fun getLabel(strings: UiStrings): String = when(this) {
        DATE_NEWEST -> strings.newestFirst
        RATING_HIGH -> strings.highestRated
        AZ -> strings.nameAZ
        FAVORITES -> strings.favorites
    }
}

// СОСТОЯНИЯ ПРОВЕРКИ ОБНОВЛЕНИЯ ПРИЛОЖЕНИЯ
enum class AppUpdateStatus {
    IDLE, LOADING, NO_UPDATE, UPDATE_AVAILABLE, ERROR
}

// ВСПОМОГАТЕЛЬНЫЙ КЛАСС ДЛЯ SEMANTIC VERSIONING
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String // "Alpha", "Beta", "" (release)
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch

        // ЕСЛИ ЦИФРЫ РАВНЫ, СРАВНИВАЕМ СУФФИКСЫ
        // ПУСТОЙ СУФФИКС (RELEASE) ВСЕГДА БОЛЬШЕ, ЧЕМ ЛЮБОЙ НЕПУСТОЙ (ALPHA/BETA)
        if (suffix.isEmpty() && other.suffix.isNotEmpty()) return 1
        if (suffix.isNotEmpty() && other.suffix.isEmpty()) return -1
        if (suffix.isEmpty() && other.suffix.isEmpty()) return 0

        // СРАВНЕНИЕ ТИПОВ СУФФИКСОВ (ALPHA < BETA < RC)
        return suffix.compareTo(other.suffix, ignoreCase = true)
    }
}

class AnimeViewModel : ViewModel() {

    private data class LegacyAnime(
        val id: String,
        val title: String,
        val episodes: Int,
        val rating: Int,
        val imageFileName: String?,
        val orderIndex: Int,
        val dateAdded: Long,
        val isFavorite: Boolean,
        val tags: List<String>?,      // Nullable, чтобы принять отсутствие поля
        val categoryType: String?     // Nullable
    )

    var currentLanguage by mutableStateOf(AppLanguage.EN)
        private set

    // ТЕМА ПРИЛОЖЕНИЯ
    var currentTheme by mutableStateOf(AppTheme.SYSTEM)
        private set

    val strings: UiStrings get() = getStrings(currentLanguage)

    // СОСТОЯНИЕ ОБНОВЛЕНИЯ ПРИЛОЖЕНИЯ
    var updateStatus by mutableStateOf(AppUpdateStatus.IDLE)
        private set
    var latestDownloadUrl by mutableStateOf<String?>(null)
        private set
    var currentVersionName by mutableStateOf("v1.0.0")
        private set

    // СОСТОЯНИЕ АВАТАРКИ ПОЛЬЗОВАТЕЛЯ
    var userAvatarPath by mutableStateOf<String?>(null)
        private set

    // --- ФИЛЬТРАЦИЯ ПО ЖАНРАМ ---
    var filterSelectedTags by mutableStateOf<List<String>>(emptyList())
    var filterCategoryType by mutableStateOf("")
    var isGenreFilterVisible by mutableStateOf(false)

    private val SETTINGS_FILE = "settings.json"
    private val USER_AVATAR_FILE = "user_avatar.jpg" // ИМЯ ФАЙЛА АВАТАРА

    private fun getSettingsFile(): File = File(getRoot(), SETTINGS_FILE)

    fun setLanguage(lang: AppLanguage) {
        currentLanguage = lang
        saveSettings()
    }

    fun setAppTheme(theme: AppTheme) {
        currentTheme = theme
        saveSettings()
    }

    // ЛОГИКА СОХРАНЕНИЯ АВАТАРА ПОЛЬЗОВАТЕЛЯ
    fun saveUserAvatar(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(getImgDir(), USER_AVATAR_FILE)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    userAvatarPath = destFile.absolutePath
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadUserAvatar() {
        val f = File(getImgDir(), USER_AVATAR_FILE)
        if (f.exists()) {
            userAvatarPath = f.absolutePath
        }
    }

    // ИНИЦИАЛИЗАЦИЯ ВЕРСИИ ПРИЛОЖЕНИЯ ПРИ СТАРТЕ
    fun initAppVersion(context: Context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            currentVersionName = pInfo.versionName ?: "v1.0.0"
        } catch (e: Exception) {
            currentVersionName = "v1.0.0"
        }
    }

    // ЛОГИКА ПРОВЕРКИ ОБНОВЛЕНИЙ ПРИЛОЖЕНИЯ
    fun checkAppUpdate(context: Context) {
        if (updateStatus == AppUpdateStatus.LOADING) return
        updateStatus = AppUpdateStatus.LOADING

        if (currentVersionName == "v1.0.0") initAppVersion(context)
        val localVer = currentVersionName

        viewModelScope.launch {
            try {
                val release = repository.checkGithubUpdate()
                if (release != null) {
                    val remoteTag = release.tagName
                    if (isNewerVersion(localVer, remoteTag)) {
                        latestDownloadUrl = release.downloadUrl
                        updateStatus = AppUpdateStatus.UPDATE_AVAILABLE
                    } else {
                        updateStatus = AppUpdateStatus.NO_UPDATE
                    }
                } else {
                    updateStatus = AppUpdateStatus.ERROR
                    delay(2000)
                    updateStatus = AppUpdateStatus.IDLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateStatus = AppUpdateStatus.ERROR
                delay(2000)
                updateStatus = AppUpdateStatus.IDLE
            }
        }
    }

    private fun isNewerVersion(local: String, remote: String): Boolean {
        try {
            val localSem = parseVersion(local)
            val remoteSem = parseVersion(remote)
            return remoteSem > localSem
        } catch (e: Exception) {
            return false
        }
    }

    private fun parseVersion(versionStr: String): SemanticVersion {
        val clean = versionStr.removePrefix("v").trim()
        val dashSplit = clean.split("-", limit = 2)
        val numbersPart = dashSplit[0]
        val suffix = if (dashSplit.size > 1) dashSplit[1] else ""
        val dots = numbersPart.split(".").map { it.toIntOrNull() ?: 0 }
        val major = dots.getOrElse(0) { 0 }
        val minor = dots.getOrElse(1) { 0 }
        val patch = dots.getOrElse(2) { 0 }
        return SemanticVersion(major, minor, patch, suffix)
    }

    private fun saveSettings() {
        try {
            val settings = mapOf(
                "lang" to currentLanguage.name,
                "theme" to currentTheme.name
            )
            getSettingsFile().writeText(Gson().toJson(settings))
        } catch(e: Exception) { e.printStackTrace() }
    }

    fun loadSettings() {
        val f = getSettingsFile()
        if (f.exists()) {
            try {
                val json = f.readText()
                val map: Map<String, String> = Gson().fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
                currentLanguage = AppLanguage.valueOf(map["lang"] ?: "EN")

                // Загрузка темы (по умолчанию SYSTEM)
                val themeStr = map["theme"] ?: "SYSTEM"
                currentTheme = try {
                    AppTheme.valueOf(themeStr)
                } catch (e: Exception) { AppTheme.SYSTEM }

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

    private fun getRoot(): File {
        val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val f = File(d, ROOT); if (!f.exists()) f.mkdirs(); return f
    }
    private fun getImgDir(): File { val f = File(getRoot(), IMG_DIR); if(!f.exists()) f.mkdirs(); return f }
    private fun getDataFile(): File = File(getRoot(), FILE_NAME)
    private fun getIgnoredFile(): File = File(getRoot(), IGNORED_FILE_NAME)

    // ==========================================
    // СУПЕР-НАДЕЖНАЯ ЗАГРУЗКА (Ручной парсинг)
    // ==========================================
    fun loadAnime() {
        val f = getDataFile()
        if (!f.exists()) return

        try {
            val json = f.readText()
            if (json.isBlank()) return

            // 1. Парсим как сырой элемент, не привязываясь к классу
            val jsonElement = JsonParser.parseString(json)

            val restoredList = mutableListOf<Anime>()

            if (jsonElement.isJsonArray) {
                val array = jsonElement.asJsonArray

                // 2. Проходимся по каждому объекту вручную
                array.forEach { element ->
                    try {
                        val obj = element.asJsonObject

                        // 3. Безопасно извлекаем поля. Если поля нет - берем дефолт.
                        val id = if (obj.has("id")) obj.get("id").asString else UUID.randomUUID().toString()
                        val title = if (obj.has("title")) obj.get("title").asString else "Unknown Title"

                        // Защита для числовых полей
                        val episodes = if (obj.has("episodes") && !obj.get("episodes").isJsonNull) obj.get("episodes").asInt else 0
                        val rating = if (obj.has("rating") && !obj.get("rating").isJsonNull) obj.get("rating").asInt else 0
                        val orderIndex = if (obj.has("orderIndex") && !obj.get("orderIndex").isJsonNull) obj.get("orderIndex").asInt else 0
                        val dateAdded = if (obj.has("dateAdded") && !obj.get("dateAdded").isJsonNull) obj.get("dateAdded").asLong else System.currentTimeMillis()

                        val imageFileName = if (obj.has("imageFileName") && !obj.get("imageFileName").isJsonNull) obj.get("imageFileName").asString else null
                        val isFavorite = if (obj.has("isFavorite") && !obj.get("isFavorite").isJsonNull) obj.get("isFavorite").asBoolean else false

                        // 4. ВОТ ЗДЕСЬ БЫЛА ПРОБЛЕМА РАНЬШЕ:
                        // Аккуратно читаем теги
                        val tags = mutableListOf<String>()
                        if (obj.has("tags") && obj.get("tags").isJsonArray) {
                            obj.get("tags").asJsonArray.forEach { tagElement ->
                                tags.add(tagElement.asString)
                            }
                        }

                        // Аккуратно читаем категорию
                        val categoryType = if (obj.has("categoryType") && !obj.get("categoryType").isJsonNull) obj.get("categoryType").asString else ""

                        // 5. Собираем объект
                        restoredList.add(Anime(
                            id = id,
                            title = title,
                            episodes = episodes,
                            rating = rating,
                            imageFileName = imageFileName,
                            orderIndex = orderIndex,
                            dateAdded = dateAdded,
                            isFavorite = isFavorite,
                            tags = tags,
                            categoryType = categoryType
                        ))

                    } catch (e: Exception) {
                        // Если один конкретный аниме сломан, мы его пропускаем,
                        // но не роняем весь список.
                        e.printStackTrace()
                    }
                }
            }

            // 6. Обновляем UI
            _animeList.clear()
            _animeList.addAll(restoredList.sortedBy { it.orderIndex })

            // 7. Сразу перезаписываем файл в правильном формате,
            // чтобы в следующий раз всё работало штатно.
            if (_animeList.isNotEmpty()) {
                save()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // Можно добавить Toast здесь, чтобы увидеть ошибку на экране
        }

        // Остальная логика (аватар, обновления)
        loadUserAvatar()

        // Загрузка ignored
        val fIgnored = getIgnoredFile()
        if (fIgnored.exists()) {
            try {
                val ignoredJson = fIgnored.readText()
                val map: Map<String, Int> = Gson().fromJson(ignoredJson, object : TypeToken<Map<String, Int>>() {}.type) ?: emptyMap()
                ignoredUpdatesMap.putAll(map)
            } catch (e: Exception) { e.printStackTrace() }
        }

        needsUpdateCheck = true
        checkForUpdates()
    }

    fun toggleFavorite(id: String) {
        val idx = _animeList.indexOfFirst { it.id == id }
        if (idx != -1) {
            val item = _animeList[idx]
            _animeList[idx] = item.copy(isFavorite = !item.isFavorite)
            save()
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        if (!force && !needsUpdateCheck && _updates.isEmpty()) return
        if (isCheckingUpdates) return
        isCheckingUpdates = true

        viewModelScope.launch {
            try {
                val newUpdates = mutableListOf<AnimeUpdate>()
                val currentList = _animeList.toList()

                currentList.forEach { anime ->
                    val result = repository.findTotalEpisodes(anime.title)
                    if (result != null) {
                        val (remoteEps, source) = result
                        val isIgnored = ignoredUpdatesMap[anime.id] == remoteEps
                        if (remoteEps > anime.episodes && !isIgnored) {
                            newUpdates.add(AnimeUpdate(anime.id, anime.title, anime.episodes, remoteEps, source))
                        }
                    }
                }
                _updates.clear()
                _updates.addAll(newUpdates)
                if (_updates.isEmpty()) needsUpdateCheck = false
            } catch (e: Exception) { e.printStackTrace() }
            finally { isCheckingUpdates = false }
        }
    }

    fun acceptUpdate(update: AnimeUpdate, ctx: Context) {
        val anime = getAnimeById(update.animeId) ?: return
        // При обновлении сохраняем существующие теги
        updateAnime(ctx, anime.id, anime.title, update.newEpisodes, anime.rating, null, anime.tags, anime.categoryType)
        _updates.remove(update)
    }

    fun dismissUpdate(update: AnimeUpdate) {
        ignoredUpdatesMap[update.animeId] = update.newEpisodes
        saveIgnored()
        _updates.remove(update)
        if (_updates.isEmpty()) needsUpdateCheck = false
    }

    fun getAnimeById(id: String): Anime? = _animeList.find { it.id == id }

    fun addAnime(ctx: Context, title: String, ep: Int, rate: Int, uri: Uri?, tags: List<String>, categoryType: String) {
        val id = UUID.randomUUID().toString()
        val img = if (uri != null) saveImg(ctx, uri, id) else null
        _animeList.add(0, Anime(
            id = id,
            title = title,
            episodes = ep,
            rating = rate,
            imageFileName = img,
            orderIndex = (_animeList.maxOfOrNull { it.orderIndex }?:0)+1,
            tags = tags,
            categoryType = categoryType
        ))
        save()
        needsUpdateCheck = true
    }

    fun updateAnime(ctx: Context, id: String, title: String, ep: Int, rate: Int, uri: Uri?, tags: List<String>, categoryType: String) {
        val idx = _animeList.indexOfFirst { it.id == id }
        if (idx == -1) return
        var img = _animeList[idx].imageFileName
        if (uri != null) {
            val newImg = saveImg(ctx, uri, id)
            if (newImg != null) {
                _animeList[idx].imageFileName?.let { File(getImgDir(), it).delete() }
                img = newImg
            }
        }
        _animeList[idx] = _animeList[idx].copy(
            title=title,
            episodes=ep,
            rating=rate,
            imageFileName=img,
            tags = tags,
            categoryType = categoryType
        )
        save()
        needsUpdateCheck = true
        if (ignoredUpdatesMap.containsKey(id)) {
            ignoredUpdatesMap.remove(id)
            saveIgnored()
        }
    }

    fun deleteAnime(id: String) {
        val anime = _animeList.find { it.id == id } ?: return
        anime.imageFileName?.let { File(getImgDir(), it).delete() }
        _animeList.remove(anime)
        save()
        needsUpdateCheck = true
    }

    private fun save() { try { getDataFile().writeText(Gson().toJson(_animeList)) } catch(e:Exception){e.printStackTrace()} }
    private fun saveIgnored() { try { getIgnoredFile().writeText(Gson().toJson(ignoredUpdatesMap)) } catch(e:Exception){e.printStackTrace()} }

    private fun saveImg(ctx: Context, uri: Uri, id: String): String? {
        return try {
            val name = "img_${id}_${System.currentTimeMillis()}.jpg"
            ctx.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(File(getImgDir(), name)).use { o -> i.copyTo(o) } }
            name
        } catch(e: Exception) { null }
    }

    fun getImgPath(name: String?): File? = if(name!=null) File(getImgDir(), name).let { if(it.exists()) it else null } else null

    fun getDisplayList(): List<Anime> {
        val rawQuery = searchQuery.trim()

        // --- ЛОГИКА ФИЛЬТРАЦИИ ПО ЖАНРАМ (ШАГ 1) ---
        // ЕСЛИ 1 ЖАНР -> OR (ПРИСУТСТВУЕТ ХОТЯ БЫ ОДИН) - но containsAll для списка из 1 элемента тоже работает как "contains"
        // ЕСЛИ 2+ ЖАНРА -> AND (ПРИСУТСТВУЮТ ВСЕ ВЫБРАННЫЕ)
        val filteredByGenre = if (filterSelectedTags.isEmpty()) _animeList else {
            _animeList.filter { anime ->
                anime.tags.containsAll(filterSelectedTags)
            }
        }

        if (rawQuery.isBlank()) return sortList(filteredByGenre)

        val normalizedQuery = rawQuery.lowercase()
        val rankedList = filteredByGenre.mapNotNull { anime ->
            val score = calculateRelevanceScore(normalizedQuery, anime.title.lowercase())
            if (score > 0) RankedAnime(anime, score) else null
        }
        return rankedList.sortedWith(
            compareByDescending<RankedAnime> { it.score }
                .thenBy { it.anime.title }
        ).map { it.anime }
    }

    private fun calculateRelevanceScore(query: String, title: String): Int {
        if (title == query) return 100
        if (title.startsWith(query)) return 90
        val words = title.split(" ", "-", ":")
        if (words.any { it.startsWith(query) }) return 80
        if (title.contains(query)) return 60
        if (query.length > 2) {
            val dist = levenshtein(query, title)
            val maxEdits = if (query.length < 6) 1 else 2
            val prefix = title.take(query.length + 1)
            val distPrefix = levenshtein(query, prefix)
            if (dist <= maxEdits) return 50
            if (distPrefix <= maxEdits) return 45
        }
        return 0
    }

    private fun sortList(list: List<Anime>): List<Anime> {
        return when(sortOption) {
            SortOption.DATE_NEWEST -> list.sortedByDescending { it.dateAdded }
            SortOption.RATING_HIGH -> list.sortedByDescending { it.rating }
            SortOption.AZ -> list.sortedBy { it.title }
            SortOption.FAVORITES -> list.filter { it.isFavorite }.sortedBy { it.title }
        }
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) return 0
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost; cost = newCost; newCost = swap
        }
        return cost[lhsLength]
    }
}

// ==========================================
// UI COMPONENTS
// ==========================================

fun performHaptic(view: View, type: String) {
    when (type) {
        "success" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        "warning" -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        "light" -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}

fun performHaptic(view: View, isStrong: Boolean) {
    performHaptic(view, if (isStrong) "warning" else "light")
}

val StarIconVector: ImageVector get() = ImageVector.Builder(name = "Star", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).path(fill = SolidColor(Color.Black), fillAlpha = 1f, stroke = null, strokeAlpha = 1f, pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero) { moveTo(8.243f, 7.34f); lineTo(1.863f, 8.265f); lineTo(1.75f, 8.288f); arcTo(1f, 1f, 0f, false, false, 1.31f, 9.972f); lineTo(5.932f, 14.471f); lineTo(4.842f, 20.826f); lineTo(4.829f, 20.936f); arcTo(1f, 1f, 0f, false, false, 6.293f, 21.88f); lineTo(12.0f, 18.88f); lineTo(17.693f, 21.88f); lineTo(17.793f, 21.926f); arcTo(1f, 1f, 0f, false, false, 19.145f, 20.826f); lineTo(18.054f, 14.471f); lineTo(22.678f, 9.971f); lineTo(22.756f, 9.886f); arcTo(1f, 1f, 0f, false, false, 22.123f, 8.266f); lineTo(15.743f, 7.34f); lineTo(12.891f, 1.56f); arcTo(1f, 1f, 0f, false, false, 11.097f, 1.56f); lineTo(8.243f, 7.34f); close() }.build()

@Composable
fun StarRatingBar(rating: Int, onRatingChanged: (Int) -> Unit) {
    val colors = listOf(RateColor1, RateColor2, RateColor3, RateColor4, RateColor5)
    val activeColor = if (rating > 0) colors[rating - 1] else RateColorEmpty
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            val isSelected = i <= rating
            val animatedColor by animateColorAsState(targetValue = if (isSelected) activeColor else RateColorEmpty, animationSpec = tween(300), label = "color")
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
    FloatingActionButton(
        onClick = {
            if (isEnabled && !isSaved) {
                isSaved = true
                performHaptic(view, "success")
                onClick()
            }
        },
        containerColor = if (isEnabled) MaterialTheme.colorScheme.primary else Color(0xFF424242),
        contentColor = if (isEnabled) Color.White else Color.Gray,
        shape = CircleShape,
        modifier = Modifier.size(64.dp)
    ) {
        AnimatedContent(targetState = isSaved, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "save_animation") { saved ->
            if (saved) Icon(imageVector = Icons.Default.Check, contentDescription = "Saved", modifier = Modifier.size(28.dp))
            else Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(28.dp))
        }
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
            AnimatedContent(targetState = isCopied, transitionSpec = { (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut()) }, label = "copy_icon") { copied ->
                if (copied) { LaunchedEffect(Unit) { delay(2000); isCopied = false }; Icon(Icons.Default.Check, null, tint = BrandBlue, modifier = Modifier.size(24.dp)) }
                else Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.TextAsIndividualLetters(
    animatedContentScope: AnimatedContentScope,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(),
    textColor: Color
) {
    Row(modifier) {
        text.forEachIndexed { index, letter ->
            Text(
                text = "$letter",
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "hint_$index"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = { _, _ ->
                        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 1000f)
                    }
                ),
                style = style,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AnimatedOneUiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val showHintAbove = isFocused || value.isNotEmpty()
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.secondary
    val textStyle = TextStyle(fontSize = 18.sp, color = textColor, fontFamily = SnProFamily)
    val hintStyleInner = TextStyle(fontSize = 18.sp, fontFamily = SnProFamily)
    val hintStyleOuter = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = SnProFamily)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        interactionSource = interactionSource,
        textStyle = textStyle,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        decorationBox = { innerTextField ->
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = showHintAbove,
                    transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
                    label = "hintAnimation"
                ) { targetShowAbove ->
                    if (targetShowAbove) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(start = 24.dp, bottom = 4.dp).height(16.dp)) {
                                TextAsIndividualLetters(animatedContentScope = this@AnimatedContent, text = placeholder, style = hintStyleOuter, textColor = MaterialTheme.colorScheme.primary)
                            }
                            Box(modifier = Modifier.fillMaxWidth().sharedElement(rememberSharedContentState(key = "container"), animatedVisibilityScope = this@AnimatedContent).clip(RoundedCornerShape(20.dp)).background(containerColor).padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
                                innerTextField()
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.fillMaxWidth().sharedElement(rememberSharedContentState(key = "container"), animatedVisibilityScope = this@AnimatedContent).clip(RoundedCornerShape(20.dp)).background(containerColor).padding(horizontal = 24.dp, vertical = 16.dp), contentAlignment = Alignment.CenterStart) {
                                TextAsIndividualLetters(animatedContentScope = this@AnimatedContent, text = placeholder, style = hintStyleInner, textColor = hintColor)
                                innerTextField()
                            }
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// NEUMORPHIC RATING CHIP
// ==========================================
@Composable
fun RatingChip(rating: Int) {
    val starTint = getRatingColor(rating)
    val isDark = isSystemInDarkTheme()
    val bgColor = MaterialTheme.colorScheme.background.copy(alpha = if (isDark) 0.3f else 0.35f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f)

    Box(
        modifier = Modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp), spotColor = shadowColor)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
fun ThemeOptionItem(
    label: String,
    isSelected: Boolean,
    themeType: AppTheme,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) BrandBlue else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val textColor = if (isSelected) BrandBlue else MaterialTheme.colorScheme.onSurface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    ) {
        // Visual Preview Box
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
        ) {
            ThemePreviewCanvas(themeType)

            // Selected Checkmark Overlay
            if (isSelected) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BrandBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ThemePreviewCanvas(type: AppTheme) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        when (type) {
            AppTheme.LIGHT -> {
                // Background
                drawRect(color = LightBackground)
                // Mock Card
                drawRoundRect(
                    color = LightSurface,
                    topLeft = Offset(0f, size.height * 0.6f),
                    size = size.copy(height = size.height * 0.4f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            AppTheme.DARK -> {
                // Background
                drawRect(color = DarkBackground)
                // Mock Card
                drawRoundRect(
                    color = DarkSurfaceVariant,
                    topLeft = Offset(0f, size.height * 0.6f),
                    size = size.copy(height = size.height * 0.4f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
            AppTheme.SYSTEM -> {
                // Grey Background
                drawRect(color = Color(0xFFB0B0B0))

                // Diagonal Stripes
                val stripeWidth = 10f
                val gap = 10f
                var x = -size.height // Start off-screen to cover diagonal
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(x, size.height),
                        end = Offset(x + size.height, 0f),
                        strokeWidth = stripeWidth
                    )
                    x += (stripeWidth + gap)
                }
            }
        }
        // Outer border for definition (subtle)
        drawRoundRect(
            color = Color.Gray.copy(alpha = 0.3f),
            style = Stroke(width = 1f),
            cornerRadius = CornerRadius(30f, 30f) // Matches clip
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun OneUiAnimeCard(
    anime: Anime,
    viewModel: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    val imageFile = remember(anime.imageFileName) { viewModel.getImgPath(anime.imageFileName) }
    val view = LocalView.current
    val cardShape = RoundedCornerShape(24.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline

    // --- CONTRAST CHIP COLORS (Логика инверсии) ---
    val isDark = isSystemInDarkTheme()
    val chipBgColor = if (isDark) Color(0xFFE0E0E0) else Color(0xFF303030) // Светлый на темном, темный на светлом
    val chipTextColor = if (isDark) Color.Black else Color.White

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp)
                .sharedBounds(rememberSharedContentState(key = "container_${anime.id}"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds, placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize)
                .shadow(elevation = 12.dp, shape = cardShape, spotColor = Color.Black.copy(alpha = 0.08f), ambientColor = Color.Black.copy(alpha = 0.04f))
                .background(brush = Brush.verticalGradient(colors = listOf(surfaceColor, surfaceVariant)), shape = cardShape)
                .border(width = 1.dp, color = borderColor, shape = cardShape)
                .clip(cardShape)
                .clickable { performHaptic(view, "light"); onClick() }
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.aspectRatio(1f).fillMaxHeight().sharedElement(rememberSharedContentState(key = "image_${anime.id}"), animatedVisibilityScope = animatedVisibilityScope).clip(RoundedCornerShape(18.dp)).background(Color.Black)) {
                    if (imageFile != null) AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(imageFile).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(Color(0xFF2C2C2E))) { Text(text = anime.title.take(1).uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray) }
                }
                Spacer(Modifier.width(16.dp))
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = anime.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, lineHeight = 20.sp, modifier = Modifier.weight(1f, false))
                            if (anime.isFavorite) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Star, contentDescription = "Favorite", tint = RateColor3, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(text = "${anime.episodes} episodes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                        // --- ОТОБРАЖЕНИЕ ЖАНРОВ С КОНТРАСТНЫМ ФОНОМ ---
                        if (anime.tags.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                maxItemsInEachRow = 3
                            ) {
                                anime.tags.take(3).forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(chipBgColor) // Применяем инвертированный фон
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = SnProFamily,
                                                color = chipTextColor // Применяем инвертированный текст
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (anime.rating > 0) RatingChip(rating = anime.rating)
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

    fun dismiss() {
        visible = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onCancel()
        }, 300)
    }

    BackHandler { dismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { dismiss() }
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(200)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (imageFile != null) {
                    Box(
                        modifier = Modifier
                            .height(280.dp)
                            .aspectRatio(0.7f)
                            .shadow(elevation = 16.dp, shape = RoundedCornerShape(20.dp), spotColor = accentColor)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Gray)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.2f)))))
                    }
                    Spacer(Modifier.height(24.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        performHaptic(view, "success")
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = confirmText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        performHaptic(view, "light")
                        dismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(text = cancelText, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// ==========================================
// GENRE FILTER SHEET
// ==========================================
@Composable
fun GenreFilterSheet(
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(Unit) { visible = true }

    fun triggerDismiss() {
        visible = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onDismiss()
        }, 300)
    }

    val onTagToggle: (String, String) -> Unit = { tag, categoryType ->
        val currentTags = viewModel.filterSelectedTags.toMutableList()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
            if (currentTags.isEmpty()) {
                viewModel.filterCategoryType = ""
            }
        } else {
            if (currentTags.size < 3 && (viewModel.filterCategoryType.isEmpty() || viewModel.filterCategoryType == categoryType)) {
                currentTags.add(tag)
                viewModel.filterCategoryType = categoryType
            }
        }
        viewModel.filterSelectedTags = currentTags
        performHaptic(view, "light")
    }

    BackHandler { triggerDismiss() }

    Box(
        modifier = Modifier.fillMaxSize().zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { triggerDismiss() }
            )
        }

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
            ) {
                Text(
                    text = viewModel.strings.filterByGenre,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                GenreSelectionSection(
                    selectedTags = viewModel.filterSelectedTags,
                    activeCategory = viewModel.filterCategoryType,
                    onTagToggle = onTagToggle
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { triggerDismiss() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// STATS OVERLAY
// ==========================================
@Composable
fun StatsOverlay(
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    fun triggerDismiss() {
        visible = false
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onDismiss()
        }, 300)
    }

    BackHandler { triggerDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        // SCRIM
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { triggerDismiss() }
            )
        }

        // CONTENT
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = 0.85f, // iOS-like
                    stiffness = 450f
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200) // EXIT НЕ ТРОГАЕМ
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp) // Чуть шире чем диалог
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp) // Отступ снизу
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.background) // Фон приложения
                    .padding(top = 24.dp) // Отступ внутри
            ) {
                WatchStatsContent(animeList = viewModel.animeList, viewModel = viewModel)
            }
        }
    }
}


@Composable
fun MalistWorkspaceTopBar(
    viewModel: AnimeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.saveUserAvatar(context, uri)
            }
        }
    )

    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).statusBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (viewModel.userAvatarPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(viewModel.userAvatarPath!!))
                        .crossfade(true)
                        .build(),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("M", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = viewModel.strings.appName, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

// ==========================================
// STATS CARD (UPDATED WITH COLORS)
// ==========================================
@Composable
fun StatsCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onClick: () -> Unit,
    iconTint: Color? = null,
    iconBg: Color? = null,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(20.dp)

    val finalIconTint = iconTint ?: MaterialTheme.colorScheme.onSecondaryContainer
    val finalIconBg = iconBg ?: MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.08f))
            .clip(shape)
            .background(brush = Brush.verticalGradient(colors = listOf(surfaceColor, surfaceVariant)))
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable { onClick() }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(finalIconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = finalIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor, fontWeight = FontWeight.SemiBold)
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) { content() }
            }
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

        // 1. EPISODES (BLUE)
        StatsCard(
            title = s.episodesWatched,
            icon = Icons.Default.Visibility,
            isExpanded = expandedIndex == 0,
            iconTint = EpisodesColor,
            iconBg = EpisodesColor.copy(alpha = 0.15f),
            onClick = { expandedIndex = if (expandedIndex == 0) -1 else 0 }
        ) {
            Text(text = "$totalEpisodes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 2. TIME (INDIGO/PURPLE)
        StatsCard(
            title = s.timeSpent,
            icon = Icons.Default.Schedule,
            isExpanded = expandedIndex == 1,
            iconTint = TimeColor,
            iconBg = TimeColor.copy(alpha = 0.15f),
            onClick = { expandedIndex = if (expandedIndex == 1) -1 else 1 }
        ) {
            Column {
                Text(text = "$formattedMinutes min", style = MaterialTheme.typography.titleLarge, color = textColor)
                Text(text = "$formattedHours h", style = MaterialTheme.typography.titleLarge, color = secColor)
                Text(text = "$formattedDays days", style = MaterialTheme.typography.titleLarge, color = BrandBlue.copy(alpha = 0.6f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 3. RATING (YELLOW/AMBER)
        StatsCard(
            title = s.avgRating,
            icon = Icons.Default.Star,
            isExpanded = expandedIndex == 2,
            iconTint = RatingColor,
            iconBg = RatingColor.copy(alpha = 0.18f),
            onClick = { expandedIndex = if (expandedIndex == 2) -1 else 2 }
        ) {
            Text(text = String.format("%.1f / 5⭐", avgRating), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // 4. RANK (GREEN - ОБНОВЛЕННЫЙ ЦВЕТ)
        StatsCard(
            title = s.rankTitle,
            icon = Icons.Default.MilitaryTech,
            isExpanded = expandedIndex == 3,
            iconTint = RankColor, // ТЕПЕРЬ ЗЕЛЕНЫЙ
            iconBg = RankColor.copy(alpha = 0.16f),
            onClick = { expandedIndex = if (expandedIndex == 3) -1 else 3 }
        ) {
            Text(text = rankName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = rankColor)
        }
    }
}

// ==========================================
// SWIPE LOGIC & COMPOSABLES
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeBackground(dismissState: SwipeToDismissBoxState) {
    val direction = dismissState.dismissDirection

    val color by animateColorAsState(
        when (direction) {
            SwipeToDismissBoxValue.EndToStart -> BrandRed
            SwipeToDismissBoxValue.StartToEnd -> RateColor3
            else -> Color.Transparent
        }, label = "swipeColor"
    )

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Star
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        else -> Icons.Default.Delete
    }

    val scale by animateFloatAsState(
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) 1.2f else 0.8f,
        label = "iconScale"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (color != Color.Transparent) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.scale(scale),
                tint = Color.White
            )
        }
    }
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

    BackHandler(enabled = isSearchVisible || vm.searchQuery.isNotEmpty()) {
        if (isSearchVisible) { performHaptic(view, "light"); isSearchVisible = false; vm.searchQuery = ""; focusManager.clearFocus(); kbd?.hide() }
    }

    val listState = rememberLazyListState()
    var overscrollAmount by remember { mutableFloatStateOf(0f) }

    val isHeaderFloating by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 } }
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val bgColor = MaterialTheme.colorScheme.background

    val shouldBlur = (isSearchVisible && vm.searchQuery.isBlank()) || showCSheet || animeToDelete != null || animeToFavorite != null || vm.isGenreFilterVisible
    val blurAmount by animateDpAsState(targetValue = if (shouldBlur) 10.dp else 0.dp, label = "blur")

    Scaffold(containerColor = Color.Transparent, bottomBar = {}, floatingActionButton = {}) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(bgColor))

            Box(modifier = Modifier.zIndex(6f).align(Alignment.TopEnd).padding(end = 16.dp)) {
                GlassActionDock(
                    hazeState = hazeState,
                    isFloating = isHeaderFloating,
                    sortOption = vm.sortOption,
                    viewModel = vm,
                    onSortSelected = { sort -> performHaptic(view, "light"); vm.sortOption = sort },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Column(modifier = Modifier.fillMaxSize().blur(blurAmount)) {
                Box(modifier = Modifier.fillMaxSize().weight(1f).background(bgColor)) {
                    val list = vm.getDisplayList()
                    if (list.isEmpty()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            MalistWorkspaceTopBar(viewModel = vm)
                            Box(modifier = Modifier.weight(1f)) {
                                EmptyStateView(
                                    title = if (vm.searchQuery.isNotEmpty()) vm.strings.noResults else if (vm.sortOption == SortOption.FAVORITES) vm.strings.noFavorites else vm.strings.emptyTitle,
                                    subtitle = if (vm.searchQuery.isNotEmpty()) "" else if (vm.sortOption == SortOption.FAVORITES) "" else vm.strings.emptySubtitle
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .customOverscroll(
                                    listState = listState,
                                    onNewOverscrollAmount = { overscrollAmount = it }
                                )
                                .offset { IntOffset(0, overscrollAmount.roundToInt()) }
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(top = 0.dp, bottom = 120.dp, start = 0.dp, end = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.haze(state = hazeState)
                            ) {
                                item {
                                    MalistWorkspaceTopBar(viewModel = vm)
                                }
                                items(list, key = { it.id }) { anime ->

                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = {
                                            when(it) {
                                                SwipeToDismissBoxValue.StartToEnd -> {
                                                    performHaptic(view, "success")
                                                    animeToFavorite = anime
                                                    false
                                                }
                                                SwipeToDismissBoxValue.EndToStart -> {
                                                    performHaptic(view, "warning")
                                                    animeToDelete = anime
                                                    false
                                                }
                                                else -> false
                                            }
                                        },
                                        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = { SwipeBackground(dismissState) },
                                        modifier = Modifier.padding(horizontal = 16.dp).animateItem()
                                    ) {
                                        OneUiAnimeCard(
                                            anime = anime,
                                            viewModel = vm,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onClick = { nav.navigate("add_anime?animeId=${anime.id}") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSearchVisible && animeToDelete == null && animeToFavorite == null) {
                GlassBottomNavigation(
                    hazeState = hazeState,
                    nav = nav,
                    viewModel = vm,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onShowStats = { showCSheet = true },
                    onShowNotifs = { /* NO-OP, handled in GlassActionDock */ },
                    onSearchClick = {
                        performHaptic(view, "light")
                        isSearchVisible = !isSearchVisible
                        if (!isSearchVisible) {
                            vm.searchQuery = ""
                            focusManager.clearFocus()
                            kbd?.hide()
                        }
                    },
                    isSearchActive = isSearchVisible,
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f).navigationBarsPadding()
                )
            }

            AnimatedVisibility(
                visible = isSearchVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).windowInsetsPadding(WindowInsets.ime).padding(bottom = 16.dp).zIndex(10f)
            ) {
                SimpGlassCard(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    BasicTextField(
                        value = vm.searchQuery,
                        onValueChange = { vm.searchQuery = it; if (it.isNotEmpty()) performHaptic(view, "light") },
                        modifier = Modifier.fillMaxSize().focusRequester(searchFocusRequester).padding(horizontal = 20.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SnProFamily),
                        cursorBrush = SolidColor(BrandBlue),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = BrandBlue)
                                Spacer(Modifier.width(12.dp))
                                Box {
                                    if (vm.searchQuery.isEmpty()) { Text(vm.strings.searchPlaceholder, color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp, fontFamily = SnProFamily) }
                                    innerTextField()
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { kbd?.hide() })
                    )
                }
                LaunchedEffect(Unit) { searchFocusRequester.requestFocus(); kbd?.show() }
            }

            if (shouldBlur && animeToDelete == null && animeToFavorite == null && !vm.isGenreFilterVisible) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focusManager.clearFocus(); isSearchVisible = false; kbd?.hide() }.zIndex(2f))
            }

            if (animeToDelete != null) {
                SpringBottomDialog(
                    title = vm.strings.deleteTitle,
                    subtitle = vm.strings.deleteSubtitle,
                    confirmText = vm.strings.deleteConfirm,
                    cancelText = vm.strings.cancel,
                    icon = Icons.Default.Delete,
                    accentColor = BrandRed,
                    imageFile = vm.getImgPath(animeToDelete?.imageFileName),
                    onConfirm = {
                        vm.deleteAnime(animeToDelete!!.id)
                        animeToDelete = null
                    },
                    onCancel = { animeToDelete = null }
                )
            }

            if (animeToFavorite != null) {
                SpringBottomDialog(
                    title = vm.strings.favTitle,
                    subtitle = vm.strings.favSubtitle,
                    confirmText = vm.strings.favConfirm,
                    cancelText = vm.strings.cancel,
                    icon = Icons.Default.Star,
                    accentColor = RateColor3,
                    imageFile = vm.getImgPath(animeToFavorite?.imageFileName),
                    onConfirm = {
                        vm.toggleFavorite(animeToFavorite!!.id)
                        animeToFavorite = null
                    },
                    onCancel = { animeToFavorite = null }
                )
            }

            AnimatedVisibility(
                visible = showScrollToTop && !isSearchVisible && animeToDelete == null && animeToFavorite == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 160.dp, end = 24.dp)
                    .zIndex(1f)
            ) {
                SimpGlassCard(
                    hazeState = hazeState,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(44.dp)
                        .clickable {
                            performHaptic(view, "light")
                            scope.launch { listState.animateScrollToItem(0) }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showCSheet) {
            StatsOverlay(
                viewModel = vm,
                onDismiss = { showCSheet = false }
            )
        }

        if (vm.isGenreFilterVisible) {
            GenreFilterSheet(
                viewModel = vm,
                onDismiss = { vm.isGenreFilterVisible = false }
            )
        }
    }
}

// ==========================================
// НОВЫЕ КОМПОНЕНТЫ ВЫБОРА ЖАНРОВ
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreSelectionSection(
    selectedTags: List<String>,
    activeCategory: String, // "Anime", "Movies", "Series" или пустая строка
    onTagToggle: (String, String) -> Unit // (tag, categoryType)
) {
    val animeGenres = listOf("Shonen", "Seinen", "Isekai", "Mecha", "Slice of Life", "Action", "Romance", "Fantasy", "Drama", "Comedy", "Horror", "Sports")
    val movieGenres = listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Sci-Fi", "Thriller", "Western", "Animation", "Documentary")
    val seriesGenres = listOf("Drama", "Sitcom", "Thriller", "Fantasy", "Sci-Fi", "Crime", "Mystery", "Action", "Adventure", "Reality")

    var expandedCategory by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // КАРТОЧКА АНИМЕ
        ExpandableCategoryCard(
            title = "Anime",
            icon = Icons.Outlined.Animation,
            genres = animeGenres,
            categoryType = "Anime",
            activeCategory = activeCategory,
            selectedTags = selectedTags,
            isExpanded = expandedCategory == "Anime",
            onExpandToggle = { expandedCategory = if (expandedCategory == "Anime") null else "Anime" },
            onTagToggle = onTagToggle
        )

        // КАРТОЧКА ФИЛЬМОВ
        ExpandableCategoryCard(
            title = "Movies",
            icon = Icons.Outlined.Movie,
            genres = movieGenres,
            categoryType = "Movies",
            activeCategory = activeCategory,
            selectedTags = selectedTags,
            isExpanded = expandedCategory == "Movies",
            onExpandToggle = { expandedCategory = if (expandedCategory == "Movies") null else "Movies" },
            onTagToggle = onTagToggle
        )

        // КАРТОЧКА СЕРИАЛОВ
        ExpandableCategoryCard(
            title = "TV Series",
            icon = Icons.Outlined.Tv,
            genres = seriesGenres,
            categoryType = "Series",
            activeCategory = activeCategory,
            selectedTags = selectedTags,
            isExpanded = expandedCategory == "Series",
            onExpandToggle = { expandedCategory = if (expandedCategory == "Series") null else "Series" },
            onTagToggle = onTagToggle
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandableCategoryCard(
    title: String,
    icon: ImageVector,
    genres: List<String>,
    categoryType: String,
    activeCategory: String,
    selectedTags: List<String>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onTagToggle: (String, String) -> Unit
) {
    // БЛОКИРОВКА: ЕСЛИ ВЫБРАНА ДРУГАЯ КАТЕГОРИЯ, ЭТА СТАНОВИТСЯ НЕДОСТУПНОЙ
    val isLocked = activeCategory.isNotEmpty() && activeCategory != categoryType
    val alpha = if (isLocked) 0.5f else 1f

    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, if(isExpanded) BrandBlue else borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked) { onExpandToggle() }
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = if(isExpanded) BrandBlue else MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(12.dp))
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if(isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    genres.forEach { genre ->
                        val isSelected = selectedTags.contains(genre)
                        GenreChip(
                            text = genre,
                            isSelected = isSelected,
                            onClick = { onTagToggle(genre, categoryType) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GenreChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) BrandBlue else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderStroke = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(50)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AddEditScreen(nav: NavController, vm: AnimeViewModel, id: String?, sharedTransitionScope: SharedTransitionScope, animatedVisibilityScope: AnimatedVisibilityScope) {
    val anime = remember(id) { id?.let { vm.getAnimeById(it) } }
    var title by remember { mutableStateOf(anime?.title ?: "") }
    var ep by remember { mutableStateOf(anime?.episodes?.toString() ?: "") }
    var rate by remember { mutableIntStateOf(anime?.rating ?: 0) }
    var uri by remember { mutableStateOf<Uri?>(null) }

    // СОСТОЯНИЕ ДЛЯ ЖАНРОВ
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

    // ЛОГИКА ПЕРЕКЛЮЧЕНИЯ ТЕГОВ
    val onTagToggle: (String, String) -> Unit = { tag, categoryType ->
        val currentTags = selectedTags.toMutableList()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
            // ЕСЛИ УДАЛИЛИ ПОСЛЕДНИЙ ТЕГ, СБРАСЫВАЕМ КАТЕГОРИЮ
            if (currentTags.isEmpty()) {
                activeCategory = ""
            }
        } else {
            // МОЖНО ДОБАВИТЬ ТОЛЬКО ЕСЛИ МЕНЬШЕ 3 И КАТЕГОРИЯ СОВПАДАЕТ (ИЛИ ПУСТАЯ)
            if (currentTags.size < 3 && (activeCategory.isEmpty() || activeCategory == categoryType)) {
                currentTags.add(tag)
                activeCategory = categoryType
            }
        }
        selectedTags = currentTags
        performHaptic(view, "light")
    }

    with(sharedTransitionScope) {
        val sharedModifier = if (id == null) { Modifier.sharedBounds(rememberSharedContentState(key = "fab_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds) } else { Modifier.sharedBounds(rememberSharedContentState(key = "card_${id}"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds) }
        Scaffold(modifier = Modifier.fillMaxSize().then(sharedModifier), containerColor = Color.Transparent, topBar = { Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textC, modifier = if (id == null) Modifier.sharedElement(rememberSharedContentState(key = "fab_icon"), animatedVisibilityScope = animatedVisibilityScope) else Modifier) }; Spacer(Modifier.width(16.dp)); Text(text = if (id == null) s.addTitle else s.editTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = textC) } }, floatingActionButton = { AnimatedSaveFab(isEnabled = hasChanges, onClick = { performHaptic(view, "success"); if (title.isNotEmpty()) { scope.launch { delay(600); if (id != null) vm.updateAnime(ctx, id, title, ep.toIntOrNull()?:0, rate, uri, selectedTags, activeCategory) else vm.addAnime(ctx, title, ep.toIntOrNull()?:0, rate, uri, selectedTags, activeCategory); nav.popBackStack() } } else { Toast.makeText(ctx, s.enterTitleToast, Toast.LENGTH_SHORT).show() } }) }) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(bg))
                Column(modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
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
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedOneUiTextField(
                                value = title,
                                onValueChange = { title = it },
                                placeholder = s.animeTitleHint,
                                singleLine = false,
                                maxLines = 4
                            )
                        }
                        if (title.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            AnimatedCopyButton(textToCopy = title)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    AnimatedOneUiTextField(
                        value = ep,
                        onValueChange = { if (it.all { c -> c.isDigit() }) ep = it },
                        placeholder = s.episodesHint,
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(Modifier.height(12.dp))
                    EpisodeSuggestions { selectedEp -> performHaptic(view, "light"); ep = selectedEp }

                    Spacer(Modifier.height(32.dp))

                    // БЛОК ВЫБОРА ЖАНРОВ
                    Text(
                        text = "Category & Genres",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        textAlign = TextAlign.Start
                    )

                    GenreSelectionSection(
                        selectedTags = selectedTags,
                        activeCategory = activeCategory,
                        onTagToggle = onTagToggle
                    )

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
fun SettingsScreen(
    nav: NavController,
    vm: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val s = vm.strings
    val view = LocalView.current
    val context = LocalContext.current

    var expandedLang by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) } // NEW STATE
    var expandedUpdate by remember { mutableStateOf(false) }
    var expandedContact by remember { mutableStateOf(false) }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "settings_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                )
                .background(bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { performHaptic(view, "light"); nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textC,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "settings_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = s.settingsScreenTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textC
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        StatsCard(
                            title = s.languageCardTitle,
                            icon = Icons.Outlined.Language,
                            isExpanded = expandedLang,
                            iconTint = EpisodesColor,
                            iconBg = EpisodesColor.copy(alpha = 0.15f),
                            onClick = {
                                performHaptic(view, "light")
                                expandedLang = !expandedLang
                            }
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LanguageOption(
                                    text = s.langEn,
                                    isSelected = vm.currentLanguage == AppLanguage.EN,
                                    onClick = { vm.setLanguage(AppLanguage.EN) }
                                )
                                LanguageOption(
                                    text = s.langRu,
                                    isSelected = vm.currentLanguage == AppLanguage.RU,
                                    onClick = { vm.setLanguage(AppLanguage.RU) }
                                )
                            }
                        }
                    }

                    // NEW THEME CARD
                    item {
                        StatsCard(
                            title = s.themeTitle,
                            icon = Icons.Outlined.Settings,
                            isExpanded = expandedTheme,
                            iconTint = Color(0xFF9C27B0), // Purple for theme
                            iconBg = Color(0xFF9C27B0).copy(alpha = 0.15f),
                            onClick = {
                                performHaptic(view, "light")
                                expandedTheme = !expandedTheme
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ThemeOptionItem(
                                    label = s.themeLight,
                                    isSelected = vm.currentTheme == AppTheme.LIGHT,
                                    themeType = AppTheme.LIGHT,
                                    onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.LIGHT) }
                                )

                                ThemeOptionItem(
                                    label = s.themeDark,
                                    isSelected = vm.currentTheme == AppTheme.DARK,
                                    themeType = AppTheme.DARK,
                                    onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.DARK) }
                                )

                                ThemeOptionItem(
                                    label = s.themeSystem,
                                    isSelected = vm.currentTheme == AppTheme.SYSTEM,
                                    themeType = AppTheme.SYSTEM,
                                    onClick = { performHaptic(view, "light"); vm.setAppTheme(AppTheme.SYSTEM) }
                                )
                            }
                        }
                    }

                    item {
                        StatsCard(
                            title = s.checkForUpdateTitle,
                            icon = Icons.Outlined.SystemUpdate,
                            isExpanded = expandedUpdate,
                            iconTint = RateColor4,
                            iconBg = RateColor4.copy(alpha = 0.15f),
                            onClick = {
                                performHaptic(view, "light")
                                expandedUpdate = !expandedUpdate
                            }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Current version: ${vm.currentVersionName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    UpdateStateButton(
                                        status = vm.updateStatus,
                                        idleText = s.checkButtonText,
                                        onClick = {
                                            if (vm.updateStatus == AppUpdateStatus.IDLE || vm.updateStatus == AppUpdateStatus.ERROR) {
                                                vm.checkAppUpdate(context)
                                            } else if (vm.updateStatus == AppUpdateStatus.UPDATE_AVAILABLE) {
                                                vm.latestDownloadUrl?.let { url ->
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    view.context.startActivity(intent)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        StatsCard(
                            title = s.contactTitle,
                            icon = Icons.Outlined.Person,
                            isExpanded = expandedContact,
                            iconTint = BrandBlue,
                            iconBg = BrandBlue.copy(alpha = 0.15f),
                            onClick = {
                                performHaptic(view, "light")
                                expandedContact = !expandedContact
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = s.contactSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                performHaptic(view, "light")
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Phnem/MAList"))
                                                view.context.startActivity(intent)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.gh),
                                            contentDescription = "GitHub",
                                            modifier = Modifier.size(56.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                performHaptic(view, "light")
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/H415base"))
                                                view.context.startActivity(intent)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.tg),
                                            contentDescription = "Telegram",
                                            modifier = Modifier.size(56.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// КОМПОНЕНТЫ НАСТРОЕК
// ==========================================

@Composable
fun UpdateStateButton(
    status: AppUpdateStatus,
    idleText: String,
    onClick: () -> Unit
) {
    val view = LocalView.current

    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.secondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            AppUpdateStatus.NO_UPDATE -> RateColor4
            AppUpdateStatus.UPDATE_AVAILABLE -> BrandBlue
            AppUpdateStatus.ERROR -> BrandRed
        },
        label = "btnBg"
    )

    val contentColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.onSecondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
            else -> Color.White
        },
        label = "btnContent"
    )

    val widthFraction by animateFloatAsState(
        targetValue = if (status == AppUpdateStatus.IDLE) 1f else 0.6f,
        label = "btnWidth"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                enabled = status != AppUpdateStatus.LOADING,
                onClick = {
                    performHaptic(view, "light")
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
                    .togetherWith(fadeOut(animationSpec = tween(90)))
            },
            label = "btnContentAnim"
        ) { targetStatus ->
            when (targetStatus) {
                AppUpdateStatus.IDLE -> {
                    Text(
                        text = idleText,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 16.sp
                    )
                }
                AppUpdateStatus.LOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = contentColor,
                        strokeWidth = 2.dp
                    )
                }
                AppUpdateStatus.NO_UPDATE -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Up to date",
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                AppUpdateStatus.UPDATE_AVAILABLE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdateAlt,
                            contentDescription = "Update available",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Download", fontWeight = FontWeight.Bold, color = contentColor)
                    }
                }
                AppUpdateStatus.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = contentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) BrandBlue else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
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

            LaunchedEffect(Unit) {
                viewModel.loadAnime()
                viewModel.loadSettings()
                // ВАЖНО: ИНИЦИАЛИЗАЦИЯ ВЕРСИИ ПРИ ЗАПУСКЕ
                viewModel.initAppVersion(context)
            }

            // DETERMINE THEME
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (viewModel.currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemDark
            }

            OneUiTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()

                SharedTransitionLayout {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(nav = navController, vm = viewModel, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = this) }
                        composable("add_anime?animeId={animeId}", arguments = listOf(navArgument("animeId") { nullable = true })) { AddEditScreen(navController, viewModel, it.arguments?.getString("animeId"), this@SharedTransitionLayout, this) }
                        composable("settings") {
                            SettingsScreen(
                                nav = navController,
                                vm = viewModel,
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this
                            )
                        }
                    }
                }
            }
        }
    }
    private fun checkPerms() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")); startActivity(intent); Toast.makeText(this, "Need file access", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun EmptyStateView(
    modifier: Modifier = Modifier,
    title: String = "Nothing in folder",
    subtitle: String = "Looks empty over here."
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ModernEmptyFolderIcon(modifier = Modifier.size(120.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun ModernEmptyFolderIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxSize()
        )
    }
}
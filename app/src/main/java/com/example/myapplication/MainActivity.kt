package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.min

// ==========================================
// ONE UI THEME & COLORS
// ==========================================

val OneUiBlack = Color(0xFF000000)
val OneUiListBg = Color(0xFF080808)
val OneUiCardBg = Color(0xFF1C1C1E)
val OneUiBlue = Color(0xFF3E82F7)
val OneUiRed = Color(0xFFFF3B30)
val OneUiText = Color(0xFFFFFFFF)
val OneUiTextDim = Color(0xFF999999)

// Цвета рейтинга (от красного к зеленому)
val RateColor1 = Color(0xFFD32F2F)
val RateColor2 = Color(0xFFE64A19)
val RateColor3 = Color(0xFFF57C00)
val RateColor4 = Color(0xFFFBC02D)
val RateColor5 = Color(0xFF388E3C)
val RateColorEmpty = Color(0xFF424242) // Серый для пустых звезд

@Composable
fun OneUiTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        background = OneUiBlack,
        surface = OneUiCardBg,
        primary = OneUiBlue,
        onBackground = OneUiText,
        onSurface = OneUiText,
        secondary = OneUiTextDim,
        error = OneUiRed
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                color = OneUiText
            ),
            titleMedium = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
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
    val dateAdded: Long = System.currentTimeMillis()
)

class AnimeViewModel : ViewModel() {
    private val _animeList = mutableStateListOf<Anime>()
    val animeList: List<Anime> get() = _animeList
    var searchQuery by mutableStateOf("")
    var sortOption by mutableStateOf(SortOption.DATE_NEWEST)

    private val ROOT = "MyAnimeList"; private val IMG_DIR = "collection"; private val FILE_NAME = "list.json"

    private fun getRoot(): File {
        val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val f = File(d, ROOT); if (!f.exists()) f.mkdirs(); return f
    }
    private fun getImgDir(): File { val f = File(getRoot(), IMG_DIR); if(!f.exists()) f.mkdirs(); return f }
    private fun getDataFile(): File = File(getRoot(), FILE_NAME)

    fun loadAnime() {
        val f = getDataFile()
        if (f.exists()) {
            try {
                val json = f.readText()
                val list: List<Anime> = Gson().fromJson(json, object : TypeToken<List<Anime>>() {}.type) ?: emptyList()
                _animeList.clear(); _animeList.addAll(list.sortedBy { it.orderIndex })
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getAnimeById(id: String): Anime? = _animeList.find { it.id == id }

    fun addAnime(ctx: Context, title: String, ep: Int, rate: Int, uri: Uri?) {
        val id = UUID.randomUUID().toString()
        val img = if (uri != null) saveImg(ctx, uri, id) else null
        _animeList.add(0, Anime(id, title, ep, rate, img, (_animeList.maxOfOrNull { it.orderIndex }?:0)+1))
        save()
    }

    fun updateAnime(ctx: Context, id: String, title: String, ep: Int, rate: Int, uri: Uri?) {
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
        _animeList[idx] = _animeList[idx].copy(title=title, episodes=ep, rating=rate, imageFileName=img)
        save()
    }

    fun deleteAnime(id: String) {
        val anime = _animeList.find { it.id == id } ?: return
        anime.imageFileName?.let { File(getImgDir(), it).delete() }
        _animeList.remove(anime)
        save()
    }

    private fun save() { try { getDataFile().writeText(Gson().toJson(_animeList)) } catch(e:Exception){e.printStackTrace()} }

    private fun saveImg(ctx: Context, uri: Uri, id: String): String? {
        return try {
            val name = "img_${id}_${System.currentTimeMillis()}.jpg"
            ctx.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(File(getImgDir(), name)).use { o -> i.copyTo(o) } }
            name
        } catch(e: Exception) { null }
    }

    fun getImgPath(name: String?): File? = if(name!=null) File(getImgDir(), name).let { if(it.exists()) it else null } else null

    // --- УМНЫЙ ПОИСК ---
    fun getDisplayList(): List<Anime> {
        val rawQuery = searchQuery.trim()
        if (rawQuery.isBlank()) return sortList(_animeList)
        val queryTokens = tokenize(rawQuery)
        val filtered = _animeList.filter { anime ->
            val titleTokens = tokenize(anime.title)
            queryTokens.all { qToken ->
                titleTokens.any { tToken -> isTokenMatch(qToken, tToken) }
            }
        }
        return sortList(filtered)
    }

    private fun sortList(list: List<Anime>): List<Anime> {
        return when(sortOption) {
            SortOption.DATE_NEWEST -> list.sortedByDescending { it.dateAdded }
            SortOption.RATING_HIGH -> list.sortedByDescending { it.rating }
            SortOption.AZ -> list.sortedBy { it.title }
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zа-я0-9]"), " ")
            .split(" ")
            .filter { it.isNotBlank() }
    }

    private fun isTokenMatch(query: String, target: String): Boolean {
        if (target.contains(query)) return true
        val qStem = getStem(query)
        val tStem = getStem(target)
        if (tStem.startsWith(qStem)) return true
        val maxDist = if (query.length > 6) 2 else if (query.length > 3) 1 else 0
        if (levenshtein(query, target) <= maxDist) return true
        if (query.length > 3 && levenshtein(qStem, tStem) <= maxDist) return true
        return false
    }

    private fun getStem(word: String): String {
        var res = word
        val endings = listOf("ями", "ами", "ов", "ев", "ей", "ий", "ой", "ый", "ая", "ое", "ые", "ие", "ом", "ем", "ам", "ах", "ях", "у", "ю", "а", "я", "о", "е", "ы", "и", "ь")
        for (end in endings) {
            if (res.length > end.length + 2 && res.endsWith(end)) {
                return res.substring(0, res.length - end.length)
            }
        }
        return res
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

enum class SortOption(val label: String) {
    DATE_NEWEST("Newest First"),
    RATING_HIGH("Highest Rated"),
    AZ("Name (A-Z)")
}

// ==========================================
// UI COMPONENTS
// ==========================================

val OneUiShape = RoundedCornerShape(26.dp)

fun performHaptic(view: android.view.View, isStrong: Boolean = false) {
    if (isStrong) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

// --- ИКОНКА ЗВЕЗДЫ (Твой SVG) ---
val StarIconVector: ImageVector
    get() = ImageVector.Builder(
        name = "Star",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(Color.Black), // Цвет переопределяется tint
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero
    ) {
        // Твой SVG path: M8.243 7.34l-6.38 .925l-.113 .023a1 1 0 0 0 -.44 1.684l4.622 4.499l-1.09 6.355l-.013 .11a1 1 0 0 0 1.464 .944l5.706 -3l5.693 3l.1 .046a1 1 0 0 0 1.352 -1.1l-1.091 -6.355l4.624 -4.5l.078 -.085a1 1 0 0 0 -.633 -1.62l-6.38 -.926l-2.852 -5.78a1 1 0 0 0 -1.794 0l-2.853 5.78z
        moveTo(8.243f, 7.34f)
        lineTo(1.863f, 8.265f)
        lineTo(1.75f, 8.288f)
        arcTo(1f, 1f, 0f, false, false, 1.31f, 9.972f)
        lineTo(5.932f, 14.471f)
        lineTo(4.842f, 20.826f)
        lineTo(4.829f, 20.936f)
        arcTo(1f, 1f, 0f, false, false, 6.293f, 21.88f)
        lineTo(12.0f, 18.88f)
        lineTo(17.693f, 21.88f)
        lineTo(17.793f, 21.926f)
        arcTo(1f, 1f, 0f, false, false, 19.145f, 20.826f)
        lineTo(18.054f, 14.471f)
        lineTo(22.678f, 9.971f)
        lineTo(22.756f, 9.886f)
        arcTo(1f, 1f, 0f, false, false, 22.123f, 8.266f)
        lineTo(15.743f, 7.34f)
        lineTo(12.891f, 1.56f)
        arcTo(1f, 1f, 0f, false, false, 11.097f, 1.56f)
        lineTo(8.243f, 7.34f)
        close()
    }.build()

// --- НОВЫЙ РЕЙТИНГ (ЗВЕЗДЫ) ---
@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    val colors = listOf(RateColor1, RateColor2, RateColor3, RateColor4, RateColor5)

    // Определяем цвет для ВСЕХ активных звезд в зависимости от текущего рейтинга
    // Если рейтинг 0, то цвет не важен (будет серый). Если 5 - зеленый.
    val activeColor = if (rating > 0) colors[rating - 1] else RateColorEmpty

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center, // Центрируем звезды
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val isSelected = i <= rating
            // Анимация цвета: если звезда активна, она берет activeColor, иначе серая
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) activeColor else RateColorEmpty,
                animationSpec = tween(300),
                label = "color"
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "scale"
            )

            Icon(
                imageVector = StarIconVector, // Используем твой SVG
                contentDescription = "Rate $i",
                tint = animatedColor,
                modifier = Modifier
                    .size(54.dp) // Чуть крупнее для звезд
                    .scale(scale)
                    .padding(4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // Убираем рипл эффект, оставляем только анимацию скейла
                    ) {
                        onRatingChanged(i)
                    }
            )
        }
    }
}

// --- ПУЗЫРЬКИ И КОПИРОВАНИЕ ---

@Composable
fun EpisodeSuggestions(onSelect: (String) -> Unit) {
    val suggestions = listOf("12", "13", "24", "36", "48", "100")

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEach { num ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E))
                    .clickable { onSelect(num) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = num, color = OneUiBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AnimatedCopyButton(textToCopy: String) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(contentAlignment = Alignment.Center) {
        androidx.compose.animation.AnimatedVisibility(
            visible = isCopied,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-40).dp)
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Copied!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        IconButton(
            onClick = {
                if (textToCopy.isNotEmpty()) {
                    performHaptic(view, false)
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    isCopied = true
                }
            }
        ) {
            AnimatedContent(
                targetState = isCopied,
                transitionSpec = {
                    (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                },
                label = "copy_icon"
            ) { copied ->
                if (copied) {
                    LaunchedEffect(Unit) {
                        delay(2000)
                        isCopied = false
                    }
                    Icon(Icons.Default.Check, null, tint = OneUiBlue, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.ContentCopy, null, tint = OneUiTextDim, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// Улучшенный TextField
@Composable
fun OneUiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, color = Color.Gray, fontSize = 18.sp)
                innerTextField()
            }
        }
    )
}

// --- ОСНОВНОЙ КАРТОЧНЫЙ UI ---

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun OneUiAnimeCard(
    anime: Anime,
    viewModel: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val imageFile = remember(anime.imageFileName) { viewModel.getImgPath(anime.imageFileName) }
    val view = LocalView.current

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "container_${anime.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                )
                .shadow(
                    elevation = 8.dp,
                    shape = OneUiShape,
                    spotColor = Color.Black.copy(alpha = 0.5f),
                    ambientColor = Color.Black.copy(alpha = 0.2f)
                )
                .clip(OneUiShape)
                .background(OneUiCardBg)
                .combinedClickable(
                    onClick = {
                        performHaptic(view, false)
                        onClick()
                    },
                    onLongClick = {
                        performHaptic(view, true)
                        onLongClick()
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "image_${anime.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (imageFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = anime.title.take(1).uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = anime.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${anime.episodes} episodes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Icon(
                        // Используем твою иконку и в списке
                        imageVector = StarIconVector,
                        contentDescription = null,
                        tint = if (anime.rating > 0) listOf(RateColor1, RateColor2, RateColor3, RateColor4, RateColor5)[(anime.rating - 1).coerceIn(0, 4)] else RateColorEmpty,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = anime.rating.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleAnimePreviewOverlay(
    anime: Anime,
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val imageFile = remember(anime.imageFileName) { viewModel.getImgPath(anime.imageFileName) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!showConfirmDialog) {
                    performHaptic(view, false)
                    onDismiss()
                }
            }
            .zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = !showConfirmDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(OneUiCardBg)
                ) {
                    if (imageFile != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(imageFile).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(anime.title.take(1), fontSize = 60.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 300f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = anime.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = OneUiBlue.copy(alpha = 0.8f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "${anime.episodes} EP",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Icon(
                                imageVector = StarIconVector,
                                contentDescription = null,
                                tint = if (anime.rating > 0) listOf(RateColor1, RateColor2, RateColor3, RateColor4, RateColor5)[(anime.rating - 1).coerceIn(0, 4)] else RateColorEmpty,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = "${anime.rating}/5",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                AnimatedTrashButton(
                    onClick = {
                        performHaptic(view, true)
                        showConfirmDialog = true
                    }
                )
            }
        }

        if (showConfirmDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    performHaptic(view, true)
                    onDelete()
                },
                onCancel = {
                    performHaptic(view, false)
                    showConfirmDialog = false
                }
            )
        }
    }
}

@Composable
fun AnimatedTrashButton(onClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.8f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .size(70.dp)
            .scale(scale)
            .shadow(12.dp, CircleShape, spotColor = OneUiRed)
            .clip(CircleShape)
            .background(OneUiRed)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = "Delete",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
fun DeleteConfirmationDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onCancel) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF202022))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(OneUiRed.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DeleteForever, null, tint = OneUiRed, modifier = Modifier.size(32.dp))
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Delete title?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OneUiTextDim,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { onCancel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cancel", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(25.dp))
                                .background(OneUiRed)
                                .clickable { onConfirm() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREENS
// ==========================================

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
    var showSortSheet by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isDockVisible by remember { mutableStateOf(true) }

    var selectedAnimeForPreview by remember { mutableStateOf<Anime?>(null) }
    var currentPreviewAnime by remember { mutableStateOf<Anime?>(null) }

    if (selectedAnimeForPreview != null) {
        currentPreviewAnime = selectedAnimeForPreview
    }

    BackHandler(enabled = isSearchFocused || vm.searchQuery.isNotEmpty() || selectedAnimeForPreview != null) {
        if (selectedAnimeForPreview != null) {
            performHaptic(view, false)
            selectedAnimeForPreview = null
        } else {
            performHaptic(view, false)
            vm.searchQuery = ""
            isSearchFocused = false
            focusManager.clearFocus()
            kbd?.hide()
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -5) {
                    isDockVisible = false
                } else if (available.y > 5) {
                    isDockVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        containerColor = OneUiBlack,
        bottomBar = { }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
        ) {

            val shouldBlur = (isSearchFocused && vm.searchQuery.isBlank()) || selectedAnimeForPreview != null
            val blurAmount by animateDpAsState(targetValue = if (shouldBlur) 20.dp else 0.dp, label = "blur")

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurAmount)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OneUiBlack)
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .padding(top = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "My\nCollection",
                            style = MaterialTheme.typography.displayLarge
                        )
                        IconButton(onClick = {
                            performHaptic(view, false)
                            vm.loadAnime()
                        }) {
                            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${vm.animeList.size} items",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(
                            color = Color(0xFF2C2C2E),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                performHaptic(view, false)
                                showSortSheet = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(vm.sortOption.label, fontSize = 12.sp, color = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Список
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                        .background(OneUiListBg)
                ) {
                    val list = vm.getDisplayList()
                    if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("List is empty (or no matches)", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(list, key = { it.id }) { anime ->
                                Box(modifier = Modifier.animateItemPlacement()) {
                                    OneUiAnimeCard(
                                        anime = anime,
                                        viewModel = vm,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        onClick = {
                                            nav.navigate("add_anime?animeId=${anime.id}")
                                        },
                                        onLongClick = {
                                            performHaptic(view, true)
                                            selectedAnimeForPreview = anime
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (shouldBlur) {
                if (selectedAnimeForPreview == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                focusManager.clearFocus()
                                isSearchFocused = false
                                kbd?.hide()
                            }
                            .zIndex(2f)
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedAnimeForPreview != null,
                enter = fadeIn() + scaleIn(initialScale = 0.9f) + slideInVertically { it / 6 },
                exit = fadeOut() + scaleOut(targetScale = 0.9f) + slideOutVertically { it / 6 },
                modifier = Modifier.zIndex(50f)
            ) {
                currentPreviewAnime?.let { anime ->
                    SimpleAnimePreviewOverlay(
                        anime = anime,
                        viewModel = vm,
                        onDismiss = { selectedAnimeForPreview = null },
                        onDelete = {
                            scope.launch {
                                selectedAnimeForPreview = null
                                delay(250)
                                vm.deleteAnime(anime.id)
                            }
                        }
                    )
                }
            }

            // Кнопка наверх
            AnimatedVisibility(
                visible = showScrollToTop && !isSearchFocused && selectedAnimeForPreview == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 24.dp)
                    .zIndex(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333).copy(alpha = 0.8f))
                        .clickable {
                            performHaptic(view, false)
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "Up", tint = Color.White)
                }
            }

            // Док
            AnimatedVisibility(
                visible = (isDockVisible || isSearchFocused) && selectedAnimeForPreview == null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val glowColor = Color.White.copy(alpha = 0.15f)
                    val borderColor = Color.White.copy(alpha = 0.1f)

                    BasicTextField(
                        value = vm.searchQuery,
                        onValueChange = {
                            vm.searchQuery = it
                            if(it.isNotEmpty()) performHaptic(view, false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = glowColor, spotColor = glowColor)
                            .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF202022).copy(alpha = 0.95f))
                            .padding(horizontal = 20.dp)
                            .onFocusChanged { isSearchFocused = it.isFocused },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                        cursorBrush = SolidColor(OneUiBlue),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = if(isSearchFocused) OneUiBlue else Color.Gray)
                                Spacer(Modifier.width(12.dp))
                                Box {
                                    if (vm.searchQuery.isEmpty()) {
                                        Text("Search", color = Color.Gray, fontSize = 16.sp)
                                    }
                                    innerTextField()
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { kbd?.hide() })
                    )

                    Spacer(Modifier.width(12.dp))

                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(12.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
                                .border(1.dp, borderColor, CircleShape)
                                .sharedBounds(
                                    rememberSharedContentState(key = "fab_container"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                                )
                                .clip(CircleShape)
                                .background(OneUiBlue)
                                .clickable {
                                    performHaptic(view, false)
                                    nav.navigate("add_anime")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = Color.White,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "fab_icon"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text("Sort by", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
                    SortOption.values().forEach { option ->
                        NavigationDrawerItem(
                            label = { Text(option.label) },
                            icon = { },
                            selected = vm.sortOption == option,
                            onClick = {
                                performHaptic(view, false)
                                vm.sortOption = option; showSortSheet = false
                            },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

// --- НОВЫЙ ЭКРАН ДОБАВЛЕНИЯ / РЕДАКТИРОВАНИЯ ---

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AddEditScreen(
    nav: NavController,
    vm: AnimeViewModel,
    id: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val anime = remember(id) { id?.let { vm.getAnimeById(it) } }

    // States
    var title by remember { mutableStateOf(anime?.title ?: "") }
    var ep by remember { mutableStateOf(anime?.episodes?.toString() ?: "") }
    var rate by remember { mutableIntStateOf(anime?.rating ?: 0) }
    var uri by remember { mutableStateOf<Uri?>(null) }

    // Логика блокировки кнопки Save
    val hasChanges by remember(title, ep, rate, uri) {
        derivedStateOf {
            if (id == null) {
                title.isNotBlank()
            } else {
                (anime != null) && (
                        title != anime.title ||
                                ep != anime.episodes.toString() ||
                                rate != anime.rating ||
                                uri != null
                        )
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val ctx = LocalContext.current
    val view = LocalView.current

    with(sharedTransitionScope) {
        val sharedModifier = if (id == null) {
            Modifier.sharedBounds(rememberSharedContentState(key = "fab_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds)
        } else {
            Modifier.sharedBounds(rememberSharedContentState(key = "card_${id}"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds)
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().then(sharedModifier),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = if (id == null) Modifier.sharedElement(rememberSharedContentState(key = "fab_icon"), animatedVisibilityScope = animatedVisibilityScope) else Modifier)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(text = if (id == null) "Add title" else "Edit title", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OneUiText)
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Картинка
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .aspectRatio(0.7f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                        .clickable {
                            performHaptic(view, false)
                            launcher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val imageModifier = if (id != null) Modifier.sharedElement(rememberSharedContentState(key = "image_${id}"), animatedVisibilityScope = animatedVisibilityScope) else Modifier
                    Box(modifier = Modifier.fillMaxSize().then(imageModifier).clip(RoundedCornerShape(32.dp))) {
                        if (uri != null) AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else if (anime?.imageFileName != null) AsyncImage(vm.getImgPath(anime.imageFileName), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary); Text("Add Photo", color = MaterialTheme.colorScheme.secondary) }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Название + Копирование
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        OneUiTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = "Title",
                            singleLine = false,
                            maxLines = 4
                        )
                    }
                    if (title.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        AnimatedCopyButton(textToCopy = title)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Серии
                OneUiTextField(
                    value = ep,
                    onValueChange = { if (it.all { c -> c.isDigit() }) ep = it },
                    placeholder = "Episodes",
                    keyboardType = KeyboardType.Number
                )

                Spacer(Modifier.height(12.dp))

                // Пузырьки серий
                EpisodeSuggestions { selectedEp ->
                    performHaptic(view, false)
                    ep = selectedEp
                }

                Spacer(Modifier.height(32.dp))

                // ЗВЕЗДНЫЙ РЕЙТИНГ
                Text("Rating", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                StarRatingBar(rating = rate) { newRate ->
                    performHaptic(view, false)
                    rate = newRate
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(24.dp))

                // Кнопка Save
                Button(
                    onClick = {
                        performHaptic(view, false)
                        if (title.isNotEmpty()) {
                            if (id != null) vm.updateAnime(ctx, id, title, ep.toIntOrNull()?:0, rate, uri)
                            else vm.addAnime(ctx, title, ep.toIntOrNull()?:0, rate, uri)
                            nav.popBackStack()
                        } else Toast.makeText(ctx, "Enter title", Toast.LENGTH_SHORT).show()
                    },
                    enabled = hasChanges,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "Save",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasChanges) Color.White else OneUiTextDim
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.attributes.preferredDisplayModeId = 0 }
        checkPerms()
        setContent {
            OneUiTheme {
                val navController = rememberNavController()
                val viewModel: AnimeViewModel = viewModel()
                LaunchedEffect(Unit) { viewModel.loadAnime() }
                SharedTransitionLayout {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(navController, viewModel, this@SharedTransitionLayout, this) }
                        composable("add_anime?animeId={animeId}", arguments = listOf(navArgument("animeId") { nullable = true })) { AddEditScreen(navController, viewModel, it.arguments?.getString("animeId"), this@SharedTransitionLayout, this) }
                    }
                }
            }
        }
    }
    private fun checkPerms() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Need file access", Toast.LENGTH_LONG).show()
        }
    }
}
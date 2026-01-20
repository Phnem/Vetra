package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ==========================================
// ONE UI THEME
// ==========================================

val OneUiBlack = Color(0xFF000000)      // Фон заголовка
val OneUiListBg = Color(0xFF080808)     // Фон панели списка (едва светлее черного, чтобы видеть форму)
val OneUiCardBg = Color(0xFF1C1C1E)     // Цвет карточек
val OneUiBlue = Color(0xFF3E82F7)
val OneUiText = Color(0xFFFFFFFF)
val OneUiTextDim = Color(0xFF999999)

@Composable
fun OneUiTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        background = OneUiBlack,
        surface = OneUiCardBg,
        primary = OneUiBlue,
        onBackground = OneUiText,
        onSurface = OneUiText,
        secondary = OneUiTextDim
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

    private fun save() { try { getDataFile().writeText(Gson().toJson(_animeList)) } catch(e:Exception){e.printStackTrace()} }

    private fun saveImg(ctx: Context, uri: Uri, id: String): String? {
        return try {
            val name = "img_${id}_${System.currentTimeMillis()}.jpg"
            ctx.contentResolver.openInputStream(uri)?.use { i -> FileOutputStream(File(getImgDir(), name)).use { o -> i.copyTo(o) } }
            name
        } catch(e: Exception) { null }
    }

    fun getImgPath(name: String?): File? = if(name!=null) File(getImgDir(), name).let { if(it.exists()) it else null } else null

    fun getDisplayList(): List<Anime> {
        val list = if (searchQuery.isBlank()) _animeList else _animeList.filter { it.title.contains(searchQuery, true) }
        return when(sortOption) {
            SortOption.DATE_NEWEST -> list.sortedByDescending { it.dateAdded }
            SortOption.RATING_HIGH -> list.sortedByDescending { it.rating }
            SortOption.AZ -> list.sortedBy { it.title }
        }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OneUiAnimeCard(
    anime: Anime,
    viewModel: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit
) {
    val imageFile = remember(anime.imageFileName) { viewModel.getImgPath(anime.imageFileName) }

    with(sharedTransitionScope) {
        // КАРТОЧКА
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .sharedBounds(
                    rememberSharedContentState(key = "card_${anime.id}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
                )
                .clip(OneUiShape)
                .background(OneUiCardBg) // Цвет самой карточки
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Картинка
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
                        Icons.Default.Star,
                        null,
                        tint = Color(0xFFFFB400),
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

// ==========================================
// SCREENS
// ==========================================

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    nav: NavController,
    vm: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val kbd = LocalSoftwareKeyboardController.current
    var showSortSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    Scaffold(
        containerColor = OneUiBlack,
        bottomBar = { /* Пусто */ }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. ОСНОВНОЙ ЛЕЙАУТ
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // --- HEADER (Viewing Area) ---
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
                        IconButton(onClick = { vm.loadAnime() }) {
                            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // СОРТИРОВКА
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
                            onClick = { showSortSheet = true }
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

                // --- СПИСОК (Interaction Area) ---
                // Занимает все оставшееся место, примыкая прямо к хедеру
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f) // Заполняет остаток экрана
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)) // Сильное скругление
                        .background(OneUiListBg) // Чуть светлее черного
                ) {
                    val list = vm.getDisplayList()
                    if (list.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("List is empty", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            // Отступы для контента внутри скругленной панели
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp) // Карточки разделены
                        ) {
                            items(list, key = { it.id }) { anime ->
                                OneUiAnimeCard(
                                    anime = anime,
                                    viewModel = vm,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                ) {
                                    nav.navigate("add_anime?animeId=${anime.id}")
                                }
                            }
                        }
                    }
                }
            }

            // 2. SCROLL TO TOP BUTTON (Справа внизу)
            AnimatedVisibility(
                visible = showScrollToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 110.dp, end = 24.dp)
                    .zIndex(5f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333).copy(alpha = 0.8f)) // Полупрозрачный
                        .clickable {
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. ПОИСК + FAB (Внизу, без подложки)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search
                    BasicTextField(
                        value = vm.searchQuery,
                        onValueChange = { vm.searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .shadow(12.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF2C2C2E))
                            .padding(horizontal = 24.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = Color.Gray)
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

                    // FAB
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(12.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary)
                                .sharedBounds(
                                    rememberSharedContentState(key = "fab_container"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                                )
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { nav.navigate("add_anime") },
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

        // Шторка сортировки
        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "Sort by",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )
                    SortOption.values().forEach { option ->
                        NavigationDrawerItem(
                            label = { Text(option.label) },
                            icon = { },
                            selected = vm.sortOption == option,
                            onClick = {
                                vm.sortOption = option
                                showSortSheet = false
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
    var title by remember { mutableStateOf(anime?.title ?: "") }
    var ep by remember { mutableStateOf(anime?.episodes?.toString() ?: "") }
    var rate by remember { mutableFloatStateOf(anime?.rating?.toFloat() ?: 3f) }
    var uri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri = it }
    val ctx = LocalContext.current

    with(sharedTransitionScope) {
        val sharedModifier = if (id == null) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "fab_container"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        } else {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "card_${id}"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize().then(sharedModifier),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            modifier = if (id == null) Modifier.sharedElement(
                                rememberSharedContentState(key = "fab_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ) else Modifier
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (id == null) "New Anime" else "Edit Anime",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = OneUiText
                    )
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
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .aspectRatio(0.7f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    val imageModifier = if (id != null) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = "image_${id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(imageModifier)
                            .clip(RoundedCornerShape(32.dp))
                    ) {
                        if (uri != null) {
                            AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else if (anime?.imageFileName != null) {
                            AsyncImage(vm.getImgPath(anime.imageFileName), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text("Add Photo", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                OneUiTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
                Spacer(Modifier.height(16.dp))
                OneUiTextField(value = ep, onValueChange = { if (it.all { c -> c.isDigit() }) ep = it }, placeholder = "Episodes", keyboardType = KeyboardType.Number)

                Spacer(Modifier.height(32.dp))

                Text("Rating", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(5) { i ->
                        val isSelected = i < rate
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFFFB400) else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { rate = (i + 1).toFloat() }
                                .scale(if (isSelected) 1.1f else 1f)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        if (title.isNotEmpty()) {
                            if (id != null) vm.updateAnime(ctx, id, title, ep.toIntOrNull()?:0, rate.toInt(), uri)
                            else vm.addAnime(ctx, title, ep.toIntOrNull()?:0, rate.toInt(), uri)
                            nav.popBackStack()
                        } else Toast.makeText(ctx, "Enter title", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun OneUiTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 24.dp),
        textStyle = TextStyle(fontSize = 18.sp, color = Color.White),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.Gray, fontSize = 18.sp)
                }
                innerTextField()
            }
        }
    )
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.preferredDisplayModeId = 0
        }

        checkPerms()
        setContent {
            OneUiTheme {
                val navController = rememberNavController()
                val viewModel: AnimeViewModel = viewModel()
                LaunchedEffect(Unit) { viewModel.loadAnime() }

                SharedTransitionLayout {
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController, viewModel, this@SharedTransitionLayout, this)
                        }
                        composable("add_anime?animeId={animeId}", arguments = listOf(navArgument("animeId") { nullable = true })) {
                            AddEditScreen(navController, viewModel, it.arguments?.getString("animeId"), this@SharedTransitionLayout, this)
                        }
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
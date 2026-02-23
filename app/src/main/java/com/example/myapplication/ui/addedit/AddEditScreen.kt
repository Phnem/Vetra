package com.example.myapplication.ui.addedit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myapplication.data.models.*
import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import com.example.myapplication.ui.shared.components.*
import com.example.myapplication.ui.shared.theme.*
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.addedit.CommentMorphingContainer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AddEditScreen(
    navController: NavController,
    viewModel: AddEditViewModel,
    homeViewModel: HomeViewModel,
    animeId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsVm: com.example.myapplication.ui.settings.SettingsViewModel = org.koin.androidx.compose.koinViewModel()
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
    val currentLanguage = settingsState.language
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.updateImageUri(it)
    }
    val ctx = LocalContext.current
    val view = LocalView.current
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val scope = rememberCoroutineScope()

    LaunchedEffect(animeId) {
        viewModel.loadAnime(animeId)
    }

    val anime = remember(animeId) {
        animeId?.let { homeViewModel.getAnimeById(it) }
    }

    val commentHazeState = remember { HazeState() }
    with(sharedTransitionScope) {
        val sharedModifier = if (animeId == null) {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "fab_container"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(32.dp))
            )
        } else {
            Modifier.sharedBounds(
                rememberSharedContentState(key = "anime_${animeId}_bounds"),
                animatedVisibilityScope = animatedVisibilityScope,
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(32.dp))
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = textC,
                            modifier = if (animeId == null) Modifier.sharedElement(
                                rememberSharedContentState(key = "fab_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ) else Modifier
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = if (animeId == null) "Add Title" else "Edit",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textC
                    )
                }
            },
            floatingActionButton = {
                AnimatedSaveFab(
                    isEnabled = uiState.hasChanges,
                    onClick = {
                        performHaptic(view, "success")
                        if (uiState.title.isNotEmpty()) {
                            scope.launch {
                                delay(600)
                                viewModel.saveAnime(ctx) {
                                    homeViewModel.loadAnime()
                                    navController.popBackStack()
                                }
                            }
                        } else {
                            android.widget.Toast.makeText(ctx, "Enter title", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(sharedModifier)
                    .clip(RoundedCornerShape(32.dp))
                    .hazeSource(commentHazeState)
                    .background(bg)
            ) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .imePadding()
                        .padding(horizontal = 24.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .aspectRatio(0.7f)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(32.dp)
                            )
                            .clickable {
                                performHaptic(view, "light")
                                launcher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModifier = if (animeId != null) Modifier.sharedElement(
                            rememberSharedContentState(key = "anime_${animeId}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ) else Modifier
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(imageModifier)
                                .clip(RoundedCornerShape(32.dp))
                        ) {
                            if (uiState.imageUri != null) {
                                AsyncImage(
                                    uiState.imageUri,
                                    null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (anime?.imageFileName != null) {
                                AsyncImage(
                                    homeViewModel.getImgPath(anime.imageFileName),
                                    null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text("Add photo", color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            AnimatedOneUiTextField(
                                value = uiState.title,
                                onValueChange = viewModel::updateTitle,
                                placeholder = "Anime title",
                                singleLine = false,
                                maxLines = 4
                            )
                        }
                        if (uiState.title.isNotEmpty()) {
                            Spacer(Modifier.width(8.dp))
                            AnimatedCopyButton(textToCopy = uiState.title)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    AnimatedOneUiTextField(
                        value = uiState.episodes,
                        onValueChange = viewModel::updateEpisodes,
                        placeholder = "Episodes watched",
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(Modifier.height(12.dp))
                    EpisodeSuggestions { selectedEp ->
                        performHaptic(view, "light")
                        viewModel.updateEpisodes(selectedEp)
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = "Category & Genres",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                    )
                    GenreSelectionSection(
                        strings = getStrings(currentLanguage),
                        currentLanguage = currentLanguage,
                        selectedTags = uiState.selectedTags,
                        activeCategory = uiState.categoryType,
                        onTagToggle = { tag, categoryType ->
                            val currentTags = uiState.selectedTags.toMutableList()
                            if (currentTags.contains(tag)) {
                                currentTags.remove(tag)
                                if (currentTags.isEmpty()) {
                                    viewModel.updateTags(emptyList(), "")
                                } else {
                                    viewModel.updateTags(currentTags, uiState.categoryType)
                                }
                            } else {
                                if (currentTags.size < 3 && (uiState.categoryType.isEmpty() || uiState.categoryType == categoryType)) {
                                    currentTags.add(tag)
                                    viewModel.updateTags(currentTags, categoryType)
                                }
                            }
                            performHaptic(view, "light")
                        }
                    )
                    Spacer(Modifier.height(32.dp))
                    StarRatingBar(rating = uiState.rating) { newRate ->
                        performHaptic(view, "light")
                        viewModel.updateRating(newRate)
                    }
                    Spacer(Modifier.height(24.dp))
                    CommentMorphingContainer(
                        state = uiState,
                        hazeState = commentHazeState,
                        onModeChange = viewModel::updateCommentMode,
                        onSaveComment = viewModel::saveComment,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

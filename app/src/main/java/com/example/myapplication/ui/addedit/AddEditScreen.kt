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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.myapplication.data.models.*
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import com.example.myapplication.ui.shared.components.*
import com.example.myapplication.ui.shared.InertialCollisionState
import com.example.myapplication.ui.shared.inertialCollision
import com.example.myapplication.ui.shared.rememberInertialCollisionState
import com.example.myapplication.ui.shared.theme.*
import com.example.myapplication.ui.addedit.CommentMorphingContainer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@Composable
private fun AnimatedFormRow(
    index: Int,
    collisionState: InertialCollisionState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.inertialCollision(
            state = collisionState,
            index = index,
            baseMultiplier = 4.5f
        )
    ) {
        content()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AddEditScreen(
    navController: NavController,
    viewModel: AddEditViewModel,
    animeId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsVm: com.example.myapplication.ui.settings.SettingsViewModel = org.koin.androidx.compose.koinViewModel()
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
    val currentLanguage = settingsState.language
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        viewModel.onEvent(AddEditEvent.OnImageUriChanged(it))
    }
    val ctx = LocalContext.current
    val view = LocalView.current
    val textC = MaterialTheme.colorScheme.onBackground
    val collisionState = rememberInertialCollisionState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AddEditEffect.NavigateBack -> navController.popBackStack()
                is AddEditEffect.ShowError -> android.widget.Toast.makeText(ctx, effect.message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(animeId) {
        viewModel.loadAnime(animeId)
        collisionState.triggerCollision(
            impactForce = 55f,
            stiffness = 200f,
            dampingRatio = 0.45f
        )
    }

    val imageFilePath = uiState.imageFilePath

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
                    isEnabled = uiState.hasChanges && !uiState.isLoading,
                    onClick = {
                        performHaptic(view, "success")
                        if (uiState.title.isNotEmpty()) {
                            viewModel.onEvent(AddEditEvent.OnSave)
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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .imePadding()
                        .padding(horizontal = 24.dp)
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedFormRow(index = 0, collisionState = collisionState) {
                        Spacer(Modifier.height(16.dp))
                    }
                    AnimatedFormRow(index = 1, collisionState = collisionState) {
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
                                        model = ImageRequest.Builder(ctx)
                                            .data(uiState.imageUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (imageFilePath != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(ctx)
                                            .data(imageFilePath)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
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
                    }
                    AnimatedFormRow(index = 2, collisionState = collisionState) {
                        Spacer(Modifier.height(32.dp))
                    }
                    AnimatedFormRow(index = 3, collisionState = collisionState) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                AnimatedOneUiTextField(
                                    value = uiState.title,
                                    onValueChange = { viewModel.onEvent(AddEditEvent.OnTitleChanged(it)) },
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
                    }
                    AnimatedFormRow(index = 4, collisionState = collisionState) {
                        Spacer(Modifier.height(24.dp))
                    }
                    AnimatedFormRow(index = 5, collisionState = collisionState) {
                        AnimatedOneUiTextField(
                            value = uiState.episodes,
                            onValueChange = { viewModel.onEvent(AddEditEvent.OnEpisodesChanged(it)) },
                            placeholder = "Episodes watched",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    AnimatedFormRow(index = 6, collisionState = collisionState) {
                        Spacer(Modifier.height(12.dp))
                    }
                    AnimatedFormRow(index = 7, collisionState = collisionState) {
                        EpisodeSuggestions { selectedEp ->
                            performHaptic(view, "light")
                            viewModel.onEvent(AddEditEvent.OnEpisodesChanged(selectedEp))
                        }
                    }
                    AnimatedFormRow(index = 8, collisionState = collisionState) {
                        Spacer(Modifier.height(32.dp))
                    }
                    AnimatedFormRow(index = 9, collisionState = collisionState) {
                        Text(
                            text = "Category & Genres",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start
                        )
                    }
                    AnimatedFormRow(index = 10, collisionState = collisionState) {
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
                                        viewModel.onEvent(AddEditEvent.OnTagsChanged(emptyList(), ""))
                                    } else {
                                        viewModel.onEvent(AddEditEvent.OnTagsChanged(currentTags, uiState.categoryType))
                                    }
                                } else {
                                    if (currentTags.size < 3 && (uiState.categoryType.isEmpty() || uiState.categoryType == categoryType)) {
                                        currentTags.add(tag)
                                        viewModel.onEvent(AddEditEvent.OnTagsChanged(currentTags, categoryType))
                                    }
                                }
                                performHaptic(view, "light")
                            }
                        )
                    }
                    AnimatedFormRow(index = 11, collisionState = collisionState) {
                        Spacer(Modifier.height(32.dp))
                    }
                    AnimatedFormRow(index = 12, collisionState = collisionState) {
                        StarRatingBar(rating = uiState.rating) { newRate ->
                            performHaptic(view, "light")
                            viewModel.onEvent(AddEditEvent.OnRatingChanged(newRate))
                        }
                    }
                    AnimatedFormRow(index = 13, collisionState = collisionState) {
                        Spacer(Modifier.height(24.dp))
                    }
                    AnimatedFormRow(index = 14, collisionState = collisionState) {
                        CommentMorphingContainer(
                            state = uiState,
                            hazeState = commentHazeState,
                            onModeChange = { viewModel.onEvent(AddEditEvent.OnCommentModeChanged(it)) },
                            onSaveComment = { viewModel.onEvent(AddEditEvent.OnSaveComment(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(120.dp))
                }
            }
        }
    }
}

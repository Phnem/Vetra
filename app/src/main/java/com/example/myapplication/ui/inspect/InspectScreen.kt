package com.example.myapplication.ui.inspect

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.example.myapplication.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.myapplication.domain.inspect.InspectContentMode
import com.example.myapplication.ui.home.ApiSearchResultCard
import com.example.myapplication.ui.settings.SegmentedOption
import com.example.myapplication.isAppInDarkTheme
import com.example.myapplication.ui.shared.theme.AccentMauveDark
import com.example.myapplication.ui.shared.theme.AccentMauveLight
import com.example.myapplication.ui.shared.theme.AccentOnMauveDark
import com.example.myapplication.ui.shared.theme.AccentOnMauveLight
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun InspectScreen(
    navController: NavController,
    viewModel: InspectViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lang by viewModel.uiLanguage.collectAsStateWithLifecycle()
    val strings = getStrings(lang)
    val contentMode by viewModel.contentMode.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    val addingId by viewModel.addingFromApiId.collectAsStateWithLifecycle()
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val isDark = isAppInDarkTheme()
    val backBubbleColor = if (isDark) AccentMauveDark else AccentMauveLight
    val backArrowColor = if (isDark) AccentOnMauveDark else AccentOnMauveLight
    val capsuleAccent = if (isDark) AccentMauveLight else AccentMauveDark
    val capsulePill = if (isDark) AccentMauveDark else AccentMauveLight
    val inspectHazeState = remember { HazeState() }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.analyzeImage(context, it) }
    }

    BackHandler { navController.popBackStack() }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "inspect_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(32.dp))
                )
                .clip(RoundedCornerShape(32.dp))
                .hazeSource(inspectHazeState)
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
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(backBubbleColor)
                            .clickable {
                                performHaptic(view, "light")
                                navController.popBackStack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.inspectBack,
                            tint = backArrowColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.frame_inspect_24),
                        contentDescription = null,
                        tint = textC,
                        modifier = Modifier
                            .size(30.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "inspect_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )

                    Text(
                        text = strings.inspectTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textC,
                        modifier = Modifier.weight(1f)
                    )
                }

                val animeLabel = strings.inspectModeAnime
                val moviesLabel = strings.inspectModeMoviesTv
                InspectCapsuleChipRow(
                    options = listOf(
                        SegmentedOption(label = animeLabel) { },
                        SegmentedOption(label = moviesLabel) { }
                    ),
                    selectedIndex = if (contentMode == InspectContentMode.Anime) 0 else 1,
                    accentColor = capsuleAccent,
                    pillColor = capsulePill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    onOptionClick = { index ->
                        performHaptic(view, "light")
                        when (index) {
                            0 -> viewModel.setContentMode(InspectContentMode.Anime)
                            1 -> viewModel.setContentMode(InspectContentMode.MoviesSeries)
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            performHaptic(view, "light")
                            pickLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedUri != null -> {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    performHaptic(view, "light")
                                    viewModel.clearPreviewAndResults()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isAppInDarkTheme()) AccentMauveDark else AccentMauveLight),
                                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                    contentDescription = strings.inspectClearPhoto,
                                    tint = Color.White
                                )
                            }
                        }
                        else -> {
                            Text(
                                text = strings.inspectPickScreenshot,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (val s = uiState) {
                        is InspectUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(s.message, color = textC)
                                }
                            }
                        }
                        is InspectUiState.Error -> {
                            Text(
                                text = s.message,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                        is InspectUiState.Idle -> { /* empty */ }
                        is InspectUiState.Success -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = s.results,
                                    key = { m -> "${m.result.source}_${m.result.externalId ?: m.result.title}" }
                                ) { uiModel ->
                                    val r = uiModel.result
                                    val addKey = "${r.source}_${r.externalId ?: r.title}"
                                    ApiSearchResultCard(
                                        result = r,
                                        isAdded = uiModel.isAdded,
                                        isLoading = addingId == addKey,
                                        displayGenres = null,
                                        addLabel = strings.addButton,
                                        addedLabel = strings.addedButton,
                                        onAddClick = {
                                            performHaptic(view, "light")
                                            viewModel.addFromApi(r)
                                        },
                                        modifier = Modifier.fillMaxWidth()
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

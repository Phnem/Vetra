package com.example.myapplication.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.myapplication.ui.shared.customOverscroll
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.GlassActionDock
import com.example.myapplication.GlassBottomNavigation
import com.example.myapplication.GenreFilterOverlay
import com.example.myapplication.NotificationSyncOverlay
import com.example.myapplication.SimpGlassCard
import com.example.myapplication.SortFilterOverlay
import com.example.myapplication.data.models.*
import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import com.example.myapplication.ui.details.AnimeDetailsSheet
import com.example.myapplication.ui.details.DetailsViewModel
import com.example.myapplication.ui.navigation.navigateToAddEdit
import com.example.myapplication.ui.navigation.navigateToWelcome
import com.example.myapplication.ui.shared.theme.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    detailsViewModel: DetailsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val genreRepository: GenreRepository = koinInject()
    val settingsVm: com.example.myapplication.ui.settings.SettingsViewModel = org.koin.androidx.compose.koinViewModel()
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
    val currentLanguage = settingsState.language
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val kbd = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    val hazeState = remember { HazeState() }

    var showCSheet by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var showNotificationsOverlay by remember { mutableStateOf(false) }
    val notifVisibleState = remember { MutableTransitionState(false) }
    notifVisibleState.targetState = showNotificationsOverlay

    var showSortOverlay by remember { mutableStateOf(false) }
    val sortVisibleState = remember { MutableTransitionState(false) }
    sortVisibleState.targetState = showSortOverlay

    val genreFilterVisibleState = remember { MutableTransitionState(false) }
    genreFilterVisibleState.targetState = uiState.isGenreFilterVisible

    val searchFocusRequester = remember { FocusRequester() }
    var animeToDelete by remember { mutableStateOf<Anime?>(null) }
    var animeToFavorite by remember { mutableStateOf<Anime?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    var isDockVisible by remember { mutableStateOf(true) }
    val finalDockVisible = isDockVisible || isSearchVisible
    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val threshold = 10f
                if (available.y < -threshold) {
                    if (isDockVisible) isDockVisible = false
                } else if (available.y > threshold) {
                    if (!isDockVisible) isDockVisible = true
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    BackHandler(enabled = isSearchVisible || uiState.searchQuery.isNotEmpty()) {
        if (isSearchVisible) {
            performHaptic(view, "light")
            isSearchVisible = false
            viewModel.updateSearchQuery("")
            focusManager.clearFocus()
            kbd?.hide()
        }
    }

    val listState = rememberLazyListState()
    var overscrollAmount by remember { mutableFloatStateOf(0f) }
    val isHeaderFloating by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 } }
    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val bgColor = MaterialTheme.colorScheme.background

    val showDetails = uiState.selectedAnimeForDetails != null
    val shouldBlur = (isSearchVisible && uiState.searchQuery.isBlank()) ||
            showCSheet || animeToDelete != null || animeToFavorite != null ||
            uiState.isGenreFilterVisible || showNotificationsOverlay || showSortOverlay ||
            showDetails
    val blurAmount by animateDpAsState(targetValue = if (shouldBlur) 10.dp else 0.dp, label = "blur")

    // Details overlay shown on top of everything
    if (showDetails) {
        val selectedAnime = uiState.selectedAnimeForDetails!!
        AnimeDetailsSheet(
            viewModel = detailsViewModel,
            anime = selectedAnime,
            language = currentLanguage,
            getImgPath = { name -> viewModel.getImgPath(name) },
            onDismiss = { viewModel.clearSelectedAnime() }
        )
    }

    Scaffold(containerColor = Color.Transparent, bottomBar = {}, floatingActionButton = {}) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().background(bgColor))

            Box(modifier = Modifier.zIndex(6f).align(Alignment.TopEnd).padding(end = 16.dp)) {
                GlassActionDock(
                    hazeState = hazeState,
                    isFloating = isHeaderFloating,
                    sortOption = uiState.sortOption,
                    filterSelectedTags = uiState.filterTags,
                    updates = uiState.updates,
                    onOpenSort = {
                        performHaptic(view, "light")
                        showSortOverlay = !showSortOverlay
                        if (showSortOverlay) {
                            showNotificationsOverlay = false
                            viewModel.setGenreFilterVisible(false)
                        }
                    },
                    onOpenNotifications = {
                        performHaptic(view, "light")
                        showNotificationsOverlay = !showNotificationsOverlay
                        if (showNotificationsOverlay) {
                            showSortOverlay = false
                            viewModel.setGenreFilterVisible(false)
                        }
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (notifVisibleState.currentState || notifVisibleState.targetState) {
                Box(modifier = Modifier.zIndex(5f).fillMaxSize()) {
                    NotificationSyncOverlay(
                        visibleState = notifVisibleState,
                        strings = getStrings(currentLanguage),
                        updates = uiState.updates,
                        onDismiss = { showNotificationsOverlay = false },
                        onLogout = {
                            DropboxSyncManager.logout()
                            navController.navigateToWelcome()
                        }
                    )
                }
            }

            if (sortVisibleState.currentState || sortVisibleState.targetState) {
                Box(modifier = Modifier.zIndex(5f).fillMaxSize()) {
                    SortFilterOverlay(
                        visibleState = sortVisibleState,
                        strings = getStrings(currentLanguage),
                        sortOption = uiState.sortOption,
                        sortAscending = uiState.sortAscending,
                        filterSelectedTags = uiState.filterTags,
                        filterCategoryType = uiState.filterCategory,
                        currentLanguage = currentLanguage,
                        onDismiss = { showSortOverlay = false },
                        onSortSelected = { option ->
                            performHaptic(view, "light")
                            viewModel.updateSortOption(option)
                        },
                        onOpenGenreFilter = {
                            performHaptic(view, "light")
                            showSortOverlay = false
                            viewModel.toggleGenreFilter()
                        }
                    )
                }
            }

            if (genreFilterVisibleState.currentState || genreFilterVisibleState.targetState) {
                Box(modifier = Modifier.zIndex(5f).fillMaxSize()) {
                    GenreFilterOverlay(
                        visibleState = genreFilterVisibleState,
                        strings = getStrings(currentLanguage),
                        filterSelectedTags = uiState.filterTags,
                        filterCategoryType = uiState.filterCategory,
                        currentLanguage = currentLanguage,
                        getGenreName = { genreId -> genreRepository.getLabel(genreId, currentLanguage) },
                        onTagToggle = { tag, categoryType ->
                            val currentTags = uiState.filterTags.toMutableList()
                            if (currentTags.contains(tag)) {
                                currentTags.remove(tag)
                                val newCategory = if (currentTags.isEmpty()) "" else uiState.filterCategory
                                viewModel.updateFilterTags(currentTags, newCategory)
                            } else {
                                if (currentTags.size < 3 && (uiState.filterCategory.isEmpty() || uiState.filterCategory == categoryType)) {
                                    currentTags.add(tag)
                                    viewModel.updateFilterTags(currentTags, categoryType)
                                }
                            }
                        },
                        onDismiss = { viewModel.toggleGenreFilter() }
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize().blur(blurAmount)) {
                Box(modifier = Modifier.fillMaxSize().weight(1f).background(bgColor)) {
                    val list by viewModel.animeListFlow.collectAsState(initial = emptyList())
                    val isEmpty = list.isEmpty()
                    if (isEmpty) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            MalistWorkspaceTopBar(
                                strings = getStrings(currentLanguage),
                                userAvatarPath = null,
                                onSaveUserAvatar = { uri ->
                                    // TODO: Implement avatar save
                                }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                EmptyStateView(
                                    title = if (uiState.searchQuery.isNotEmpty()) "No results" else "Nothing in folder",
                                    subtitle = if (uiState.searchQuery.isNotEmpty()) "" else "Looks empty over here."
                                )
                            }
                        }
                    } else {
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                                .customOverscroll(listState) { overscrollAmount = it }
                                .offset { IntOffset(0, overscrollAmount.roundToInt()) }
                        ) {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(top = 0.dp, bottom = 220.dp, start = 0.dp, end = 0.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.haze(state = hazeState)
                            ) {
                                item {
                            MalistWorkspaceTopBar(
                                strings = getStrings(currentLanguage),
                                userAvatarPath = null,
                                onSaveUserAvatar = { uri ->
                                    // TODO: Implement avatar save
                                }
                            )
                                }
                                items(
                                    items = list,
                                    key = { it.id }
                                ) { anime ->
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
                                        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
                                    )
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = { SwipeBackground(dismissState) },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem()
                                    ) {
                                        OneUiAnimeCard(
                                            anime = anime,
                                            getImgPath = { name -> viewModel.getImgPath(name) },
                                            getGenreName = { genreId -> genreRepository.getLabel(genreId, currentLanguage) },
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            onClick = { navController.navigateToAddEdit(anime.id) },
                                            onDetailsClick = {
                                                performHaptic(view, "light")
                                                viewModel.selectAnimeForDetails(anime)
                                                detailsViewModel.loadDetails(anime, currentLanguage)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }

            if (!isSearchVisible && animeToDelete == null && animeToFavorite == null) {
                Box(modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f).navigationBarsPadding()) {
                    AnimatedVisibility(
                        visible = finalDockVisible,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn(animationSpec = tween(250)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
                        ) + fadeOut(animationSpec = tween(200))
                    ) {
                        GlassBottomNavigation(
                            hazeState = hazeState,
                            nav = navController,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onShowStats = { showCSheet = true },
                            onShowNotifs = {},
                            onSearchClick = {
                                performHaptic(view, "light")
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) {
                                    viewModel.updateSearchQuery("")
                                    focusManager.clearFocus()
                                    kbd?.hide()
                                }
                            },
                            isSearchActive = isSearchVisible,
                            modifier = Modifier
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isSearchVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).windowInsetsPadding(WindowInsets.ime).padding(bottom = 16.dp).zIndex(10f)
            ) {
                SimpGlassCard(hazeState = hazeState, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = {
                            viewModel.updateSearchQuery(it)
                            if (it.isNotEmpty()) performHaptic(view, "light")
                        },
                        modifier = Modifier.fillMaxSize().focusRequester(searchFocusRequester).padding(horizontal = 20.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = SnProFamily),
                        cursorBrush = SolidColor(BrandBlue),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = BrandBlue)
                                Spacer(Modifier.width(12.dp))
                                Box {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text("Search in collection...", color = MaterialTheme.colorScheme.secondary, fontSize = 16.sp, fontFamily = SnProFamily)
                                    }
                                    innerTextField()
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { kbd?.hide() })
                    )
                }
                LaunchedEffect(Unit) {
                    searchFocusRequester.requestFocus()
                    kbd?.show()
                }
            }

            if (shouldBlur && animeToDelete == null && animeToFavorite == null && uiState.selectedAnimeForDetails == null && !showNotificationsOverlay && !showSortOverlay && !uiState.isGenreFilterVisible) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    focusManager.clearFocus()
                    isSearchVisible = false
                    kbd?.hide()
                }.zIndex(2f))
            }

            if (animeToDelete != null) {
                SpringBottomDialog(
                    title = "Delete title?",
                    subtitle = "This action cannot be undone.",
                    confirmText = "Delete",
                    cancelText = "Cancel",
                    icon = Icons.Default.Delete,
                    accentColor = BrandRed,
                    imageFile = viewModel.getImgPath(animeToDelete?.imageFileName),
                    onConfirm = {
                        viewModel.deleteAnime(animeToDelete!!.id)
                        animeToDelete = null
                    },
                    onCancel = { animeToDelete = null }
                )
            }
            if (animeToFavorite != null) {
                SpringBottomDialog(
                    title = "Add to favorites?",
                    subtitle = "Future you will thank you :)",
                    confirmText = "Add to favorites",
                    cancelText = "Cancel",
                    icon = Icons.Rounded.Star,
                    accentColor = RateColor3,
                    imageFile = viewModel.getImgPath(animeToFavorite?.imageFileName),
                    onConfirm = {
                        viewModel.toggleFavorite(animeToFavorite!!.id)
                        animeToFavorite = null
                    },
                    onCancel = { animeToFavorite = null }
                )
            }

            AnimatedVisibility(
                visible = showScrollToTop && !isSearchVisible && animeToDelete == null && animeToFavorite == null,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 160.dp, end = 24.dp).zIndex(1f)
            ) {
                SimpGlassCard(
                    hazeState = hazeState,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp).clickable {
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
            LaunchedEffect(showCSheet) {
                if (showCSheet) viewModel.loadStatsAnimeList()
            }
            StatsOverlay(
                animeList = uiState.statsAnimeList,
                strings = getStrings(currentLanguage),
                onDismiss = { showCSheet = false }
            )
        }
    }
}

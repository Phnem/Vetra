package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// SORT & FILTER OVERLAYS (Unified Design)
// ==========================================

// --- ЦВЕТОВАЯ ПАЛИТРА (Единая для всех меню) ---
private val CustomDarkSurface = Color(0xFF1F222B)
private val CustomDarkCard = Color(0xFF262A35)
private val CustomDarkBorder = Color.White.copy(alpha = 0.08f)

// Цвета акцентов
private val ColorDate = Color(0xFF3E82F7)
private val ColorRating = Color(0xFFFFD60A)
private val ColorAZ = Color(0xFF5E5CE6)
private val ColorFav = Color(0xFFFF453A)
private val IconFilterColor = Color(0xFFE91E63)

// Цвета категорий жанров
private val ColorAnime = Color(0xFFFF2D55)
private val ColorMovies = Color(0xFF5AC8FA)
private val ColorSeries = Color(0xFFFFCC00)

// ==========================================
// 1. МЕНЮ СОРТИРОВКИ (Справа сверху)
// ==========================================
@Composable
fun SortFilterOverlay(
    visibleState: MutableTransitionState<Boolean>,
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onOpenGenreFilter: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val strings = viewModel.strings

    val panelBgColor = if (isDark) CustomDarkSurface else MaterialTheme.colorScheme.surface
    val itemCardColor = if (isDark) CustomDarkCard else MaterialTheme.colorScheme.surfaceVariant
    val itemBorderColor = if (isDark) CustomDarkBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = scaleOut(
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(200)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 90.dp, end = 16.dp)
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = panelBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    border = if (isDark) BorderStroke(1.dp, itemBorderColor) else null,
                    modifier = Modifier.clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Sorting & Filters",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, top = 4.dp)
                        )

                        Text(
                            "Sort by",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                        )

                        SortOption.values().forEach { option ->
                            val isSelected = viewModel.sortOption == option
                            val (icon, accentColor) = when (option) {
                                SortOption.DATE_NEWEST -> Icons.Default.DateRange to ColorDate
                                SortOption.RATING_HIGH -> Icons.Default.Star to ColorRating
                                SortOption.AZ -> Icons.AutoMirrored.Filled.Sort to ColorAZ
                                SortOption.FAVORITES -> Icons.Default.Favorite to ColorFav
                            }
                            SortPillCard(
                                icon = icon,
                                title = option.getLabel(strings),
                                isSelected = isSelected,
                                cardColor = if (isSelected) accentColor.copy(alpha = 0.1f) else itemCardColor,
                                borderColor = if (isSelected) accentColor.copy(alpha = 0.5f) else itemBorderColor,
                                iconBgColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.15f),
                                iconTintColor = if (isSelected) Color.White else accentColor,
                                onClick = {
                                    onSortSelected(option)
                                    onDismiss()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        Text(
                            "Filters",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                        )

                        val activeFiltersCount = viewModel.filterSelectedTags.size
                        SortPillCard(
                            icon = Icons.Outlined.FilterList,
                            title = strings.filterByGenre,
                            subtitle = if (activeFiltersCount > 0) "$activeFiltersCount active tags" else "No filters active",
                            isSelected = activeFiltersCount > 0,
                            cardColor = itemCardColor,
                            borderColor = itemBorderColor,
                            iconBgColor = IconFilterColor.copy(alpha = 0.15f),
                            iconTintColor = IconFilterColor,
                            onClick = {
                                onOpenGenreFilter()
                                onDismiss()
                            },
                            contentEnd = {
                                if (activeFiltersCount > 0) Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(IconFilterColor)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeFiltersCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. МЕНЮ ЖАНРОВ (По центру/Снизу)
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenreFilterOverlay(
    visibleState: MutableTransitionState<Boolean>,
    viewModel: AnimeViewModel,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val strings = viewModel.strings

    val panelBgColor = if (isDark) CustomDarkSurface else MaterialTheme.colorScheme.surface
    val itemCardColor = if (isDark) CustomDarkCard else MaterialTheme.colorScheme.surfaceVariant
    val itemBorderColor = if (isDark) CustomDarkBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val buttonColor = BrandBlue

    val animeGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.ANIME) }
    val movieGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.MOVIE) }
    val seriesGenres = remember { GenreRepository.getGenresForCategory(GenreCategory.SERIES) }

    var expandedCategory by remember { mutableStateOf<String?>(null) }

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
    }

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(200)
            ) + fadeOut(),
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = panelBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = if (isDark) BorderStroke(1.dp, itemBorderColor) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        strings.filterByGenre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                    )

                    ExpandableGenrePill(
                        title = strings.genreAnime,
                        icon = Icons.Outlined.Animation,
                        accentColor = ColorAnime,
                        isExpanded = expandedCategory == "Anime",
                        isActive = viewModel.filterCategoryType == "Anime" || viewModel.filterCategoryType.isEmpty(),
                        genres = animeGenres,
                        selectedTags = viewModel.filterSelectedTags,
                        viewModel = viewModel,
                        cardColor = itemCardColor,
                        borderColor = itemBorderColor,
                        onExpand = {
                            expandedCategory = if (expandedCategory == "Anime") null else "Anime"
                        },
                        onTagToggle = { id -> onTagToggle(id, "Anime") }
                    )
                    Spacer(Modifier.height(12.dp))

                    ExpandableGenrePill(
                        title = strings.genreMovies,
                        icon = Icons.Outlined.Movie,
                        accentColor = ColorMovies,
                        isExpanded = expandedCategory == "Movies",
                        isActive = viewModel.filterCategoryType == "Movies" || viewModel.filterCategoryType.isEmpty(),
                        genres = movieGenres,
                        selectedTags = viewModel.filterSelectedTags,
                        viewModel = viewModel,
                        cardColor = itemCardColor,
                        borderColor = itemBorderColor,
                        onExpand = {
                            expandedCategory = if (expandedCategory == "Movies") null else "Movies"
                        },
                        onTagToggle = { id -> onTagToggle(id, "Movies") }
                    )
                    Spacer(Modifier.height(12.dp))

                    ExpandableGenrePill(
                        title = strings.genreSeries,
                        icon = Icons.Outlined.Tv,
                        accentColor = ColorSeries,
                        isExpanded = expandedCategory == "Series",
                        isActive = viewModel.filterCategoryType == "Series" || viewModel.filterCategoryType.isEmpty(),
                        genres = seriesGenres,
                        selectedTags = viewModel.filterSelectedTags,
                        viewModel = viewModel,
                        cardColor = itemCardColor,
                        borderColor = itemBorderColor,
                        onExpand = {
                            expandedCategory = if (expandedCategory == "Series") null else "Series"
                        },
                        onTagToggle = { id -> onTagToggle(id, "Series") }
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) {
                        Text(
                            "Apply",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ
// ==========================================

@Composable
private fun SortPillCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isSelected: Boolean,
    cardColor: Color,
    borderColor: Color,
    iconBgColor: Color,
    iconTintColor: Color,
    modifier: Modifier = Modifier,
    contentEnd: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(cardColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSystemInDarkTheme()) Color.White else Color.Black
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSystemInDarkTheme()) Color.Gray else Color.DarkGray,
                    maxLines = 1
                )
            }
        }
        if (contentEnd != null) {
            Spacer(Modifier.width(8.dp))
            contentEnd()
        } else if (isSelected) {
            Icon(
                Icons.Default.Check,
                null,
                tint = if (isSystemInDarkTheme()) Color.White else Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandableGenrePill(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    isActive: Boolean,
    genres: List<GenreDefinition>,
    selectedTags: List<String>,
    viewModel: AnimeViewModel,
    cardColor: Color,
    borderColor: Color,
    onExpand: () -> Unit,
    onTagToggle: (String) -> Unit
) {
    val alpha = if (isActive) 1f else 0.4f
    val animatedShape by animateFloatAsState(
        if (isExpanded) 24f else 50f,
        label = "shape"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(animatedShape.dp))
            .background(cardColor)
            .border(
                1.dp,
                if (isExpanded) accentColor.copy(alpha = 0.5f) else borderColor,
                RoundedCornerShape(animatedShape.dp)
            )
            .clickable(enabled = isActive) { onExpand() }
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSystemInDarkTheme()) Color.White else Color.Black,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        }

        if (isExpanded) {
            HorizontalDivider(
                color = borderColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genreDef ->
                    val isSelected = selectedTags.contains(genreDef.id)
                    val displayName = if (viewModel.currentLanguage == AppLanguage.RU) genreDef.ru else genreDef.en

                    val chipBg = if (isSelected) accentColor else Color.Transparent
                    val chipBorder = if (isSelected) null else BorderStroke(1.dp, borderColor)
                    val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    val chipFontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(chipBg)
                            .then(if (chipBorder != null) Modifier.border(chipBorder, CircleShape) else Modifier)
                            .clickable { onTagToggle(genreDef.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = displayName,
                            color = chipText,
                            fontSize = 13.sp,
                            fontWeight = chipFontWeight
                        )
                    }
                }
            }
        }
    }
}
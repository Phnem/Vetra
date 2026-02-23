package com.example.myapplication

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.models.SortOption
import com.example.myapplication.data.models.UiStrings
import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.ui.navigation.navigateToAddEdit
import com.example.myapplication.ui.navigation.navigateToSettings
import com.example.myapplication.ui.shared.components.GenreSelectionSection
import com.example.myapplication.ui.shared.fluidClickable
import com.example.myapplication.ui.shared.theme.BrandBlue
import com.example.myapplication.ui.shared.theme.BrandRed
import com.example.myapplication.ui.shared.theme.SnProFamily
import com.example.myapplication.utils.performHaptic
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.CupertinoMaterials

@Composable
fun isAppInDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background.toArgb() == Color(0xFF111318).toArgb()
}
// ==========================================
// КОМПОНЕНТ "СИМП-СТЕКЛО"
// ==========================================
@Composable
fun SimpGlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isAppInDarkTheme()
    val shineColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.6f)
    val borderStroke = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = CupertinoMaterials.ultraThin()
            )
            .border(0.5.dp, borderStroke, shape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val rect = Rect(offset = Offset.Zero, size = size)
            val path = Path().apply {
                val radius = if (shape == CircleShape) size.height / 2 else 32.dp.toPx()
                addRoundRect(RoundRect(rect, CornerRadius(radius)))
            }
            drawPath(
                path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        shineColor,
                        Color.Transparent,
                        Color.Transparent,
                        shineColor.copy(alpha = 0.1f)
                    )
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        content()
    }
}

// ==========================================
// GLASSACTIONDOCK (Обновленный)
// ==========================================
@Composable
fun GlassActionDock(
    hazeState: HazeState,
    isFloating: Boolean,
    sortOption: SortOption,
    filterSelectedTags: List<String>,
    updates: List<AnimeUpdate>,
    onOpenSort: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    val topPadding by animateDpAsState(
        targetValue = if (isFloating) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dockPadding"
    )
    val effectAlpha by animateFloatAsState(
        targetValue = if (isFloating) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hazeAlpha"
    )
    val borderStrokeBase = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.8f)
    val borderColor by animateColorAsState(
        targetValue = if (isFloating) borderStrokeBase else Color.Transparent,
        label = "border"
    )
    val shineColorBase = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.6f)
    val shineAlpha by animateFloatAsState(
        targetValue = if (isFloating) 1f else 0f,
        label = "shineAlpha"
    )
    val buttonBgColor by animateColorAsState(
        targetValue = if (isFloating) Color.Transparent else
            if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else Color.Black.copy(alpha = 0.04f),
        label = "btnBg"
    )

    Box(
        modifier = modifier
            .padding(top = topPadding)
            .statusBarsPadding()
            .clip(RoundedCornerShape(32.dp))
            .hazeEffect(
                state = hazeState,
                style = CupertinoMaterials.ultraThin()
            ) {
                alpha = effectAlpha
            }
            .border(0.5.dp, borderColor, RoundedCornerShape(32.dp))
    ) {
        if (shineAlpha > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val rect = Rect(offset = Offset.Zero, size = size)
                val path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(32.dp.toPx()))) }
                drawPath(
                    path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            shineColorBase.copy(alpha = shineColorBase.alpha * shineAlpha),
                            Color.Transparent,
                            Color.Transparent,
                            shineColorBase.copy(alpha = 0.05f * shineAlpha)
                        )
                    ),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { onOpenSort() },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(buttonBgColor)
            ) {
                val icon = if (filterSelectedTags.isNotEmpty()) Icons.Outlined.FilterList else Icons.AutoMirrored.Filled.Sort
                val tint = if (filterSelectedTags.isNotEmpty()) BrandBlue else MaterialTheme.colorScheme.onSurface

                Icon(imageVector = icon, contentDescription = "Sort", tint = tint)
            }

            Box {
                IconButton(
                    onClick = { onOpenNotifications() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(buttonBgColor)
                ) {
                    Icon(
                        imageVector = HeroiconsRectangleStack,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (updates.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(8.dp)
                            .background(BrandRed, CircleShape)
                    )
                }
            }
        }
    }
}

// ==========================================
// ПАНЕЛЬ НАВИГАЦИИ (Compact Style)
// ==========================================
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassBottomNavigation(
    hazeState: HazeState,
    nav: androidx.navigation.NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowStats: () -> Unit,
    onShowNotifs: () -> Unit,
    onSearchClick: () -> Unit,
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = isAppInDarkTheme()
    val currentThemeColor = MaterialTheme.colorScheme.onSurface
    val borderStroke = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(64.dp)
                .wrapContentWidth()
                .clip(CircleShape)
                .hazeEffect(
                    state = hazeState,
                    style = CupertinoMaterials.ultraThin()
                )
                .border(0.5.dp, borderStroke, CircleShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                performHaptic(view, "light")
                                onShowStats()
                            }
                    ) {
                        Icon(
                            imageVector = HeroiconsSquaresPlus,
                            contentDescription = "Stats",
                            tint = currentThemeColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                performHaptic(view, "success")
                                nav.navigateToAddEdit()
                            }
                    ) {
                        with(sharedTransitionScope) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .sharedBounds(
                                        rememberSharedContentState(key = "fab_container"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                                    )
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = HeroiconsPlus,
                                    contentDescription = "Add",
                                    tint = currentThemeColor,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .sharedElement(
                                            rememberSharedContentState(key = "fab_icon"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                )
                            }
                        }
                    }
                }

                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .fluidClickable {
                                performHaptic(view, "light")
                                nav.navigateToSettings()
                            }
                    ) {
                        with(sharedTransitionScope) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .sharedBounds(
                                        rememberSharedContentState(key = "settings_container"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
                                    )
                                    .background(Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = currentThemeColor.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .sharedElement(
                                            rememberSharedContentState(key = "settings_icon"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        SimpGlassCard(
            hazeState = hazeState,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp)
                .size(64.dp)
                .clickable {
                    performHaptic(view, "light")
                    onSearchClick()
                }
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isSearchActive) BrandBlue else currentThemeColor,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ==========================================
// ВСТРОЕННЫЕ ИКОНКИ (Heroicons)
// ==========================================

private var _HeroiconsPlus: ImageVector? = null
val HeroiconsPlus: ImageVector
    get() {
        if (_HeroiconsPlus != null) return _HeroiconsPlus!!
        _HeroiconsPlus = ImageVector.Builder(
            name = "plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 20f,
            viewportHeight = 20f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.75f, 4.75f)
                arcToRelative(0.75f, 0.75f, 0f, false, false, -1.5f, 0f)
                verticalLineToRelative(4.5f)
                horizontalLineToRelative(-4.5f)
                arcToRelative(0.75f, 0.75f, 0f, false, false, 0f, 1.5f)
                horizontalLineToRelative(4.5f)
                verticalLineToRelative(4.5f)
                arcToRelative(0.75f, 0.75f, 0f, false, false, 1.5f, 0f)
                verticalLineToRelative(-4.5f)
                horizontalLineToRelative(4.5f)
                arcToRelative(0.75f, 0.75f, 0f, false, false, 0f, -1.5f)
                horizontalLineToRelative(-4.5f)
                verticalLineToRelative(-4.5f)
                close()
            }
        }.build()
        return _HeroiconsPlus!!
    }

private var _HeroiconsSquaresPlus: ImageVector? = null
val HeroiconsSquaresPlus: ImageVector
    get() {
        if (_HeroiconsSquaresPlus != null) return _HeroiconsSquaresPlus!!
        _HeroiconsSquaresPlus = ImageVector.Builder(
            name = "squares-plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(13.5f, 16.875f)
                horizontalLineToRelative(3.375f)
                moveToRelative(0f, 0f)
                horizontalLineToRelative(3.375f)
                moveToRelative(-3.375f, 0f)
                verticalLineTo(13.5f)
                moveToRelative(0f, 3.375f)
                verticalLineToRelative(3.375f)
                moveTo(6f, 10.5f)
                horizontalLineToRelative(2.25f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, 2.25f, -2.25f)
                verticalLineTo(6f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, -2.25f, -2.25f)
                horizontalLineTo(6f)
                arcTo(2.25f, 2.25f, 0f, false, false, 3.75f, 6f)
                verticalLineToRelative(2.25f)
                arcTo(2.25f, 2.25f, 0f, false, false, 6f, 10.5f)
                close()
                moveToRelative(0f, 9.75f)
                horizontalLineToRelative(2.25f)
                arcTo(2.25f, 2.25f, 0f, false, false, 10.5f, 18f)
                verticalLineToRelative(-2.25f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, -2.25f, -2.25f)
                horizontalLineTo(6f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, -2.25f, 2.25f)
                verticalLineTo(18f)
                arcTo(2.25f, 2.25f, 0f, false, false, 6f, 20.25f)
                close()
                moveToRelative(9.75f, -9.75f)
                horizontalLineTo(18f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, 2.25f, -2.25f)
                verticalLineTo(6f)
                arcTo(2.25f, 2.25f, 0f, false, false, 18f, 3.75f)
                horizontalLineToRelative(-2.25f)
                arcTo(2.25f, 2.25f, 0f, false, false, 13.5f, 6f)
                verticalLineToRelative(2.25f)
                arcToRelative(2.25f, 2.25f, 0f, false, false, 2.25f, 2.25f)
                close()
            }
        }.build()
        return _HeroiconsSquaresPlus!!
    }

private var _HeroiconsRectangleStack: ImageVector? = null
val HeroiconsRectangleStack: ImageVector
    get() {
        if (_HeroiconsRectangleStack != null) return _HeroiconsRectangleStack!!
        _HeroiconsRectangleStack = ImageVector.Builder(
            name = "rectangle-stack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Miter
            ) {
                moveTo(6f, 6.878f)
                verticalLineTo(6f)
                arcToRelative(2.25f, 2.25f, 0f, false, true, 2.25f, -2.25f)
                horizontalLineToRelative(7.5f)
                arcTo(2.25f, 2.25f, 0f, false, true, 18f, 6f)
                verticalLineToRelative(0.878f)
                moveToRelative(-12f, 0f)
                curveToRelative(0.235f, -0.083f, 0.487f, -0.128f, 0.75f, -0.128f)
                horizontalLineToRelative(10.5f)
                curveToRelative(0.263f, 0f, 0.515f, 0.045f, 0.75f, 0.128f)
                moveToRelative(-12f, 0f)
                arcTo(2.25f, 2.25f, 0f, false, false, 4.5f, 9f)
                verticalLineToRelative(0.878f)
                moveToRelative(13.5f, -3f)
                arcTo(2.25f, 2.25f, 0f, false, true, 19.5f, 9f)
                verticalLineToRelative(0.878f)
                moveToRelative(0f, 0f)
                arcToRelative(2.246f, 2.246f, 0f, false, false, -0.75f, -0.128f)
                horizontalLineTo(5.25f)
                curveToRelative(-0.263f, 0f, -0.515f, 0.045f, -0.75f, 0.128f)
                moveToRelative(15f, 0f)
                arcTo(2.25f, 2.25f, 0f, false, true, 21f, 12f)
                verticalLineToRelative(6f)
                arcToRelative(2.25f, 2.25f, 0f, false, true, -2.25f, 2.25f)
                horizontalLineTo(5.25f)
                arcTo(2.25f, 2.25f, 0f, false, true, 3f, 18f)
                verticalLineToRelative(-6f)
                curveToRelative(0f, -0.98f, 0.626f, -1.813f, 1.5f, -2.122f)
            }
        }.build()
        return _HeroiconsRectangleStack!!
    }

// ==========================================
// ЦВЕТА И РАСШИРЕНИЯ ДЛЯ СТАРОГО ДИЗАЙНА
// ==========================================
private val CustomDarkSurface = Color(0xFF1F222B)
private val CustomDarkCard = Color(0xFF262A35)
private val CustomDarkBorder = Color.White.copy(alpha = 0.08f)
private val IconFilterColor = Color(0xFFE91E63)

fun SortOption.getLabel(strings: UiStrings): String = when (this) {
    SortOption.RATING -> strings.ratingHigh
    SortOption.EPISODES -> strings.episodesHigh
    SortOption.TITLE -> strings.titleAZ
}

fun SortOption.getIcon(): ImageVector = when (this) {
    SortOption.RATING -> Icons.Rounded.Star
    SortOption.EPISODES -> Icons.Outlined.Tv
    SortOption.TITLE -> Icons.AutoMirrored.Filled.Sort
}

fun SortOption.getAccentColor(): Color = when (this) {
    SortOption.RATING -> Color(0xFFFFD60A)
    SortOption.EPISODES -> Color(0xFF3E82F7)
    SortOption.TITLE -> Color(0xFF5E5CE6)
}

// ==========================================
// SORT & FILTER OVERLAYS
// ==========================================

@Composable
fun SortFilterOverlay(
    visibleState: MutableTransitionState<Boolean>,
    strings: UiStrings,
    sortOption: SortOption,
    sortAscending: Boolean = false,
    filterSelectedTags: List<String>,
    filterCategoryType: String,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onOpenGenreFilter: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.toArgb() == Color(0xFF111318).toArgb()
    val panelBgColor = if (isDark) CustomDarkSurface else MaterialTheme.colorScheme.surface
    val itemCardColor = if (isDark) CustomDarkCard else MaterialTheme.colorScheme.surfaceVariant
    val itemBorderColor = if (isDark) CustomDarkBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
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
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = scaleOut(
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 90.dp, end = 16.dp)
                    .width(280.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = panelBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = BorderStroke(0.5.dp, if (isDark) itemBorderColor else Color.Black.copy(alpha = 0.06f)),
                    modifier = Modifier.clickable(enabled = false) {}
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Sort by",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                        )

                        SortOption.entries.forEach { option ->
                            val isSelected = sortOption == option
                            val accentColor = option.getAccentColor()
                            val directionIcon = if (isSelected) {
                                if (sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                            } else null

                            SortPillCard(
                                icon = option.getIcon(),
                                title = option.getLabel(strings),
                                isSelected = isSelected,
                                cardColor = if (isSelected) accentColor.copy(alpha = 0.1f) else itemCardColor,
                                borderColor = if (isSelected) accentColor.copy(alpha = 0.4f) else itemBorderColor,
                                iconBgColor = if (isSelected) accentColor else accentColor.copy(alpha = 0.12f),
                                iconTintColor = if (isSelected) Color.White else accentColor,
                                onClick = {
                                    onSortSelected(option)
                                    onDismiss()
                                },
                                contentEnd = {
                                    if (directionIcon != null) {
                                        Icon(
                                            imageVector = directionIcon,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        val activeFiltersCount = filterSelectedTags.size
                        SortPillCard(
                            icon = Icons.Outlined.FilterList,
                            title = strings.filterByGenre,
                            isSelected = activeFiltersCount > 0,
                            cardColor = itemCardColor,
                            borderColor = itemBorderColor,
                            iconBgColor = IconFilterColor.copy(alpha = 0.12f),
                            iconTintColor = IconFilterColor,
                            onClick = {
                                onOpenGenreFilter()
                                onDismiss()
                            },
                            contentEnd = {
                                if (activeFiltersCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(IconFilterColor)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = activeFiltersCount.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
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
// КОМПОНЕНТ КАРТОЧКИ СОРТИРОВКИ
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
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }
        }
        if (contentEnd != null) {
            Spacer(Modifier.width(8.dp))
            contentEnd()
        } else if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun GenreFilterOverlay(
    visibleState: MutableTransitionState<Boolean>,
    strings: UiStrings,
    filterSelectedTags: List<String>,
    filterCategoryType: String,
    currentLanguage: AppLanguage,
    getGenreName: (String) -> String,
    onTagToggle: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val genreRepository: GenreRepository = org.koin.compose.koinInject()
    val isDark = isAppInDarkTheme()
    val panelBg = if (isDark) Color(0xFF1F222B) else MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    
    BackHandler { onDismiss() }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150))
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
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = panelBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = if (isDark) BorderStroke(1.dp, borderColor) else null
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = strings.filterByGenre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    GenreSelectionSection(
                        strings = strings,
                        currentLanguage = currentLanguage,
                        selectedTags = filterSelectedTags,
                        activeCategory = filterCategoryType,
                        onTagToggle = onTagToggle
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text(
                            "Done",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = SnProFamily
                        )
                    }
                }
            }
        }
    }
}
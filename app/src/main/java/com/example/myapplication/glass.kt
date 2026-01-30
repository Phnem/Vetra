package com.example.myapplication

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

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
    val isDark = isSystemInDarkTheme()

    val glassTint = if (isDark) {
        Color.Black.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    val shineColor = if (isDark) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.7f)
    val borderStroke = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .clip(shape)
            .hazeChild(
                state = hazeState,
                shape = shape,
                style = HazeStyle(
                    blurRadius = 20.dp,
                    tint = glassTint
                )
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
                    colors = listOf(shineColor, Color.Transparent, Color.Transparent, shineColor.copy(alpha = 0.1f))
                ),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        content()
    }
}

// ==========================================
// GLASSACTIONDOCK (МЕНЮ СОРТИРОВКИ И ФИЛЬТРА)
// ==========================================
@Composable
fun GlassActionDock(
    hazeState: HazeState,
    isFloating: Boolean,
    sortOption: SortOption,
    viewModel: AnimeViewModel,
    onSortSelected: (SortOption) -> Unit,
    modifier: Modifier = Modifier
){
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val topPadding by animateDpAsState(
        targetValue = if (isFloating) 16.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dockPadding"
    )

    val targetTint = if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)
    val tintColor by animateColorAsState(targetValue = if (isFloating) targetTint else Color.Transparent, label = "tint")
    val blurRadius by animateDpAsState(targetValue = if (isFloating) 30.dp else 0.dp, label = "blur")
    val borderStrokeBase = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else Color.Transparent
    val borderColor by animateColorAsState(targetValue = if (isFloating) borderStrokeBase else Color.Transparent, label = "border")
    val shineColorBase = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.6f)
    val shineAlpha by animateFloatAsState(targetValue = if (isFloating) 1f else 0f, label = "shineAlpha")
    val buttonBgColor by animateColorAsState(targetValue = if (isFloating) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), label = "btnBg")

    var expandedSort by remember { mutableStateOf(false) }
    var expandedNotifs by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(top = topPadding)
            .statusBarsPadding()
            .clip(RoundedCornerShape(32.dp))
            .hazeChild(
                state = hazeState,
                shape = RoundedCornerShape(32.dp),
                style = HazeStyle(blurRadius = blurRadius, tint = tintColor)
            )
            .border(0.5.dp, borderColor, RoundedCornerShape(32.dp))
    ) {
        if (shineAlpha > 0f) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val rect = Rect(offset = Offset.Zero, size = size)
                val path = Path().apply { addRoundRect(RoundRect(rect, CornerRadius(32.dp.toPx()))) }
                drawPath(path, brush = Brush.verticalGradient(colors = listOf(shineColorBase.copy(alpha = shineColorBase.alpha * shineAlpha), Color.Transparent, Color.Transparent, shineColorBase.copy(alpha = 0.05f * shineAlpha))), style = Stroke(width = 1.dp.toPx()))
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // КНОПКА СОРТИРОВКИ
            Box {
                IconButton(onClick = { expandedSort = true }, modifier = Modifier.size(44.dp).clip(CircleShape).background(buttonBgColor)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                }
                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))) {
                    DropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false },
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                        offset = DpOffset(x = 0.dp, y = 8.dp)
                    ) {
                        SortOption.values().forEach { option ->
                            val isSelected = sortOption == option
                            DropdownMenuItem(
                                text = { Text(text = option.getLabel(viewModel.strings), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                trailingIcon = {
                                    val icon = when (option) {
                                        SortOption.DATE_NEWEST -> Icons.Default.DateRange
                                        SortOption.RATING_HIGH -> Icons.Default.Star
                                        SortOption.AZ -> Icons.AutoMirrored.Filled.Sort
                                        SortOption.FAVORITES -> Icons.Default.Favorite
                                    }
                                    Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                },
                                onClick = { onSortSelected(option); expandedSort = false }
                            )
                        }

                        // ПУНКТ: ФИЛЬТР ПО ЖАНРАМ
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(viewModel.strings.filterByGenre, fontWeight = FontWeight.Bold)
                                    if (viewModel.filterSelectedTags.isNotEmpty()) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(CircleShape).background(BrandBlue).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(
                                                text = viewModel.filterSelectedTags.size.toString(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                expandedSort = false
                                viewModel.isGenreFilterVisible = true
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.FilterList, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        )
                    }
                }
            }

            // КНОПКА УВЕДОМЛЕНИЙ
            Box {
                IconButton(
                    onClick = { expandedNotifs = true },
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(buttonBgColor)
                ) {
                    Icon(
                        imageVector = HeroiconsRectangleStack,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (viewModel.updates.isNotEmpty()) {
                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(8.dp).background(BrandRed, CircleShape))
                }

                val isRu = viewModel.currentLanguage == AppLanguage.RU
                val txtHeader = if (isRu) "Обновление контента" else "Content update"
                val txtSubHeader = if (isRu) "Проверяю, вышли ли новые серии" else "Checking if new episodes are available"
                val txtEmptyTitle = if (isRu) "Кажется, ничего нового не вышло" else "Looks like nothing new was released"
                val txtEmptyBody = if (isRu) "Я проверил несколько разных API, похоже, ты обновлён!" else "I checked multiple APIs — looks like you're up to date!"

                MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(24.dp))) {
                    DropdownMenu(
                        expanded = expandedNotifs,
                        onDismissRequest = { expandedNotifs = false },
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                        offset = DpOffset(x = 0.dp, y = 8.dp),
                        modifier = Modifier.widthIn(min = 320.dp, max = 360.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(txtHeader, fontWeight = FontWeight.Black, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(4.dp))
                                    Text(txtSubHeader, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            },
                            onClick = {},
                            enabled = false
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        if (viewModel.isCheckingUpdates) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = BrandBlue)
                            }
                        } else if (viewModel.updates.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(RateColor4.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, null, tint = RateColor4, modifier = Modifier.size(36.dp))
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(txtEmptyTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(8.dp))
                                Text(txtEmptyBody, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary, lineHeight = 20.sp)
                            }
                        } else {
                            viewModel.updates.forEach { update ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(update.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                                            Text("${update.newEpisodes} eps (You: ${update.currentEpisodes})", style = MaterialTheme.typography.bodyMedium, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    trailingIcon = {
                                        Row {
                                            IconButton(onClick = { viewModel.dismissUpdate(update) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = BrandRed, modifier = Modifier.size(20.dp)) }
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = { viewModel.acceptUpdate(update, context); Toast.makeText(context, "${viewModel.strings.updatedToast}${update.title}!", Toast.LENGTH_SHORT).show() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Check, null, tint = BrandBlue, modifier = Modifier.size(20.dp)) }
                                        }
                                    },
                                    onClick = {},
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
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
    viewModel: AnimeViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onShowStats: () -> Unit,
    onShowNotifs: () -> Unit,
    onSearchClick: () -> Unit,
    isSearchActive: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val currentThemeColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .fillMaxWidth()
    ) {
        // 1. ЦЕНТРАЛЬНАЯ КАПСУЛА (Меню)
        SimpGlassCard(
            hazeState = hazeState,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(64.dp)
                .wrapContentWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 1. Stats
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { performHaptic(view, "light"); onShowStats() }) {
                        Icon(imageVector = HeroiconsSquaresPlus, contentDescription = "Stats", tint = currentThemeColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
                    }
                }

                // 2. Add
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { performHaptic(view, "success"); nav.navigate("add_anime") }) {
                        with(sharedTransitionScope) {
                            Box(modifier = Modifier.size(48.dp).sharedBounds(rememberSharedContentState(key = "fab_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds).background(Color.Transparent), contentAlignment = Alignment.Center) {
                                Icon(imageVector = HeroiconsPlus, contentDescription = "Add", tint = currentThemeColor, modifier = Modifier.size(36.dp).sharedElement(rememberSharedContentState(key = "fab_icon"), animatedVisibilityScope = animatedVisibilityScope))
                            }
                        }
                    }
                }

                // 3. Settings
                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().clip(CircleShape).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { performHaptic(view, "light"); nav.navigate("settings") }) {
                        with(sharedTransitionScope) {
                            Box(modifier = Modifier.size(48.dp).sharedBounds(rememberSharedContentState(key = "settings_container"), animatedVisibilityScope = animatedVisibilityScope, resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds).background(Color.Transparent), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings", tint = currentThemeColor.copy(alpha = 0.6f), modifier = Modifier.size(32.dp).sharedElement(rememberSharedContentState(key = "settings_icon"), animatedVisibilityScope = animatedVisibilityScope))
                            }
                        }
                    }
                }
            }
        }

        // 2. Кнопка поиска
        SimpGlassCard(
            hazeState = hazeState,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp).size(64.dp).clickable { performHaptic(view, "light"); onSearchClick() }
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = if(isSearchActive) BrandBlue else currentThemeColor, modifier = Modifier.size(32.dp))
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
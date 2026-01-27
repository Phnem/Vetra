package com.example.myapplication

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle

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
            .border(0.5.dp, borderStroke, shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

// ==========================================
// НОВАЯ ПАНЕЛЬ НАВИГАЦИИ (SPLIT STYLE)
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

    Row(
        modifier = modifier
            .padding(bottom = 24.dp, start = 75.dp, end = 75.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {

        // 1. БОЛЬШАЯ КАПСУЛА (Меню)
        SimpGlassCard(
            hazeState = hazeState,
            shape = CircleShape,
            modifier = Modifier
                .height(64.dp)
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 13.dp,
                        end = 14.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ------------------------------------------------
                // 1. Кнопка ЛЕВАЯ (Stats) -> ВЕС 1f
                // ------------------------------------------------
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Убрали Column, просто иконка в кликабельном боксе
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { performHaptic(view, false); onShowStats() }
                    ) {
                        Icon(
                            imageVector = HeroiconsSquaresPlus,
                            contentDescription = "Stats",
                            tint = currentThemeColor.copy(alpha = 0.6f),
                            // УВЕЛИЧИЛИ РАЗМЕР: было 24.dp -> стало 32.dp
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // ------------------------------------------------
                // 2. Кнопка ЦЕНТР (New) -> ВЕС 1f
                // ------------------------------------------------
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { performHaptic(view, false); nav.navigate("add_anime") }
                    ) {
                        with(sharedTransitionScope) {
                            Box(
                                modifier = Modifier
                                    // УВЕЛИЧИЛИ КОНТЕЙНЕР: было 28.dp -> стало 48.dp
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
                                    // УВЕЛИЧИЛИ ИКОНКУ: было 28.dp -> стало 36.dp
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

                // ------------------------------------------------
                // 3. Кнопка ПРАВАЯ (Checklist) -> ВЕС 1f
                // ------------------------------------------------
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { performHaptic(view, false); onShowNotifs() }
                        ) {
                            Icon(
                                imageVector = HeroiconsRectangleStack,
                                contentDescription = "Checklist",
                                tint = currentThemeColor.copy(alpha = 0.6f),
                                // УВЕЛИЧИЛИ РАЗМЕР: было 24.dp -> стало 32.dp
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Точка уведомлений
                        if (viewModel.updates.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(start = 22.dp, bottom = 22.dp) // Чуть сдвинул точку для большой иконки
                                    .size(8.dp)
                                    .background(BrandRed, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // 2. Кнопка поиска
        SimpGlassCard(
            hazeState = hazeState,
            shape = CircleShape,
            modifier = Modifier
                .size(64.dp)
                .clickable { performHaptic(view, false); onSearchClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if(isSearchActive) BrandBlue else currentThemeColor,
                // УВЕЛИЧИЛИ РАЗМЕР: было 28.dp -> стало 32.dp
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

// ==========================================
// ВСТРОЕННЫЕ ИКОНКИ (ИЗ ТВОИХ ФАЙЛОВ)
// ==========================================

// 1. Plus(1).kt (Жирный плюс)
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

// 2. Файл (2).kt -> HeroiconsSquaresPlus (Квадратики с плюсом)
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

// 3. Файл (1).kt -> HeroiconsRectangleStack (Стопка)
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
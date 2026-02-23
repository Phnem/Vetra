package com.example.myapplication.ui.shared

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch

/**
 * iOS-like rubber-band overscroll effect.
 * Provides a bouncy overscroll behavior similar to iOS when scrolling beyond boundaries.
 */
fun Modifier.customOverscroll(
    listState: LazyListState,
    onOverscrollChange: (Float) -> Unit
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val overscrollAnimatable = remember { Animatable(0f) }
    
    LaunchedEffect(overscrollAnimatable.value) {
        onOverscrollChange(overscrollAnimatable.value)
    }
    
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                // Check if we're at the top or bottom
                val isAtTop = listState.firstVisibleItemIndex == 0 && 
                              listState.firstVisibleItemScrollOffset == 0
                val isAtBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == 
                                listState.layoutInfo.totalItemsCount - 1
                
                if (available.y > 0 && isAtTop) {
                    // Overscrolling at top - apply resistance
                    val resistance = 0.3f
                    val newAmount = (overscrollAnimatable.value + available.y * resistance).coerceAtMost(100f)
                    scope.launch {
                        overscrollAnimatable.snapTo(newAmount)
                        // Spring back
                        overscrollAnimatable.animateTo(
                            0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                } else if (available.y < 0 && isAtBottom) {
                    // Overscrolling at bottom - apply resistance
                    val resistance = 0.3f
                    val newAmount = (overscrollAnimatable.value + available.y * resistance).coerceAtLeast(-100f)
                    scope.launch {
                        overscrollAnimatable.snapTo(newAmount)
                        // Spring back
                        overscrollAnimatable.animateTo(
                            0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                } else {
                    // Reset overscroll when scrolling normally
                    if (overscrollAnimatable.value != 0f) {
                        scope.launch {
                            overscrollAnimatable.animateTo(
                                0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                }
                
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    
    this.nestedScroll(connection)
}

/**
 * Современный модификатор для iOS-like нажатий.
 * @param scaleDown Масштаб элемента при удержании (по умолчанию 0.95f)
 * @param enabled Активна ли кнопка
 * @param onClick Действие при клике
 */
fun Modifier.fluidClickable(
    scaleDown: Float = 0.95f,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fluidClickScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick
        )
}

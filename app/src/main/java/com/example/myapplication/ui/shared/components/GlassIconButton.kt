package com.example.myapplication.ui.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.safeHaze
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeState

private val glassButtonHazeStyle = HazeStyle(tints = emptyList(), noiseFactor = 0f, blurRadius = 28.dp)

/**
 * Универсальная Glass-кнопка с иконкой.
 * Использует safeHaze при наличии hazeState; иначе — полупрозрачный фон.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    hazeState: HazeState? = null,
    backgroundColor: Color = Color.White.copy(alpha = if (enabled) 0.15f else 0.05f),
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    iconOffsetX: Dp = 0.dp,
    iconOffsetY: Dp = 0.dp
) {
    val borderAlpha = if (enabled) 0.3f else 0.1f
    val borderColor = Color.White.copy(alpha = borderAlpha)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (hazeState != null) Modifier.safeHaze(hazeState, glassButtonHazeStyle)
                else Modifier
            )
            .background(backgroundColor)
            .border(0.5.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple()
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier
                .size(iconSize)
                .offset(iconOffsetX, iconOffsetY)
        )
    }
}

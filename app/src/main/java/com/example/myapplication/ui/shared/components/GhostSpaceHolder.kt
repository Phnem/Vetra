package com.example.myapplication.ui.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

/**
 * Удерживает пространство при скрытии контента (например, при Shared Transition).
 * Предотвращает "схлопывание" списка, когда AnimatedVisibility удаляет узел.
 */
@Composable
fun GhostSpaceHolder(
    isSpaceLocked: Boolean,
    content: @Composable () -> Unit
) {
    var savedSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier.onSizeChanged { size ->
            if (size.width > 0 && size.height > 0) {
                savedSize = size
            }
        }
    ) {
        val holdModifier = if (isSpaceLocked && savedSize != IntSize.Zero) {
            Modifier.size(
                width = with(density) { savedSize.width.toDp() },
                height = with(density) { savedSize.height.toDp() }
            )
        } else Modifier

        Box(modifier = holdModifier) {
            content()
        }
    }
}

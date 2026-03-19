package com.example.myapplication.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.shared.theme.SnProFamily

sealed interface UpdateTileState {
    data object Idle : UpdateTileState
    data object Checking : UpdateTileState
    data class UpdateAvailable(val newVersion: String) : UpdateTileState
    data class UpToDate(val currentVersion: String) : UpdateTileState
    data object Error : UpdateTileState
}

/**
 * Контент плитки обновлений (анимированная часть).
 * Оборачивается в BaseTile для полного размера, иконки и блика.
 */
@Composable
fun UpdateTile(
    state: UpdateTileState,
    checkButtonText: String,
    checkingText: String,
    availableText: String,
    upToDateText: String,
    versionLabel: String,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
        },
        modifier = modifier.fillMaxWidth(),
        label = "UpdateTileTransition"
    ) { targetState ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when (targetState) {
                is UpdateTileState.Idle -> UpdateTileRow(
                    text = checkButtonText,
                    icon = Icons.Default.Refresh
                )
                is UpdateTileState.Checking -> UpdateTileRow(
                    text = checkingText,
                    isChecking = true
                )
                is UpdateTileState.UpdateAvailable -> UpdateTileRow(
                    text = availableText,
                    subtitle = "$versionLabel ${targetState.newVersion}",
                    icon = Icons.Default.Download,
                    tint = MaterialTheme.colorScheme.primary
                )
                is UpdateTileState.UpToDate -> UpdateTileRow(
                    text = upToDateText,
                    subtitle = targetState.currentVersion,
                    icon = Icons.Default.Check,
                    tint = MaterialTheme.colorScheme.primary
                )
                is UpdateTileState.Error -> UpdateTileRow(
                    text = checkButtonText,
                    icon = Icons.Default.Refresh
                )
            }
        }
    }
}

@Composable
private fun UpdateTileRow(
    text: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isChecking: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                fontFamily = SnProFamily,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = SnProFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

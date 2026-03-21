package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.HeroiconsRectangleStack
import com.example.myapplication.data.models.UiStrings
import com.example.myapplication.ui.shared.theme.BrandBlue
import com.example.myapplication.ui.shared.theme.BrandRed

/**
 * Stateless sort + notifications actions for workspace header and glass dock.
 *
 * @param dockButtonBackground When non-[Color.Transparent] (and useDockSizing), applies circular pill behind icons like [GlassActionDock].
 */
@Composable
fun WorkspaceSortNotificationActions(
    strings: UiStrings,
    filterSelectedTags: List<String>,
    updatesCount: Int,
    onOpenSort: () -> Unit,
    onOpenNotifications: () -> Unit,
    dockButtonBackground: Color,
    useDockSizing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (useDockSizing) 12.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val sortModifier = if (useDockSizing) {
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(dockButtonBackground)
        } else {
            Modifier
        }
        IconButton(
            onClick = onOpenSort,
            modifier = sortModifier
        ) {
            val icon = if (filterSelectedTags.isNotEmpty()) Icons.Outlined.FilterList else Icons.AutoMirrored.Filled.Sort
            val tint = if (filterSelectedTags.isNotEmpty()) BrandBlue else MaterialTheme.colorScheme.onSurface
            Icon(icon, contentDescription = strings.cdSort, tint = tint)
        }
        Box {
            val notifModifier = if (useDockSizing) {
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(dockButtonBackground)
            } else {
                Modifier
            }
            IconButton(
                onClick = onOpenNotifications,
                modifier = notifModifier
            ) {
                Icon(
                    imageVector = HeroiconsRectangleStack,
                    contentDescription = strings.cdNotifications,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (updatesCount > 0) {
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

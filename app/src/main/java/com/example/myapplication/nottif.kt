package com.example.myapplication

import com.example.myapplication.data.models.*
import com.example.myapplication.ui.shared.theme.BrandBlue
import com.example.myapplication.ui.shared.theme.BrandRed
import com.example.myapplication.utils.performHaptic
import android.text.format.DateFormat
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

// ==========================================
// NOTIFICATION & SYNC OVERLAY
// ==========================================

// Цвета
private val CustomDarkSurface = Color(0xFF1F222B)
private val CustomDarkCard = Color(0xFF262A35)
private val CustomDarkBorder = Color.White.copy(alpha = 0.08f)

private val IconTimeColor = Color(0xFF5E5CE6)
private val IconStatusColor = Color(0xFF2E7D32)
private val IconAccountColor = Color(0xFF3E82F7)
private val IconUpdateColor = Color(0xFFFF9F0A)

// ... (imports)

@Composable
fun NotificationSyncOverlay(
    visibleState: MutableTransitionState<Boolean>,
    strings: UiStrings,
    updates: List<AnimeUpdate>,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onAcceptUpdate: (AnimeUpdate) -> Unit = {},
    onDismissUpdate: (AnimeUpdate) -> Unit = {}
) {
    val syncState by DropboxSyncManager.syncState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Вместо .luminance() < 0.5f
    val isDark = MaterialTheme.colorScheme.background.toArgb() == Color(0xFF111318).toArgb()

    // Цвета теперь будут переключаться корректно
    val panelBgColor = if (isDark) CustomDarkSurface else MaterialTheme.colorScheme.surface
    val itemCardColor = if (isDark) CustomDarkCard else MaterialTheme.colorScheme.surfaceVariant
    val itemBorderColor = if (isDark) CustomDarkBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    BackHandler { onDismiss() }

    // ... (далее код без изменений)

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. SCRIM (Затемнение фона)
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

        // 2. ПАНЕЛЬ МЕНЮ
        AnimatedVisibility(
            visibleState = visibleState,
            enter = scaleIn(
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
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
                    .width(340.dp)
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
                        // Заголовок
                        Text(
                            text = strings.nottifHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, top = 4.dp)
                        )

                        // Карточка 1: Время
                        val lastSyncTime = remember(syncState) {
                            val ts = context.getSharedPreferences(
                                "dropbox_prefs",
                                android.content.Context.MODE_PRIVATE
                            )
                                .getLong("last_sync_time", 0L)
                            if (ts == 0L) "--:--" else DateFormat.format("HH:mm", ts).toString()
                        }

                        PillCard(
                            icon = Icons.Default.Schedule,
                            title = strings.nottifLastSyncTitle,
                            subtitle = strings.nottifLastSyncSub,
                            cardColor = itemCardColor,
                            borderColor = itemBorderColor,
                            iconBgColor = IconTimeColor.copy(alpha = 0.15f),
                            iconTintColor = IconTimeColor,
                            contentEnd = {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = lastSyncTime,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Карточка 2: Статус
                        PillCard(
                            icon = Icons.Default.CloudSync,
                            title = strings.nottifStatusTitle,
                            subtitle = strings.nottifStatusSub,
                            cardColor = itemCardColor,
                            borderColor = itemBorderColor,
                            iconBgColor = IconStatusColor.copy(alpha = 0.15f),
                            iconTintColor = IconStatusColor,
                            contentEnd = { StatusIcon(syncState) }
                        )

                        Spacer(Modifier.height(12.dp))

                        // Карточка 3: Аккаунт
                        var showLogoutDialog by remember { mutableStateOf(false) }

                        PillCard(
                            icon = Icons.Default.Person,
                            title = strings.nottifAccountTitle,
                            subtitle = null,
                            cardColor = itemCardColor,
                            borderColor = itemBorderColor,
                            iconBgColor = IconAccountColor.copy(alpha = 0.15f),
                            iconTintColor = IconAccountColor,
                            contentEnd = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { DropboxSyncManager.scheduleAutoSync() },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                IconAccountColor.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Sync,
                                            null,
                                            tint = IconAccountColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { showLogoutDialog = true },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                Color.Red.copy(alpha = 0.15f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Logout,
                                            null,
                                            tint = Color.Red,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        )

                        // Уведомления
                        if (updates.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 20.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                text = strings.updatesTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                            )

                            updates.forEach { update ->
                                Spacer(Modifier.height(8.dp))
                                PillCard(
                                    icon = Icons.Outlined.Movie,
                                    title = update.title,
                                    subtitle = "+${update.newEpisodes - update.currentEpisodes} eps",
                                    cardColor = itemCardColor,
                                    borderColor = itemBorderColor,
                                    iconBgColor = IconUpdateColor.copy(alpha = 0.15f),
                                    iconTintColor = IconUpdateColor,
                                    onClick = { onAcceptUpdate(update) },
                                    contentEnd = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = { onDismissUpdate(update) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onAcceptUpdate(update) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    tint = IconAccountColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        // Диалог выхода
                        if (showLogoutDialog) {
                            AlertDialog(
                                onDismissRequest = { showLogoutDialog = false },
                                containerColor = panelBgColor,
                                titleContentColor = if (isDark) Color.White else Color.Black,
                                textContentColor = if (isDark) Color.Gray else Color.Black,
                                title = { Text(strings.nottifLogoutConfirmTitle) },
                                text = { Text(strings.nottifLogoutConfirmBody) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showLogoutDialog = false
                                            onLogout()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) { Text(strings.deleteConfirm) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showLogoutDialog = false }) { Text(strings.cancel) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// КОМПОНЕНТ "ПИЛЮЛЯ"
// ==========================================
// ==========================================
// КОМПОНЕНТ "ПИЛЮЛЯ"
// ==========================================

@Composable
private fun PillCard(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    cardColor: Color,
    borderColor: Color,
    iconBgColor: Color,
    iconTintColor: Color,
    modifier: Modifier = Modifier,
    contentEnd: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val pillShape = RoundedCornerShape(100)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(pillShape)
            .background(cardColor)
            .border(1.dp, borderColor, pillShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

        Spacer(Modifier.width(8.dp))
        contentEnd()
    }
}

// ==========================================
// СТАТУС (XML иконки + Native анимация)
// ==========================================
@Composable
private fun StatusIcon(state: SyncState) {
    when (state) {
        SyncState.DONE, SyncState.IDLE -> {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.gal),
                    contentDescription = "Synced",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        SyncState.SYNCING -> {
            val infiniteTransition = rememberInfiniteTransition(label = "spin")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing)),
                label = "spin"
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(IconAccountColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Syncing",
                    tint = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = angle }
                )
            }
        }
        SyncState.AUTH_REQUIRED, SyncState.ERROR, SyncState.CONFLICT -> {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.krestic),
                    contentDescription = "Error",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
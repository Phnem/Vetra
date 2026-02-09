package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val TileColorDark = Color(0xFF343845)
val TileBorderDark = Color.White.copy(alpha = 0.12f)
val CloudAccent = Color(0xFF0066CC)
val CloudNegative = Color(0xFFCC3300)

private object CloudStrings {
    data class Content(
        val accountLabel: String,
        val statusLabel: String,
        val syncLabel: String,
        val netModeWifiMob: String,
        val netModeWifi: String,
        val netModeMob: String,
        val notifTitle: String,
        val notifFiles: String,
        val notifSize: String,
        val notifConsistent: String
    )

    val RU = Content(
        accountLabel = "Аккаунт",
        statusLabel = "Статус",
        syncLabel = "Синх",
        netModeWifiMob = "Wi-Fi+Моб",
        netModeWifi = "Wi-Fi",
        netModeMob = "Моб",
        notifTitle = "Статус Облака",
        notifFiles = "Файлов",
        notifSize = "Размер",
        notifConsistent = "Синхронизировано"
    )

    val EN = Content(
        accountLabel = "Account",
        statusLabel = "Status",
        syncLabel = "Sync",
        netModeWifiMob = "Wi-Fi+Mob",
        netModeWifi = "Wi-Fi Only",
        netModeMob = "Mobile Only",
        notifTitle = "Cloud Status",
        notifFiles = "Files",
        notifSize = "Size",
        notifConsistent = "Synced"
    )

    fun get(lang: AppLanguage): Content = if (lang == AppLanguage.RU) RU else EN
}

@Composable
fun CloudSettingsSection(
    viewModel: AnimeViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = CloudStrings.get(viewModel.currentLanguage)
    val s = viewModel.strings

    val syncMode by DropboxSyncManager.syncMode.collectAsState()
    val networkMode by DropboxSyncManager.networkMode.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }
    var storageStats by remember { mutableStateOf<DropboxSyncManager.StorageStats?>(null) }

    LaunchedEffect(Unit) {
        storageStats = DropboxSyncManager.calculateStorageStats()
    }

    val contentColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CloudActionCard(
                modifier = Modifier.weight(1f),
                label = strings.accountLabel,
                icon = Icons.AutoMirrored.Filled.Logout,
                accentColor = Color(0xFFFF453A),
                isDestructive = true,
                onClick = { onLogout() }
            )

            CloudActionCard(
                modifier = Modifier.weight(1f),
                label = strings.statusLabel,
                icon = Icons.Default.Info,
                accentColor = Color(0xFF2E7D32),
                onClick = {
                    scope.launch {
                        try {
                            val stats = DropboxSyncManager.calculateStorageStats()
                            showCloudStatusNotification(context, strings, stats)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error getting status", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SegmentedToggle(
            label1 = s.syncAuto,
            icon1 = Icons.Default.AutoMode,
            isSelected1 = syncMode == SyncMode.AUTO,
            label2 = s.syncManual,
            icon2 = Icons.Default.TouchApp,
            isSelected2 = syncMode == SyncMode.MANUAL,
            accentColor = BrandBlue,
            onSelect1 = { DropboxSyncManager.setSyncMode(SyncMode.AUTO) },
            onSelect2 = { DropboxSyncManager.setSyncMode(SyncMode.MANUAL) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CloudActionCard(
                modifier = Modifier.weight(1f),
                label = strings.syncLabel,
                icon = Icons.Default.Sync,
                accentColor = BrandBlue,
                isLoading = isSyncing,
                onClick = {
                    if (!isSyncing) {
                        isSyncing = true
                        scope.launch {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    DropboxSyncManager.syncNow()
                                } else {
                                    DropboxSyncManager.syncNow()
                                }
                            } else {
                                DropboxSyncManager.syncNow()
                            }
                            delay(1000)
                            isSyncing = false
                        }
                    }
                }
            )

            val (modeLabel, modeIcon) = when(networkMode) {
                NetworkMode.WIFI_AND_MOBILE -> strings.netModeWifiMob to Icons.Rounded.WifiTethering
                NetworkMode.WIFI_ONLY -> strings.netModeWifi to Icons.Rounded.Wifi
                NetworkMode.MOBILE_ONLY -> strings.netModeMob to Icons.Rounded.SignalCellularAlt
            }

            CloudActionCard(
                modifier = Modifier.weight(1f),
                label = "",
                icon = modeIcon,
                accentColor = Color(0xFFFF9F0A),
                customContent = {
                    SlotTextAnimation(text = modeLabel, color = contentColor)
                },
                onClick = {
                    val nextMode = when(networkMode) {
                        NetworkMode.WIFI_AND_MOBILE -> NetworkMode.WIFI_ONLY
                        NetworkMode.WIFI_ONLY -> NetworkMode.MOBILE_ONLY
                        NetworkMode.MOBILE_ONLY -> NetworkMode.WIFI_AND_MOBILE
                    }
                    DropboxSyncManager.setNetworkMode(nextMode)
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${s.lastSync}: ${storageStats?.lastSyncTime ?: s.never}",
            fontSize = 12.sp,
            color = contentColor.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}


@Composable
fun SegmentedToggle(
    label1: String,
    icon1: ImageVector,
    isSelected1: Boolean,
    label2: String,
    icon2: ImageVector,
    isSelected2: Boolean,
    accentColor: Color,
    onSelect1: () -> Unit,
    onSelect2: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CloudActionCard(
            modifier = Modifier.weight(1f),
            label = label1,
            icon = icon1,
            accentColor = if (isSelected1) accentColor else Color.Gray,
            isSelected = isSelected1,
            onClick = onSelect1
        )

        CloudActionCard(
            modifier = Modifier.weight(1f),
            label = label2,
            icon = icon2,
            accentColor = if (isSelected2) accentColor else Color.Gray,
            isSelected = isSelected2,
            onClick = onSelect2
        )
    }
}

@Composable
fun CloudActionCard(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean = true,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    customContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val shape = CircleShape
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) TileColorDark else MaterialTheme.colorScheme.surfaceVariant

    val borderCol = if (isSelected) {
        accentColor.copy(alpha = 0.2f)
    } else {
        if (isDark) TileBorderDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }
    val borderWidth = 1.dp

    val iconBg = if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent

    val iconTint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(cardBg)
            .border(borderWidth, borderCol, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (customContent != null) {
                    customContent()
                } else {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SlotTextAnimation(text: String, color: Color) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn())
                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
        },
        label = "SlotAnimation"
    ) { targetText ->
        Text(
            text = targetText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = color
        )
    }
}

@Composable
fun CloudInfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    iconTint: Color = CloudAccent
) {
    val bgColor = if (isSystemInDarkTheme()) TileColorDark else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}

@SuppressLint("MissingPermission")
private fun showCloudStatusNotification(
    context: Context,
    strings: CloudStrings.Content,
    stats: DropboxSyncManager.StorageStats
) {
    val formattedSize = Formatter.formatFileSize(context, stats.totalSize)
    val statusText = "${strings.notifConsistent}: ${if(stats.isConsistent) "YES" else "Checking..."}\n" +
            "${strings.notifFiles}: ${stats.fileCount} | ${strings.notifSize}: $formattedSize\n" +
            "Last: ${stats.lastSyncTime}"

    Toast.makeText(context, "$statusText", Toast.LENGTH_LONG).show()

    val channelId = "cloud_status_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Cloud Status", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Sync stats"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_upload_done)
        .setContentTitle(strings.notifTitle)
        .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        NotificationManagerCompat.from(context).notify(777, builder.build())
    }
}
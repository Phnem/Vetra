package com.example.myapplication

import android.text.format.DateFormat
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.models.UiStrings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.shared.theme.DarkBackground
import com.example.myapplication.ui.settings.tileGlow
import com.example.myapplication.utils.performHaptic
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

// —— Тёмная тема: фон панели = DarkBackground; плитки как в настройках (tileGlow) ——
private val NottifRimDark = Color.White.copy(alpha = 0.09f)
private val LabelMutedDark = Color(0xFF94A3B8)

/** Иконки панели: фиксированные оттенки по запросу */
private val NottifIconSyncBlue = Color(0xFF38BDF8)
private val NottifIconSignalGreen = Color(0xFF34C759)
private val NottifIconAccountYellow = Color(0xFFFFC400)

/** Текст/иконки на яркой кнопке синхронизации */
private val OnSyncBlueButton = Color.White

/** Сплошная «выход» как на референсе (коралловый) */
private val LogoutIconTint = Color(0xFFFF5A4D)

/** Пилюля метрик: без рамки AssistChip, полностью скруглённый контейнер. */
@Composable
private fun SyncMetricPill(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.semantics { this.contentDescription = contentDescription }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun NotificationSyncOverlay(
    syncManager: DropboxSyncManager,
    visibleState: MutableTransitionState<Boolean>,
    strings: UiStrings,
    syncReport: SyncReport,
    updates: List<AnimeUpdate>,
    isCheckingUpdates: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onCheckUpdates: () -> Unit,
    onAcceptUpdate: (AnimeUpdate) -> Unit = {},
    onDismissUpdate: (AnimeUpdate) -> Unit = {}
) {
    val syncState by syncManager.syncState.collectAsStateWithLifecycle()
    val syncMode by syncManager.syncMode.collectAsStateWithLifecycle()
    val hasToken by syncManager.hasTokenFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val isDark = isAppInDarkTheme()
    val panelBg = if (isDark) DarkBackground else MaterialTheme.colorScheme.surface
    val cardBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val darkTileBase = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val rim = if (isDark) NottifRimDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val onCard = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val muted = if (isDark) LabelMutedDark else MaterialTheme.colorScheme.onSurfaceVariant

    val lastSyncTs = remember(syncState, visibleState.currentState, visibleState.targetState) {
        context.getSharedPreferences("dropbox_prefs", android.content.Context.MODE_PRIVATE)
            .getLong("last_sync_time", 0L)
    }
    val timeStr = remember(lastSyncTs) {
        if (lastSyncTs == 0L) "--:--"
        else DateFormat.getTimeFormat(context).format(lastSyncTs)
    }
    val datePart = remember(lastSyncTs, strings) { formatSyncDateLine(lastSyncTs, strings) }
    val statusWord = remember(syncState, strings) { syncStatusWord(strings, syncState) }
    val detailLine = "$datePart • $statusWord"

    BackHandler { onDismiss() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
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
                    .padding(top = 88.dp, end = 12.dp)
                    .width(340.dp)
                    .wrapContentHeight()
            ) {
                // Небольшой внешний отступ, чтобы скругление Card не обрезало круглую кнопку закрытия
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = panelBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = BorderStroke(1.dp, rim),
                    modifier = Modifier
                        .padding(top = 10.dp, end = 6.dp)
                        .clickable(enabled = false) {}
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = 18.dp, top = 30.dp, end = 22.dp, bottom = 18.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // —— Header —— //
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.nottifPanelTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = onCard,
                                    fontSize = 22.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = strings.nottifPanelSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = muted
                                )
                            }
                            IconButton(
                                onClick = {
                                    performHaptic(view, "light")
                                    onDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.nottifCloseCd,
                                    tint = muted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // —— Main sync card (колонка: бейдж → инфо → кнопки внизу, без наложений) —— //
                        val badgeText = when (syncMode) {
                            SyncMode.AUTO -> strings.nottifBadgeAutomated
                            SyncMode.MANUAL -> strings.nottifBadgeManual
                        }
                        val syncingCloud = syncState == SyncState.SYNCING
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clip(RoundedCornerShape(22.dp))
                                .then(
                                    if (isDark) {
                                        Modifier
                                            .background(darkTileBase)
                                            .tileGlow(NottifIconSyncBlue)
                                    } else {
                                        Modifier.background(cardBg)
                                    }
                                )
                                .border(1.dp, rim, RoundedCornerShape(22.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.nottifLastSyncTitle.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = muted,
                                    letterSpacing = 0.9.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NottifIconSyncBlue,
                                    letterSpacing = 0.8.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        enabled = !syncingCloud,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        performHaptic(view, "light")
                                        scope.launch { syncManager.syncNow() }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = NottifIconSyncBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .wrapContentHeight()
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = onCard,
                                        fontSize = 22.sp,
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = detailLine,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = muted,
                                        lineHeight = 16.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val pillBg = NottifIconSyncBlue.copy(alpha = 0.28f)
                                val pillContent = OnSyncBlueButton
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SyncMetricPill(
                                        icon = Icons.Filled.CheckCircle,
                                        // totalProcessed вместо syncedCount — пользователь видит общее число учтённых операций
                                        text = "${strings.nottifSyncChipSynced}: ${syncReport.totalProcessed}",
                                        containerColor = pillBg,
                                        contentColor = pillContent,
                                        contentDescription = "${strings.nottifSyncChipSynced}: ${syncReport.totalProcessed}"
                                    )
                                    SyncMetricPill(
                                        icon = Icons.Filled.Error,
                                        text = "${strings.nottifSyncChipErrors}: ${syncReport.errorCount}",
                                        containerColor = pillBg,
                                        contentColor = pillContent,
                                        contentDescription = "${strings.nottifSyncChipErrors}: ${syncReport.errorCount}"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        performHaptic(view, "light")
                                        onCheckUpdates()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.28f))
                                ) {
                                    val infiniteTransition = rememberInfiniteTransition(label = "chk")
                                    val angle by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = 360f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(900, easing = LinearEasing),
                                            repeatMode = RepeatMode.Restart
                                        ),
                                        label = "spinChk"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = strings.nottifCheckUpdatesCd,
                                        tint = NottifIconSyncBlue,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                if (isCheckingUpdates) rotationZ = angle
                                            }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // —— Status | Account (одинаковая высота) —— //
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            NottifMiniCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                isDark = isDark,
                                glowAccent = NottifIconSignalGreen,
                                cardBg = cardBg,
                                rim = rim,
                                onCard = onCard,
                                muted = muted,
                                accentColor = NottifIconSignalGreen,
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SignalCellularAlt,
                                            contentDescription = null,
                                            tint = NottifIconSignalGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = strings.nottifStatusTitle.uppercase(Locale.getDefault()),
                                value = if (hasToken && syncState != SyncState.AUTH_REQUIRED) {
                                    strings.nottifStatusConnected
                                } else {
                                    strings.nottifStatusDisconnected
                                },
                                hint = "",
                                hintDot = false
                            )
                            NottifMiniCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                isDark = isDark,
                                glowAccent = NottifIconAccountYellow,
                                cardBg = cardBg,
                                rim = rim,
                                onCard = onCard,
                                muted = muted,
                                accentColor = NottifIconAccountYellow,
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = NottifIconAccountYellow,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                label = strings.nottifSectionAccount.uppercase(Locale.getDefault()),
                                value = strings.nottifAccountTitle,
                                hint = if (hasToken) strings.nottifAccountSignedIn else strings.nottifAccountGuest,
                                footerAction = {
                                    IconButton(
                                        onClick = {
                                            performHaptic(view, "light")
                                            showLogoutDialog = true
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                            contentDescription = strings.nottifLogoutConfirmTitle,
                                            tint = LogoutIconTint,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            )
                        }

                        // —— Episode updates —— //
                        if (updates.isNotEmpty()) {
                            Spacer(Modifier.height(18.dp))
                            HorizontalDivider(color = rim, thickness = 1.dp)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = strings.updatesTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = onCard
                            )
                            Spacer(Modifier.height(10.dp))
                            updates.forEach { update ->
                                val delta = max(0, update.newEpisodes - update.currentEpisodes)
                                val epLine = String.format(Locale.getDefault(), strings.nottifNewEpisodesFormat, delta)
                                NottifUpdateRow(
                                    isDark = isDark,
                                    glowAccent = NottifIconSyncBlue,
                                    cardBg = cardBg,
                                    darkTileBase = darkTileBase,
                                    rim = rim,
                                    onCard = onCard,
                                    accentColor = NottifIconSyncBlue,
                                    onAccentColor = OnSyncBlueButton,
                                    title = update.title,
                                    subtitle = epLine,
                                    onAccept = {
                                        performHaptic(view, "success")
                                        onAcceptUpdate(update)
                                    },
                                    onDismissRow = {
                                        performHaptic(view, "light")
                                        onDismissUpdate(update)
                                    }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = panelBg,
                titleContentColor = onCard,
                textContentColor = muted,
                title = { Text(strings.nottifLogoutConfirmTitle) },
                text = { Text(strings.nottifLogoutConfirmBody) },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) { Text(strings.deleteConfirm) }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) { Text(strings.cancel) }
                }
            )
        }
    }
}

@Composable
private fun NottifMiniCard(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    glowAccent: Color,
    cardBg: Color,
    rim: Color,
    onCard: Color,
    muted: Color,
    accentColor: Color,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    hint: String,
    hintDot: Boolean = false,
    footerAction: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isDark) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .tileGlow(glowAccent)
                } else {
                    Modifier.background(cardBg)
                }
            )
            .border(1.dp, rim, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = muted,
                    letterSpacing = 0.8.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = onCard,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp
        )
        when {
            hint.isNotEmpty() && footerAction != null -> {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hintDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    footerAction()
                }
            }
            hint.isNotEmpty() -> {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hintDot) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = muted
                    )
                }
            }
            footerAction != null -> {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    footerAction()
                }
            }
        }
    }
}

@Composable
private fun NottifUpdateRow(
    isDark: Boolean,
    glowAccent: Color,
    cardBg: Color,
    darkTileBase: Color,
    rim: Color,
    onCard: Color,
    accentColor: Color,
    onAccentColor: Color,
    title: String,
    subtitle: String,
    onAccept: () -> Unit,
    onDismissRow: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isDark) {
                    Modifier
                        .background(darkTileBase)
                        .tileGlow(glowAccent)
                } else {
                    Modifier.background(cardBg)
                }
            )
            .border(1.dp, rim, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = onCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAccept() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = onAccentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismissRow() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSyncDateLine(ts: Long, strings: UiStrings): String {
    if (ts == 0L) return strings.never
    val cal = Calendar.getInstance()
    val nowDay = cal.get(Calendar.DAY_OF_YEAR)
    val nowYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = ts
    val d = cal.get(Calendar.DAY_OF_YEAR)
    val y = cal.get(Calendar.YEAR)
    return if (d == nowDay && y == nowYear) {
        strings.nottifDateToday
    } else {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}

private fun syncStatusWord(strings: UiStrings, state: SyncState): String = when (state) {
    SyncState.DONE, SyncState.IDLE -> strings.nottifLastSyncSuccess
    SyncState.SYNCING -> strings.nottifLastSyncSyncing
    SyncState.ERROR -> strings.nottifLastSyncError
    SyncState.AUTH_REQUIRED -> strings.nottifLastSyncNeedsLogin
    SyncState.CONFLICT -> strings.nottifLastSyncConflict
}

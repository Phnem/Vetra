package com.example.myapplication.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.data.models.*
import com.example.myapplication.network.AppContentType
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import com.example.myapplication.ui.shared.theme.*
import com.example.myapplication.ui.navigation.navigateToWelcome
import com.example.myapplication.R

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bg = MaterialTheme.colorScheme.background
    val textC = MaterialTheme.colorScheme.onBackground
    val view = LocalView.current
    val context = LocalContext.current

    var expandedLang by remember { mutableStateOf(false) }
    var expandedTheme by remember { mutableStateOf(false) }
    var expandedUpdate by remember { mutableStateOf(false) }
    var expandedContact by remember { mutableStateOf(false) }
    var expandedCloud by remember { mutableStateOf(false) }
    var expandedType by remember { mutableStateOf(false) }

    val strings = getStrings(uiState.language)

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState(key = "settings_container"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    clipInOverlayDuringTransition = OverlayClip(RoundedCornerShape(32.dp))
                )
                .clip(RoundedCornerShape(32.dp))
                .background(bg)
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        performHaptic(view, "light")
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textC,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "settings_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = strings.settingsScreenTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textC
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        val accentTint = EpisodesColor
                        SettingsPill(
                            title = strings.languageCardTitle,
                            icon = Icons.Outlined.Language,
                            iconTint = accentTint,
                            isExpanded = expandedLang,
                            onClick = { performHaptic(view, "light"); expandedLang = !expandedLang }
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp)
                            ) {
                                val colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = accentTint.copy(alpha = 0.12f),
                                    activeContentColor = accentTint,
                                    inactiveContainerColor = Color.Transparent
                                )
                                SegmentedButton(
                                    selected = uiState.language == AppLanguage.EN,
                                    onClick = { viewModel.setLanguage(AppLanguage.EN) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = colors
                                ) { Text(strings.langEn, fontFamily = SnProFamily, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                                SegmentedButton(
                                    selected = uiState.language == AppLanguage.RU,
                                    onClick = { viewModel.setLanguage(AppLanguage.RU) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = colors
                                ) { Text(strings.langRu, fontFamily = SnProFamily, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }

                    item {
                        val accentTint = Color(0xFF00BFA6)
                        SettingsPill(
                            title = strings.themeTitle,
                            icon = Icons.Outlined.Palette,
                            iconTint = accentTint,
                            isExpanded = expandedTheme,
                            onClick = { performHaptic(view, "light"); expandedTheme = !expandedTheme }
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp)
                            ) {
                                val colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = accentTint.copy(alpha = 0.12f),
                                    activeContentColor = accentTint,
                                    inactiveContainerColor = Color.Transparent
                                )
                                val themeOptions = listOf(
                                    AppTheme.LIGHT to strings.themeLight,
                                    AppTheme.DARK to strings.themeDark,
                                    AppTheme.SYSTEM to strings.themeSystem
                                )
                                themeOptions.forEachIndexed { index, (theme, label) ->
                                    SegmentedButton(
                                        selected = uiState.theme == theme,
                                        onClick = {
                                            performHaptic(view, "light")
                                            viewModel.setTheme(theme)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                                        colors = colors
                                    ) { Text(label, fontFamily = SnProFamily, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                }
                            }
                        }
                    }

                    item {
                        val accentTint = Color(0xFFFF9500)
                        SettingsPill(
                            title = strings.contentTypeTitle,
                            icon = Icons.Outlined.Category,
                            iconTint = accentTint,
                            isExpanded = expandedType,
                            onClick = { expandedType = !expandedType }
                        ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp)
                            ) {
                                val colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = accentTint.copy(alpha = 0.12f),
                                    activeContentColor = accentTint,
                                    inactiveContainerColor = Color.Transparent
                                )
                                SegmentedButton(
                                    selected = uiState.contentType == AppContentType.ANIME,
                                    onClick = { viewModel.setContentType(AppContentType.ANIME) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    colors = colors
                                ) { Text(strings.typeAnime, fontFamily = SnProFamily, maxLines = 1, overflow = TextOverflow.Ellipsis) }

                                SegmentedButton(
                                    selected = uiState.contentType == AppContentType.MOVIE,
                                    onClick = { viewModel.setContentType(AppContentType.MOVIE) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    colors = colors
                                ) { Text(strings.typeMovies, fontFamily = SnProFamily, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                    }

                    item {
                        SettingsPill(
                            title = if (uiState.language == AppLanguage.RU) "Облачные настройки" else "Cloud Settings",
                            icon = Icons.Outlined.Cloud,
                            iconTint = BrandBlue,
                            isExpanded = expandedCloud,
                            onClick = { performHaptic(view, "light"); expandedCloud = !expandedCloud }
                        ) {
                            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)) {
                                val lastSyncTimestamp = remember { context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE).getLong("last_sync_time", 0L) }
                                CloudSettingsSection(
                                    strings = CloudStrings(
                                        title = if (uiState.language == AppLanguage.RU) "Синхронизация с облаком" else "Cloud Sync",
                                        syncNow = strings.syncLabel,
                                        lastSync = strings.lastSync,
                                        neverSynced = strings.never,
                                        logout = if (uiState.language == AppLanguage.RU) "Выйти" else "Logout"
                                    ),
                                    lastSyncTime = lastSyncTimestamp,
                                    onSyncClick = { com.example.myapplication.worker.SyncWorker.enqueue(context) },
                                    onLogout = { DropboxSyncManager.logout(); navController.navigateToWelcome() }
                                )
                            }
                        }
                    }

                    item {
                        SettingsPill(
                            title = strings.checkForUpdateTitle,
                            icon = Icons.Outlined.SystemUpdate,
                            iconTint = RateColor4,
                            isExpanded = expandedUpdate,
                            onClick = { performHaptic(view, "light"); expandedUpdate = !expandedUpdate }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 14.dp, top = 4.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "v${uiState.currentVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(bottom = 8.dp))
                                    UpdateStateButton(status = uiState.updateStatus, idleText = strings.checkButtonText, onClick = {
                                        if (uiState.updateStatus == AppUpdateStatus.IDLE || uiState.updateStatus == AppUpdateStatus.ERROR) {
                                            viewModel.checkAppUpdate(context)
                                        } else if (uiState.updateStatus == AppUpdateStatus.UPDATE_AVAILABLE) {
                                            uiState.latestDownloadUrl?.let { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                        }
                                    })
                                }
                            }
                        }
                    }

                    item {
                        SettingsPill(
                            title = strings.contactTitle,
                            icon = Icons.Outlined.Person,
                            iconTint = Color(0xFF3DDC84),
                            isExpanded = expandedContact,
                            onClick = { performHaptic(view, "light"); expandedContact = !expandedContact }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp, top = 4.dp)) {
                                Text(text = strings.contactSubtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).clickable { performHaptic(view, "light"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Phnem/MAList"))) }, contentAlignment = Alignment.Center) {
                                        Image(painter = painterResource(id = R.drawable.gh), contentDescription = "GitHub", modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
                                    }
                                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).clickable { performHaptic(view, "light"); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/H415base"))) }, contentAlignment = Alignment.Center) {
                                        Image(painter = painterResource(id = R.drawable.tg), contentDescription = "Telegram", modifier = Modifier.size(56.dp), contentScale = ContentScale.Fit)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    expandedContent: @Composable () -> Unit = {}
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 100.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "pillShape"
    )
    val shape = RoundedCornerShape(cornerRadius)

    val bg = MaterialTheme.colorScheme.surfaceVariant
    val borderC = iconTint.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderC, shape)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            val arrow by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "arrow"
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) iconTint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.graphicsLayer { rotationZ = arrow }
            )
        }
        if (isExpanded) {
            expandedContent()
        }
    }
}

data class CloudStrings(
    val title: String = "Синхронизация с облаком",
    val syncNow: String = "Синхронизировать сейчас",
    val lastSync: String = "Последняя синхронизация:",
    val neverSynced: String = "Никогда",
    val logout: String = "Выйти"
)

@Composable
fun CloudSettingsSection(
    strings: CloudStrings,
    lastSyncTime: Long,
    onSyncClick: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = strings.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "${strings.lastSync} ${if (lastSyncTime > 0) java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(lastSyncTime)) else strings.neverSynced}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSyncClick, modifier = Modifier.weight(1f)) {
                Text(strings.syncNow, fontFamily = SnProFamily)
            }
            OutlinedButton(onClick = onLogout) {
                Text(strings.logout, fontFamily = SnProFamily)
            }
        }
    }
}

@Composable
fun UpdateStateButton(
    status: AppUpdateStatus,
    idleText: String,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.secondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
            AppUpdateStatus.NO_UPDATE -> RateColor4
            AppUpdateStatus.UPDATE_AVAILABLE -> BrandBlue
            AppUpdateStatus.ERROR -> BrandRed
        }, label = "btnBg"
    )
    val contentColor by animateColorAsState(
        targetValue = when (status) {
            AppUpdateStatus.IDLE -> MaterialTheme.colorScheme.onSecondary
            AppUpdateStatus.LOADING -> MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)
            else -> Color.White
        }, label = "btnContent"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(if (status == AppUpdateStatus.IDLE) 1f else 0.6f)
            .height(50.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(enabled = status != AppUpdateStatus.LOADING) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            AppUpdateStatus.IDLE -> Text(idleText, fontWeight = FontWeight.Bold, color = contentColor, fontSize = 16.sp, fontFamily = SnProFamily)
            AppUpdateStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.dp)
            AppUpdateStatus.NO_UPDATE -> Icon(Icons.Default.Check, "Up to date", tint = contentColor, modifier = Modifier.size(28.dp))
            AppUpdateStatus.UPDATE_AVAILABLE -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SystemUpdateAlt, "Update", tint = contentColor, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download", fontWeight = FontWeight.Bold, color = contentColor, fontFamily = SnProFamily)
            }
            AppUpdateStatus.ERROR -> Icon(Icons.Default.Close, "Error", tint = contentColor, modifier = Modifier.size(28.dp))
        }
    }
}

package com.example.myapplication.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.R
import com.example.myapplication.SyncState
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.utils.getStrings
import com.example.myapplication.utils.performHaptic
import com.example.myapplication.ui.shared.theme.SnProFamily
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CloudSettingsSheet(
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    sharedModifier: Modifier = Modifier
) {
    val dropboxSyncManager: DropboxSyncManager = koinInject()
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = koinViewModel()
    val uiState by settingsVm.uiState.collectAsStateWithLifecycle()
    val strings = getStrings(uiState.language)
    val scope = rememberCoroutineScope()
    val lastSyncTimestamp = remember {
        context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE).getLong("last_sync_time", 0L)
    }
    val syncState by dropboxSyncManager.syncState.collectAsStateWithLifecycle()

    Card(
        modifier = sharedModifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = if (uiState.language == AppLanguage.RU) "Облачные настройки" else "Cloud Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = SnProFamily,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                    CloudSettingsSection(
                        strings = CloudStrings(
                            title = if (uiState.language == AppLanguage.RU) "Синхронизация с облаком" else "Cloud Sync",
                            syncNow = strings.syncLabel,
                            lastSync = strings.lastSync,
                            neverSynced = strings.never,
                            logout = if (uiState.language == AppLanguage.RU) "Выйти" else "Logout"
                        ),
                        lastSyncTime = lastSyncTimestamp,
                        isSyncing = syncState == SyncState.SYNCING,
                        onSyncClick = { scope.launch { dropboxSyncManager.syncNow() } },
                        onLogout = onLogout
                    )
            }
        }
    }
}

@Composable
fun ContactSheet(
    onDismiss: () -> Unit,
    sharedModifier: Modifier = Modifier
) {
    val settingsVm: SettingsViewModel = koinViewModel()
    val uiState by settingsVm.uiState.collectAsStateWithLifecycle()
    val strings = getStrings(uiState.language)
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = sharedModifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.75f))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = strings.contactTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                fontFamily = SnProFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = strings.contactSupportSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = SnProFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContactActionCard(
                iconId = R.drawable.ic_github,
                title = "GitHub",
                onClick = {
                    performHaptic(view, "light")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Phnem/Vetra")))
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
            ContactActionCard(
                iconId = R.drawable.tg,
                title = "Telegram",
                onClick = {
                    performHaptic(view, "light")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/H415base")))
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactActionCard(
    @DrawableRes iconId: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.aspectRatio(1.1f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = title,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                fontFamily = SnProFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

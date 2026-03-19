package com.example.myapplication.ui.settings

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.SyncState
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.ui.navigation.navigateToWelcome
import com.example.myapplication.ui.shared.theme.SnProFamily
import com.example.myapplication.utils.getStrings
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CloudSettingsScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope?
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

    with(sharedTransitionScope) {
        val baseModifier = Modifier
            .fillMaxSize()
            .then(
                if (animatedVisibilityScope != null) {
                    Modifier.sharedElement(
                        rememberSharedContentState(key = "tile_cloud"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.background)

        Column(
            modifier = baseModifier
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
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
                    onLogout = {
                        dropboxSyncManager.logout()
                        navController.navigateToWelcome()
                    }
                )
            }
        }
    }
}

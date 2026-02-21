package com.example.myapplication.ui.splash

import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.data.local.MigrationManager
import com.example.myapplication.data.repository.LegacyMigrationRepository
import com.example.myapplication.ui.navigation.AppRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SplashState {
    data object Loading : SplashState
    data object Migrating : SplashState
    data class Completed(val nextRoute: String) : SplashState
}

class SplashViewModel(
    private val legacyMigrationRepository: LegacyMigrationRepository,
    private val migrationManager: MigrationManager,
    private val dropboxSyncManager: DropboxSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashState>(SplashState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        startAppInitialization()
    }

    private fun startAppInitialization() {
        viewModelScope.launch {
            // 1. Ждем разрешения к файлам
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                while (!Environment.isExternalStorageManager()) {
                    delay(500)
                }
            }

            // 2. Проверяем старую папку MyAnimeList
            if (legacyMigrationRepository.needsMigration()) {
                _uiState.update { SplashState.Migrating }
                legacyMigrationRepository.migrateLegacyDataIfNeeded()
            }

            // 3. MigrationManager видит файлы в Vetro и заливает их в SQLDelight (list, updates, ignored)
            migrationManager.runMigration()

            // 4. Роутинг: авторизован → Home, иначе → Welcome
            val route = if (dropboxSyncManager.hasToken()) AppRoute.Home.route else AppRoute.Welcome.route
            _uiState.update { SplashState.Completed(route) }
        }
    }
}

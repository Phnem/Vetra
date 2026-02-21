package com.example.myapplication.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.network.AppContentType
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.data.models.AppTheme
import com.example.myapplication.data.models.AppUpdateStatus
import com.example.myapplication.data.models.SemanticVersion
import com.example.myapplication.data.repository.AnimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

private val KEY_LANG = stringPreferencesKey("lang")
private val KEY_THEME = stringPreferencesKey("theme")
private val KEY_CONTENT_TYPE = stringPreferencesKey("contentType")

class SettingsViewModel(
    private val repository: AnimeRepository,
    private val settingsDataStore: DataStore<Preferences>
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.data.first().let { prefs ->
                _uiState.update {
                    it.copy(
                        language = AppLanguage.valueOf(prefs[KEY_LANG] ?: "EN"),
                        theme = runCatching { AppTheme.valueOf(prefs[KEY_THEME] ?: "SYSTEM") }.getOrElse { AppTheme.SYSTEM },
                        contentType = runCatching { AppContentType.valueOf(prefs[KEY_CONTENT_TYPE] ?: "ANIME") }.getOrElse { AppContentType.ANIME }
                    )
                }
            }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            _uiState.update { it.copy(language = language) }
            settingsDataStore.edit { it[KEY_LANG] = language.name }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            _uiState.update { it.copy(theme = theme) }
            settingsDataStore.edit { it[KEY_THEME] = theme.name }
        }
    }

    fun setContentType(contentType: AppContentType) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentType = contentType) }
            settingsDataStore.edit { it[KEY_CONTENT_TYPE] = contentType.name }
        }
    }

    fun checkAppUpdate(context: Context) {
        if (_uiState.value.updateStatus == AppUpdateStatus.LOADING) return
        viewModelScope.launch {
            _uiState.update { it.copy(updateStatus = AppUpdateStatus.LOADING) }
            if (_uiState.value.currentVersion.isEmpty()) {
                try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    _uiState.update { it.copy(currentVersion = pInfo.versionName ?: "v1.0.0") }
                } catch (e: Exception) {
                    _uiState.update { it.copy(currentVersion = "v1.0.0") }
                }
            }
            val localVer = _uiState.value.currentVersion
            repository.checkGithubUpdate()
                .fold(
                    onSuccess = { release ->
                        if (release != null && isNewerVersion(localVer, release.tagName)) {
                            _uiState.update {
                                it.copy(
                                    updateStatus = AppUpdateStatus.UPDATE_AVAILABLE,
                                    latestDownloadUrl = release.downloadUrl
                                )
                            }
                        } else {
                            _uiState.update { it.copy(updateStatus = AppUpdateStatus.NO_UPDATE) }
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(updateStatus = AppUpdateStatus.ERROR) }
                    }
                )
        }
    }

    private fun isNewerVersion(local: String, remote: String): Boolean = runCatching {
        parseVersion(remote) > parseVersion(local)
    }.getOrElse { false }

    private fun parseVersion(versionStr: String): SemanticVersion {
        val clean = versionStr.removePrefix("v").trim()
        val dashSplit = clean.split("-", limit = 2)
        val dots = dashSplit[0].split(".").map { it.toIntOrNull() ?: 0 }
        return SemanticVersion(
            dots.getOrElse(0) { 0 },
            dots.getOrElse(1) { 0 },
            dots.getOrElse(2) { 0 },
            if (dashSplit.size > 1) dashSplit[1] else ""
        )
    }
}

package com.example.myapplication.ui.settings

import com.example.myapplication.network.AppContentType
import com.example.myapplication.network.AppLanguage
import com.example.myapplication.data.models.AppTheme
import com.example.myapplication.data.models.AppUpdateStatus

/**
 * Immutable UI state для SettingsScreen
 */
data class SettingsUiState(
    val language: AppLanguage = AppLanguage.EN,
    val theme: AppTheme = AppTheme.SYSTEM,
    val contentType: AppContentType = AppContentType.ANIME,
    val updateStatus: AppUpdateStatus = AppUpdateStatus.IDLE,
    val currentVersion: String = "",
    val latestDownloadUrl: String? = null,
    val isLoading: Boolean = false
)

package com.example.myapplication

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.data.models.AppTheme
import com.example.myapplication.ui.navigation.AppNavGraph
import com.example.myapplication.ui.navigation.AppRoute
import com.example.myapplication.ui.shared.theme.OneUiTheme
import com.example.myapplication.ui.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkPerms()

        setContent {
            val context = LocalContext.current
            val isSystemDark = isSystemInDarkTheme()

            val settingsViewModel: SettingsViewModel = koinViewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            val useDarkTheme = when (settingsState.theme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemDark
            }

            OneUiTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val startDestination = AppRoute.Splash.route

                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }

    private fun checkPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !android.os.Environment.isExternalStorageManager()) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
            android.widget.Toast.makeText(this, "Need file access", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        DropboxSyncManager.onOAuthResult()
    }
}

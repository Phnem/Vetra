package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.WelcomeScreen
import org.koin.compose.koinInject
import com.example.myapplication.ui.addedit.AddEditScreen
import com.example.myapplication.ui.addedit.AddEditViewModel
import com.example.myapplication.ui.details.DetailsViewModel
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.settings.SettingsViewModel
import com.example.myapplication.ui.splash.SplashViewModel
import com.example.myapplication.ui.splash.VetroSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = AppRoute.Welcome.route
) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val addEditViewModel: AddEditViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val detailsViewModel: DetailsViewModel = koinViewModel()
    val dropboxSyncManager: DropboxSyncManager = org.koin.compose.koinInject()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        homeViewModel.scheduleBackgroundWork(context)
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(AppRoute.Splash.route) {
                val splashViewModel: SplashViewModel = koinViewModel()
                val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()
                VetroSplashScreen(
                    uiState = splashState,
                    onSplashComplete = { nextRoute ->
                        navController.navigate(nextRoute) {
                            popUpTo(AppRoute.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoute.Welcome.route) {
                val lifecycleOwner = LocalLifecycleOwner.current

                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            dropboxSyncManager.onOAuthResult()

                            if (dropboxSyncManager.hasToken()) {
                                navController.navigateToHome()
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                
                WelcomeScreen(
                    onLoginClick = {
                        dropboxSyncManager.startOAuth(context as android.app.Activity)
                    },
                    onGuestClick = {
                        navController.navigateToHome()
                    }
                )
            }

            composable(AppRoute.Home.route) {
                HomeScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    detailsViewModel = detailsViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable(
                route = AppRoute.AddEdit.ROUTE,
                arguments = listOf(
                    navArgument(AppRoute.AddEdit.ARG_ANIME_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val animeId = backStackEntry.arguments?.getString(AppRoute.AddEdit.ARG_ANIME_ID)
                
                AddEditScreen(
                    navController = navController,
                    viewModel = addEditViewModel,
                    homeViewModel = homeViewModel,
                    animeId = animeId,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable(AppRoute.Settings.route) {
                SettingsScreen(
                    navController = navController,
                    viewModel = settingsViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }
        }
    }
}

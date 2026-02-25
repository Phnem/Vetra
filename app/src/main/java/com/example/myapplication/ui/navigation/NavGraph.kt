package com.example.myapplication.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.myapplication.DropboxSyncManager
import com.example.myapplication.WelcomeScreen
import com.example.myapplication.ui.addedit.AddEditScreen
import com.example.myapplication.ui.addedit.AddEditViewModel
import com.example.myapplication.ui.details.DetailsScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.settings.SettingsViewModel
import com.example.myapplication.ui.splash.SplashViewModel
import com.example.myapplication.ui.splash.VetroSplashScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/** Spring-based spatial transitions для премиального UX (Details и др.). */
private object PremiumTransitions {
    val detailsEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(durationMillis = 300))
    }
    val detailsExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
        scaleOut(
            targetScale = 0.95f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    }
    val detailsPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.EnterTransition = {
        scaleIn(
            initialScale = 0.95f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(durationMillis = 300))
    }
    val detailsPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> androidx.compose.animation.ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + scaleOut(
            targetScale = 0.85f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeOut(animationSpec = tween(durationMillis = 250))
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: Any = SplashRoute
) {
    val homeViewModel: HomeViewModel = koinViewModel()
    val addEditViewModel: AddEditViewModel = koinViewModel()
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val dropboxSyncManager: DropboxSyncManager = koinInject()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        homeViewModel.scheduleBackgroundWork(context)
    }

    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable<SplashRoute> {
                val splashViewModel: SplashViewModel = koinViewModel()
                val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()
                VetroSplashScreen(
                    uiState = splashState,
                    onSplashComplete = { nextRoute ->
                        when (nextRoute) {
                            "home" -> navController.navigate(HomeRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                            else -> navController.navigate(WelcomeRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable<WelcomeRoute> {
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
                    onGuestClick = { navController.navigateToHome() }
                )
            }

            composable<HomeRoute> {
                HomeScreen(
                    navController = navController,
                    viewModel = homeViewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<DetailsRoute>(
                enterTransition = PremiumTransitions.detailsEnter,
                exitTransition = PremiumTransitions.detailsExit,
                popEnterTransition = PremiumTransitions.detailsPopEnter,
                popExitTransition = PremiumTransitions.detailsPopExit
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<DetailsRoute>()
                DetailsScreen(
                    animeId = route.animeId,
                    navController = navController,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<AddEditRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<AddEditRoute>()
                AddEditScreen(
                    navController = navController,
                    viewModel = addEditViewModel,
                    homeViewModel = homeViewModel,
                    animeId = route.animeId,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<SettingsRoute> {
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

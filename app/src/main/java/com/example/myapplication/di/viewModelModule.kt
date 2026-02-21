package com.example.myapplication.di

import com.example.myapplication.ui.addedit.AddEditViewModel
import com.example.myapplication.ui.details.DetailsViewModel
import com.example.myapplication.ui.home.HomeViewModel
import com.example.myapplication.ui.settings.SettingsViewModel
import com.example.myapplication.ui.splash.SplashViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        SplashViewModel(
            legacyMigrationRepository = get(),
            migrationManager = get(),
            dropboxSyncManager = get()
        )
    }
    viewModel { HomeViewModel(repository = get(), localDataSource = get()) }
    viewModel { AddEditViewModel(repository = get(), localDataSource = get()) }
    viewModel { SettingsViewModel(repository = get(), settingsDataStore = get(named("settings"))) }
    viewModel { DetailsViewModel(repository = get()) }
}

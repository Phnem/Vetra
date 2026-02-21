package com.example.myapplication.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.local.AnimeDatabase
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.local.LegacyMigrationRepositoryImpl
import com.example.myapplication.data.local.MigrationManager
import com.example.myapplication.data.local.SQLDelightDatabaseFactory
import com.example.myapplication.data.repository.LegacyMigrationRepository
import com.example.myapplication.DropboxSyncManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val Context.migrationDataStore: DataStore<Preferences> by preferencesDataStore(name = "migration_prefs")
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_prefs")

val databaseModule = module {
    single { SQLDelightDatabaseFactory(androidContext()) }
    single { get<SQLDelightDatabaseFactory>().createDriver() }
    single { AnimeDatabase(get()) }
    single { AnimeLocalDataSource(get()) }
    single<LegacyMigrationRepository> {
        LegacyMigrationRepositoryImpl(context = androidContext())
    }

    single<DataStore<Preferences>>(named("migration")) {
        androidContext().migrationDataStore
    }
    single<DataStore<Preferences>>(named("settings")) {
        androidContext().settingsDataStore
    }
    single {
        MigrationManager(
            context = androidContext(),
            localDataSource = get(),
            dataStore = get(named("migration"))
        )
    }

    single<DropboxSyncManager> {
        DropboxSyncManager(
            context = androidContext(),
            databaseFactory = get(),
            animeLocalDataSource = get()
        )
    }
}

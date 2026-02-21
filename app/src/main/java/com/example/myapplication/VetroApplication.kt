package com.example.myapplication

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.example.myapplication.di.appModule
import com.example.myapplication.di.databaseModule
import com.example.myapplication.di.networkModule
import com.example.myapplication.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VetroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runSyncStatusMigration()
        startKoin {
            androidContext(this@VetroApplication)
            modules(
                appModule,
                networkModule,
                databaseModule,
                viewModelModule
            )
        }
    }

    /** One-time migration: add sync_status column for Delta Sync (existing DBs). */
    private fun runSyncStatusMigration() {
        try {
            val dbPath = getDatabasePath("anime.db")
            if (!dbPath.exists()) return
            SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("ALTER TABLE anime ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'")
            }
        } catch (e: Exception) {
            if (!e.message.orEmpty().contains("duplicate column name")) throw e
        }
    }
}

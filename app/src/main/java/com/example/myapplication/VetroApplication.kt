package com.example.myapplication

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import android.util.Log
import com.example.myapplication.di.appModule
import com.example.myapplication.di.databaseModule
import com.example.myapplication.di.networkModule
import com.example.myapplication.di.viewModelModule
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.io.File

class VetroApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        runVetroDbMigrationIfNeeded()
        runSyncStatusMigration()
        runCommentColumnMigration()
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .allowHardware(true)
            .build()
    }

    /**
     * Миграция базы из папки Vetro.
     * Ожидает выдачи прав и перезаписывает пустую БД при первом удачном доступе.
     */
    private fun runVetroDbMigrationIfNeeded() {
        try {
            val prefs = getSharedPreferences("vetro_migration", Context.MODE_PRIVATE)
            if (prefs.getBoolean("is_db_migrated", false)) return

            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val vetroDir = File(documentsDir, "Vetro")
            val vetroDbFile = File(vetroDir, "anime.db")
            if (!vetroDbFile.exists()) return

            val targetDbFile = getDatabasePath("anime.db")
            targetDbFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            val targetWal = File(targetDbFile.parentFile, "anime.db-wal")
            val targetShm = File(targetDbFile.parentFile, "anime.db-shm")
            if (targetWal.exists()) targetWal.delete()
            if (targetShm.exists()) targetShm.delete()

            vetroDbFile.copyTo(targetDbFile, overwrite = true)

            val vetroWal = File(vetroDir, "anime.db-wal")
            if (vetroWal.exists()) vetroWal.copyTo(targetWal, overwrite = true)
            val vetroShm = File(vetroDir, "anime.db-shm")
            if (vetroShm.exists()) vetroShm.copyTo(targetShm, overwrite = true)

            prefs.edit().putBoolean("is_db_migrated", true).apply()
            Log.d("Migration", "Успешно мигрировали базу данных из Vetro")
        } catch (e: Exception) {
            Log.e("Migration", "Ошибка при миграции из папки Vetro: ${e.message}")
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

    /** One-time migration: add comment column (existing DBs). */
    private fun runCommentColumnMigration() {
        try {
            val dbPath = getDatabasePath("anime.db")
            if (!dbPath.exists()) return
            SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("ALTER TABLE anime ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        } catch (e: Exception) {
            if (!e.message.orEmpty().contains("duplicate column name")) throw e
        }
    }
}

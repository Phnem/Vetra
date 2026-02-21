package com.example.myapplication.data.repository

/**
 * Репозиторий миграции данных со старого формата (папка MyAnimeList + list.json)
 * в текущий (Vetro + SQLDelight).
 */
interface LegacyMigrationRepository {
    /** Быстрая проверка O(1): нужна ли миграция (существует ли старая папка). */
    suspend fun needsMigration(): Boolean
    suspend fun migrateLegacyDataIfNeeded(): Result<Unit>
}

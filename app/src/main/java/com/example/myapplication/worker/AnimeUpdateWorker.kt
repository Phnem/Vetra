package com.example.myapplication.worker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.data.repository.AnimeRepository
import com.example.myapplication.network.AppContentType
import com.example.myapplication.notifications.AnimeNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class AnimeUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val repository: AnimeRepository by inject()
    private val localDataSource: AnimeLocalDataSource by inject()
    private val notifier: AnimeNotifier by inject()
    private val settingsDataStore: DataStore<Preferences> by inject(named("settings"))

    private val contentTypeKey = stringPreferencesKey("contentType")

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val contentTypeStr = settingsDataStore.data.first()[contentTypeKey] ?: "ANIME"
            val contentType = try {
                AppContentType.valueOf(contentTypeStr)
            } catch (e: Exception) {
                AppContentType.ANIME
            }
            val animeList = localDataSource.getAllAnimeList()
            if (animeList.isEmpty()) return@withContext Result.success()
            val ignoredMap = localDataSource.getIgnoredMap()
            val existingUpdates = localDataSource.getUpdates().toMutableList()
            var foundNew = false
            animeList.forEach { anime ->
                val result = repository.findTotalEpisodes(anime.title, anime.categoryType, contentType).getOrNull()
                if (result != null) {
                    val (remoteEps, sourceName) = result
                    val isIgnored = ignoredMap[anime.id] == remoteEps
                    if (remoteEps > anime.episodes && !isIgnored) {
                        val updateObj = AnimeUpdate(anime.id, anime.title, anime.episodes, remoteEps, sourceName)
                        if (existingUpdates.none { it.animeId == anime.id && it.newEpisodes == remoteEps }) {
                            existingUpdates.removeAll { it.animeId == anime.id }
                            existingUpdates.add(updateObj)
                            foundNew = true
                        }
                    }
                }
            }
            if (foundNew) {
                localDataSource.setUpdates(existingUpdates)
                existingUpdates.forEach { update -> notifier.showUpdateNotification(update) }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}

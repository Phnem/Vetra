package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.MainActivity
import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.AnimeUpdate
import com.example.myapplication.network.AppContentType
import com.example.myapplication.data.repository.AnimeRepository
import com.example.myapplication.receiver.AnimeUpdateReceiver
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
                // Отправляем отдельное уведомление для каждого нового обновления
                existingUpdates.forEach { update ->
                    sendSystemNotification(update)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendSystemNotification(update: AnimeUpdate) {
        val channelId = "anime_updates_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Anime Updates", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        
        // Уникальный ID для каждого уведомления (используем hash code animeId)
        val notifId = update.animeId.hashCode()
        
        // Intent для открытия приложения при клике на уведомление
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent для кнопки "Принять"
        val acceptIntent = Intent(applicationContext, AnimeUpdateReceiver::class.java).apply {
            action = "ACTION_ACCEPT_UPDATE"
            putExtra("ANIME_ID", update.animeId)
            putExtra("NEW_EPS", update.newEpisodes)
            putExtra("NOTIF_ID", notifId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifId,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Intent для кнопки "Отклонить"
        val dismissIntent = Intent(applicationContext, AnimeUpdateReceiver::class.java).apply {
            action = "ACTION_DISMISS_UPDATE"
            putExtra("ANIME_ID", update.animeId)
            putExtra("NEW_EPS", update.newEpisodes)
            putExtra("NOTIF_ID", notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifId + 10000, // Разные request codes для разных pending intents
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Vetro Updates")
            .setContentText("${update.title}: ${update.currentEpisodes} → ${update.newEpisodes} эп.")
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_add,
                "Принять",
                acceptPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Отклонить",
                dismissPendingIntent
            )
            .setAutoCancel(true)
            .build()
        
        manager.notify(notifId, notification)
    }
}

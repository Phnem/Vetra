package com.example.myapplication.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.data.local.AnimeLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AnimeUpdateReceiver : BroadcastReceiver(), KoinComponent {
    
    // Инжектим репозитории через Koin, так как мы не в Activity
    private val localDataSource: AnimeLocalDataSource by inject()
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val animeId = intent.getStringExtra("ANIME_ID") ?: return
        val newEps = intent.getIntExtra("NEW_EPS", -1)
        val notifId = intent.getIntExtra("NOTIF_ID", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Говорим системе: "Не убивай процесс, я еще работаю!"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    "ACTION_ACCEPT_UPDATE" -> {
                        val anime = localDataSource.getAnimeById(animeId)
                        if (anime != null && newEps != -1) {
                            localDataSource.updateAnime(anime.copy(episodes = newEps))
                            // Удаляем апдейт из списка
                            val updates = localDataSource.getUpdates().filterNot { it.animeId == animeId }
                            localDataSource.setUpdates(updates)
                        }
                    }
                    "ACTION_DISMISS_UPDATE" -> {
                        if (newEps != -1) {
                            localDataSource.addIgnored(animeId, newEps)
                            val updates = localDataSource.getUpdates().filterNot { it.animeId == animeId }
                            localDataSource.setUpdates(updates)
                        }
                    }
                }
                // Закрываем уведомление после действия
                notificationManager.cancel(notifId)
            } catch (e: Exception) {
                // Логируем ошибку, но не падаем
                android.util.Log.e("AnimeUpdateReceiver", "Error processing action: $action", e)
            } finally {
                // Обязательно говорим системе: "Я закончил, можешь освобождать ресурсы"
                pendingResult.finish()
            }
        }
    }
}

package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.min

enum class AppContentType { ANIME, MOVIES }

data class EpisodeUpdateResult(
    val episodeCount: Int,
    val sourceName: String
)

object APIfinder {
    private const val TMDB_KEY = "4f4dc3cd35d58a551162eefe92ff549c"

    private val mutex = Mutex()
    private suspend fun <T> executeSafe(block: suspend () -> T): T {
        mutex.withLock {
            delay(1200)
            return block()
        }
    }

    suspend fun findTotalEpisodes(
        title: String,
        categoryType: String,
        appContentType: AppContentType
    ): EpisodeUpdateResult? = withContext(Dispatchers.IO) {
        val normalizeTitle = title.trim()

        if (appContentType == AppContentType.ANIME) {
            val anilist = checkAniList(normalizeTitle)
            if (anilist != null) {
                return@withContext anilist
            }

            val shiki = checkShikimori(normalizeTitle)
            if (shiki != null) {
                return@withContext shiki
            }

            val jikan = checkJikan(normalizeTitle)
            if (jikan != null) {
                return@withContext jikan
            }

            return@withContext null
        }

        if (appContentType == AppContentType.MOVIES) {
            return@withContext checkTmdb(normalizeTitle)
        }

        return@withContext null
    }

    // --- ЛОГИКА API ---

    private suspend fun checkAniList(query: String): EpisodeUpdateResult? {
        return try {
            val url = URL("https://graphql.anilist.co")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val gql = """
            {
              "query": "query (${'$'}q: String) { Media (search: ${'$'}q, type: ANIME) { title { romaji english } episodes nextAiringEpisode { episode } status } }",
              "variables": { "q": "$query" }
            }
            """.trimIndent()

            conn.outputStream.write(gql.toByteArray())

            if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JsonParser.parseString(resp).asJsonObject
                if (!json.has("data") || json.get("data").isJsonNull) {
                    return null
                }
                val media = json.getAsJsonObject("data").getAsJsonObject("Media") ?: return null

                val titleObj = media.getAsJsonObject("title")
                val romaji = if (titleObj.has("romaji") && !titleObj.get("romaji").isJsonNull) titleObj.get("romaji").asString else ""
                val english = if (titleObj.has("english") && !titleObj.get("english").isJsonNull) titleObj.get("english").asString else ""

                if (!isTitleSimilar(query, romaji) && !isTitleSimilar(query, english)) {
                    return null
                }

                var count = 0
                if (media.has("nextAiringEpisode") && !media.get("nextAiringEpisode").isJsonNull) {
                    val nextEp = media.getAsJsonObject("nextAiringEpisode").get("episode").asInt
                    count = nextEp - 1
                } else {
                    if (media.has("episodes") && !media.get("episodes").isJsonNull) {
                        count = media.get("episodes").asInt
                    }
                }

                if (count > 0) EpisodeUpdateResult(count, "AniList") else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun checkShikimori(query: String): EpisodeUpdateResult? {
        return try {
            executeSafe {
                val searchUrl = "https://shikimori.one/api/animes?search=${enc(query)}&limit=1"
                val jsonArr = getJson(searchUrl).asJsonArray
                if (jsonArr.size() == 0) {
                    return@executeSafe null
                }

                val anime = jsonArr[0].asJsonObject
                val name = anime.get("name").asString
                val russian = if(anime.has("russian") && !anime.get("russian").isJsonNull) anime.get("russian").asString else ""

                if (!isTitleSimilar(query, name) && !isTitleSimilar(query, russian)) {
                    return@executeSafe null
                }

                val episodes = if (anime.get("episodes_aired").asInt > 0) {
                    anime.get("episodes_aired").asInt
                } else if (!anime.get("episodes").isJsonNull) {
                    anime.get("episodes").asInt
                } else 0

                if (episodes > 0) EpisodeUpdateResult(episodes, "Shikimori") else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun checkJikan(query: String): EpisodeUpdateResult? {
        return try {
            executeSafe {
                val searchUrl = "https://api.jikan.moe/v4/anime?q=${enc(query)}&limit=1"
                val resp = getJson(searchUrl).asJsonObject
                val data = resp.getAsJsonArray("data")
                if (data == null || data.size() == 0) {
                    return@executeSafe null
                }

                val anime = data[0].asJsonObject
                val titleDefault = anime.get("title").asString
                val titleEng = if(anime.has("title_english") && !anime.get("title_english").isJsonNull) anime.get("title_english").asString else ""

                if (!isTitleSimilar(query, titleDefault) && !isTitleSimilar(query, titleEng)) {
                    return@executeSafe null
                }

                val episodes = if (anime.get("episodes").isJsonNull) 0 else anime.get("episodes").asInt
                if (episodes > 0) EpisodeUpdateResult(episodes, "Jikan") else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun checkTmdb(query: String): EpisodeUpdateResult? {
        return try {
            executeSafe {
                val searchUrl = "https://api.themoviedb.org/3/search/tv?api_key=$TMDB_KEY&query=${enc(query)}"
                val results = getJson(searchUrl).asJsonObject.getAsJsonArray("results")

                if (results != null && results.size() > 0) {
                    val show = results[0].asJsonObject
                    val originalName = show.get("original_name").asString
                    val name = show.get("name").asString

                    if (!isTitleSimilar(query, originalName) && !isTitleSimilar(query, name)) {
                        return@executeSafe null
                    }

                    val id = show.get("id").asInt
                    val detailsUrl = "https://api.themoviedb.org/3/tv/$id?api_key=$TMDB_KEY"
                    val details = getJson(detailsUrl).asJsonObject
                    val seasons = details.getAsJsonArray("seasons")

                    var totalEpisodes = 0
                    seasons.forEach { s ->
                        val seasonObj = s.asJsonObject
                        val sNum = seasonObj.get("season_number").asInt
                        if (sNum > 0 || seasons.size() == 1) {
                            totalEpisodes += seasonObj.get("episode_count").asInt
                        }
                    }

                    if (totalEpisodes > 0) EpisodeUpdateResult(totalEpisodes, "TMDB") else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getJson(urlString: String): com.google.gson.JsonElement {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "MAList-Episode-Finder")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        if (conn.responseCode in 200..299) {
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            return JsonParser.parseString(jsonStr)
        } else {
            throw Exception("HTTP ${conn.responseCode}")
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun isTitleSimilar(query: String, target: String): Boolean {
        if (target.isBlank()) return false
        val q = query.lowercase().replace(Regex("[^a-z0-9]"), "")
        val t = target.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (t.contains(q) || q.contains(t)) return true
        val dist = levenshtein(q, t)
        val maxLength = maxOf(q.length, t.length)
        return (1.0 - (dist.toDouble() / maxLength)) > 0.7
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) return 0
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}

// ==========================================
// BACKGROUND WORKER (ФОНОВАЯ ЗАДАЧА)
// ==========================================
class AnimeUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val root = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Vetro")
            val listFile = File(root, "list.json")
            val ignoredFile = File(root, "ignored.json")
            val updatesFile = File(root, "updates.json")
            val settingsFile = File(root, "settings.json")

            if (!listFile.exists()) {
                return@withContext Result.success()
            }

            val gson = Gson()

            val contentType = try {
                if (settingsFile.exists()) {
                    val settings: Map<String, String> = gson.fromJson(settingsFile.readText(), object : TypeToken<Map<String, String>>() {}.type)
                    AppContentType.valueOf(settings["contentType"] ?: "ANIME")
                } else AppContentType.ANIME
            } catch (e: Exception) {
                AppContentType.ANIME
            }

            val animeListType = object : TypeToken<List<Anime>>() {}.type
            val ignoredType = object : TypeToken<Map<String, Int>>() {}.type
            val updatesType = object : TypeToken<List<AnimeUpdate>>() {}.type

            val animeList: List<Anime> = try {
                gson.fromJson(listFile.readText(), animeListType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            val ignoredMap: Map<String, Int> = if (ignoredFile.exists()) {
                try {
                    gson.fromJson(ignoredFile.readText(), ignoredType) ?: emptyMap()
                } catch (e: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            val existingUpdates: MutableList<AnimeUpdate> = if (updatesFile.exists()) {
                try {
                    gson.fromJson(updatesFile.readText(), updatesType) ?: mutableListOf()
                } catch (e: Exception) {
                    mutableListOf()
                }
            } else mutableListOf()

            var foundNew = false

            animeList.forEach { anime ->
                val result = APIfinder.findTotalEpisodes(anime.title, anime.categoryType, contentType)

                if (result != null) {
                    val remoteEps = result.episodeCount
                    val sourceName = result.sourceName
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
                updatesFile.writeText(gson.toJson(existingUpdates))
                sendSystemNotification(existingUpdates.size)
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure()
        }
    }

    private fun sendSystemNotification(count: Int) {
        val channelId = "anime_updates_channel"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Anime Updates", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Vetro Updates")
            .setContentText("Found new episodes for $count titles!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}
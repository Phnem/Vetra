package com.example.myapplication.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.appendPathSegments
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Ktor 3.x–based API facade. Uses Shikimori + AniList remote data sources,
 * returns Result everywhere.
 */
class VetroApiService(
    private val httpClient: HttpClient,
    private val shikimori: ShikimoriRemoteDataSource,
    private val aniList: AniListRemoteDataSource
) : ApiService {

    private val rateLimit = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchDetails(title: String, language: AppLanguage): Result<AnimeDetails?> {
        return runCatching {
            rateLimit.withLock { delay(1200) }
            val query = title.trim()
            when (language) {
                AppLanguage.EN -> aniList.fetchAnimeDetails(query).getOrNull()
                    ?: shikimori.fetchAnimeDetails(query).getOrNull()
                AppLanguage.RU -> shikimori.fetchAnimeDetails(query).getOrNull()
                    ?: aniList.fetchAnimeDetails(query).getOrNull()
            }
        }
    }

    override suspend fun findTotalEpisodes(
        title: String,
        categoryType: String,
        appContentType: AppContentType
    ): Result<Pair<Int, String>?> {
        return runCatching {
            rateLimit.withLock { delay(1200) }
            when (appContentType) {
                AppContentType.ANIME -> {
                    aniList.findTotalEpisodes(title.trim()).getOrNull()
                        ?: shikimori.findTotalEpisodes(title.trim()).getOrNull()
                        ?: checkJikan(title.trim())
                }
                AppContentType.MOVIE, AppContentType.SERIES -> checkTmdb(title.trim())
            }
        }
    }

    override suspend fun checkGithubUpdate(): Result<GithubReleaseInfo?> {
        return runCatching {
            val response = httpClient.get("https://api.github.com/repos/2004i/Vetro/releases/latest").bodyAsText()
            val root = json.parseToJsonElement(response).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.content ?: ""
            val htmlUrl = root["html_url"]?.jsonPrimitive?.content ?: ""
            val assets = root["assets"]?.jsonArray
            val downloadUrl = assets?.firstOrNull()?.jsonObject?.get("browser_download_url")?.jsonPrimitive?.content ?: ""
            if (tagName.isNotEmpty()) GithubReleaseInfo(tagName, htmlUrl, downloadUrl) else null
        }
    }

    private suspend fun checkJikan(query: String): Pair<Int, String>? {
        return runCatching {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.jikan.moe"
                    appendPathSegments("v4", "anime")
                    parameters.append("q", query)
                    parameters.append("limit", "1")
                }
            }.bodyAsText()
            val root = json.parseToJsonElement(response).jsonObject
            val data = root["data"]?.jsonArray ?: return@runCatching null
            val first = data.firstOrNull()?.jsonObject ?: return@runCatching null
            val title = first["title"]?.jsonPrimitive?.content ?: return@runCatching null
            val titleEng = first["title_english"]?.jsonPrimitive?.content ?: ""
            if (!isTitleSimilar(query, title, titleEng)) return@runCatching null
            val episodes = first["episodes"]?.jsonPrimitive?.intOrNull ?: 0
            if (episodes > 0) episodes to "Jikan" else null
        }.getOrNull()
    }

    private suspend fun checkTmdb(query: String): Pair<Int, String>? {
        return runCatching {
            val key = "4f4dc3cd35d58a551162eefe92ff549c"
            val searchResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.themoviedb.org"
                    appendPathSegments("3", "search", "tv")
                    parameters.append("api_key", key)
                    parameters.append("query", query)
                }
            }.bodyAsText()
            val root = json.parseToJsonElement(searchResponse).jsonObject
            val results = root["results"]?.jsonArray ?: return@runCatching null
            val show = results.firstOrNull()?.jsonObject ?: return@runCatching null
            val id = show["id"]?.jsonPrimitive?.int ?: return@runCatching null
            val detailsResponse = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = "api.themoviedb.org"
                    appendPathSegments("3", "tv", id.toString())
                    parameters.append("api_key", key)
                }
            }.bodyAsText()
            val details = json.parseToJsonElement(detailsResponse).jsonObject
            val seasons = details["seasons"]?.jsonArray ?: return@runCatching null
            var total = 0
            for (s in seasons) {
                val obj = s.jsonObject
                val sNum = obj["season_number"]?.jsonPrimitive?.int ?: 0
                if (sNum > 0 || seasons.size == 1) {
                    total += obj["episode_count"]?.jsonPrimitive?.int ?: 0
                }
            }
            if (total > 0) total to "TMDB" else null
        }.getOrNull()
    }

    private fun isTitleSimilar(query: String, vararg targets: String?): Boolean {
        val q = query.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        if (q.isEmpty()) return false
        for (t in targets) {
            if (t.isNullOrBlank()) continue
            val n = t.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
            if (n.contains(q) || q.contains(n)) return true
        }
        return false
    }
}

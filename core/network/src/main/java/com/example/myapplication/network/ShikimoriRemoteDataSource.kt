package com.example.myapplication.network

import com.example.myapplication.network.dto.ShikimoriSearchItemDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.delay

/**
 * Type-safe remote data source for Shikimori API.
 */
class ShikimoriRemoteDataSource(
    private val client: HttpClient
) {

    suspend fun fetchAnimeDetails(query: String): Result<AnimeDetails> = runCatching {
        val searchResponse = client.get("https://shikimori.one/api/animes") {
            parameter("search", query.trim())
            parameter("limit", "1")
        }.body<List<ShikimoriSearchItemDto>>()

        val first = searchResponse.firstOrNull()
            ?: throw NoSuchElementException("Anime not found: $query")

        if (!isTitleSimilar(query, first.name, first.russian)) {
            throw NoSuchElementException("Title mismatch: $query")
        }

        delay(300)
        val full = client.get("https://shikimori.one/api/animes/${first.id}").body<ShikimoriSearchItemDto>()
        full.toDomain()
    }

    suspend fun findTotalEpisodes(query: String): Result<Pair<Int, String>?> = runCatching {
        val searchResponse = client.get("https://shikimori.one/api/animes") {
            parameter("search", query.trim())
            parameter("limit", "1")
        }.body<List<ShikimoriSearchItemDto>>()

        val first = searchResponse.firstOrNull() ?: return@runCatching null
        if (!isTitleSimilar(query, first.name, first.russian)) return@runCatching null
        val episodes = first.episodesAired ?: first.episodes ?: 0
        if (episodes > 0) episodes to "Shikimori" else null
    }

    private fun ShikimoriSearchItemDto.toDomain(): AnimeDetails {
        val desc = description
            ?.replace(Regex("\\[.*?\\]"), "")
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim() ?: ""
        val posterUrl = image?.original?.let { if (it.startsWith("http")) it else "https://shikimori.one$it" }
        return AnimeDetails(
            title = name ?: "Unknown",
            altTitle = russian?.takeIf { it.isNotBlank() },
            description = desc,
            type = kind ?: "",
            status = status ?: "",
            episodesAired = episodesAired ?: episodes ?: 0,
            episodesTotal = episodes,
            nextEpisode = null,
            genres = genres?.mapNotNull { it.russian ?: it.name } ?: emptyList(),
            rating = score?.toFloatOrNull()?.toInt(),
            posterUrl = posterUrl,
            source = "Shikimori"
        )
    }

    private fun isTitleSimilar(query: String, vararg targets: String?): Boolean {
        val q = query.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        if (q.isEmpty()) return false
        for (t in targets) {
            if (t.isNullOrBlank()) continue
            val normalized = t.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
            if (normalized.contains(q) || q.contains(normalized)) return true
            val dist = levenshtein(q, normalized)
            val maxLen = maxOf(q.length, normalized.length)
            if ((1.0 - dist.toDouble() / maxLen) > 0.7) return true
        }
        return false
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        if (lhs == rhs) return 0
        if (lhs.isEmpty()) return rhs.length
        if (rhs.isEmpty()) return lhs.length
        var cost = IntArray(lhs.length + 1) { it }
        var newCost = IntArray(lhs.length + 1)
        for (i in 1..rhs.length) {
            newCost[0] = i
            for (j in 1..lhs.length) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                newCost[j] = minOf(cost[j - 1] + match, cost[j] + 1, newCost[j - 1] + 1)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhs.length]
    }
}

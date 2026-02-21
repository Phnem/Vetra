package com.example.myapplication.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.myapplication.network.anilist.SearchMediaQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AniList GraphQL API via Apollo Kotlin. Type-safe queries from .graphql → generated models.
 */
class AniListRemoteDataSource(
    private val apolloClient: ApolloClient
) {
    suspend fun fetchAnimeDetails(query: String): Result<AnimeDetails?> = runCatching {
        withContext(Dispatchers.IO) {
            val response = apolloClient.query(SearchMediaQuery(q = Optional.presentIfNotNull(query))).execute()
            val media = response.data?.Media ?: return@withContext null
            val title = media.title
            val romaji = title?.romaji?.orEmpty() ?: ""
            val english = title?.english?.orEmpty() ?: ""
            if (!isTitleSimilar(query, romaji, english)) return@withContext null

            val desc = media.description?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
            val type = media.type?.name ?: "ANIME"
            val status = media.status?.name ?: ""
            val episodes = media.episodes ?: 0
            val nextEp = media.nextAiringEpisode?.let { "Episode ${it.episode}" }
            val genres = media.genres?.filterNotNull() ?: emptyList()
            val rating = media.averageScore

            AnimeDetails(
                title = romaji.ifEmpty { english }.ifEmpty { query },
                altTitle = if (english.isNotEmpty() && english != romaji) english else null,
                description = desc,
                type = type,
                status = status,
                episodesAired = episodes,
                episodesTotal = media.episodes,
                nextEpisode = nextEp,
                genres = genres,
                rating = rating,
                posterUrl = null,
                source = "AniList"
            )
        }
    }

    suspend fun findTotalEpisodes(query: String): Result<Pair<Int, String>?> = runCatching {
        withContext(Dispatchers.IO) {
            val response = apolloClient.query(SearchMediaQuery(q = Optional.presentIfNotNull(query))).execute()
            val media = response.data?.Media ?: return@withContext null
            val title = media.title
            val romaji = title?.romaji?.orEmpty() ?: ""
            val english = title?.english?.orEmpty() ?: ""
            if (!isTitleSimilar(query, romaji, english)) return@withContext null

            var count = media.episodes ?: 0
            media.nextAiringEpisode?.let { count = (it.episode ?: 0) - 1 }
            if (count > 0) count to "AniList" else null
        }
    }

    private fun isTitleSimilar(query: String, vararg targets: String?): Boolean {
        val q = query.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        if (q.isEmpty()) return false
        for (t in targets) {
            if (t.isNullOrBlank()) continue
            val normalized = t.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
            if (normalized.contains(q) || q.contains(normalized)) return true
        }
        return false
    }
}

package com.example.myapplication.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.example.myapplication.network.anilist.SearchMediaPageQuery
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

            val startDate = media.startDate
            val airedOn = if (startDate?.year != null) {
                buildString {
                    append(startDate.year)
                    if (startDate.month != null) append("-${startDate.month.toString().padStart(2, '0')}")
                    if (startDate.day != null) append("-${startDate.day.toString().padStart(2, '0')}")
                }
            } else null

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
                source = "AniList",
                airedOn = airedOn
            )
        }
    }

    suspend fun searchAnime(query: String, limit: Int = 20, language: AppLanguage = AppLanguage.EN): Result<List<ApiSearchResult>> = runCatching {
        withContext(Dispatchers.IO) {
            val response = apolloClient.query(
                SearchMediaPageQuery(
                    q = Optional.presentIfNotNull(query.takeIf { it.isNotBlank() }),
                    page = Optional.present(1),
                    perPage = Optional.present(limit)
                )
            ).execute()
            val page = response.data?.Page ?: return@withContext emptyList()
            val mediaList = page.media?.filterNotNull() ?: return@withContext emptyList()
            mediaList.mapNotNull { media ->
                val title = media.title
                val romaji = title?.romaji?.orEmpty() ?: ""
                val english = title?.english?.orEmpty() ?: ""
                val displayTitle = english.ifEmpty { romaji }.ifEmpty { return@mapNotNull null }
                val desc = media.description?.replace(Regex("<[^>]+>"), "")?.trim() ?: ""
                val posterUrl = media.coverImage?.large
                ApiSearchResult(
                    title = displayTitle,
                    altTitle = if (romaji.isNotEmpty() && romaji != displayTitle) romaji else null,
                    posterUrl = posterUrl,
                    episodes = media.episodes ?: 0,
                    description = desc,
                    type = media.type?.name ?: "ANIME",
                    genres = media.genres?.filterNotNull() ?: emptyList(),
                    rating = media.averageScore,
                    source = "AniList",
                    categoryType = "ANIME",
                    externalId = media.id?.toString()
                )
            }
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
}

package com.example.myapplication.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.myapplication.data.local.AnimeDatabase
import com.example.myapplication.data.models.Anime
import com.example.myapplication.data.models.AnimeUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AnimeLocalDataSource(
    private val database: AnimeDatabase
) {

    /**
     * Реактивный поток: при любом изменении таблицы anime Flow эмитит новый список.
     * Фильтрация и сортировка выполняются в ViewModel в памяти (рекомендация для коллекций до ~3–5k записей).
     */
    fun observeAllAnime(): Flow<List<Anime>> = database.animeQueries
        .getAllAnime()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { rows -> rows.map { row -> mapRowToAnime(row.id, row.title, row.imagePath, row.episodes, row.rating, row.orderIndex, row.dateAdded, row.isFavorite, row.categoryType) } }

    fun getAnimeCount(): Int {
        return database.animeQueries.getAnimeCount().executeAsOne().toInt()
    }

    fun getMaxOrderIndex(): Int {
        return database.animeQueries.getMaxOrderIndex().executeAsOne().toInt()
    }

    fun getAnimePage(offset: Int, limit: Int): List<Anime> {
        return database.animeQueries.getAnimePaged(limit.toLong(), offset.toLong())
            .executeAsList()
            .map { row -> mapRowToAnime(row.id, row.title, row.imagePath, row.episodes, row.rating, row.orderIndex, row.dateAdded, row.isFavorite, row.categoryType) }
    }

    fun getAllAnimeList(): List<Anime> {
        return database.animeQueries.getAllAnime()
            .executeAsList()
            .map { row -> mapRowToAnime(row.id, row.title, row.imagePath, row.episodes, row.rating, row.orderIndex, row.dateAdded, row.isFavorite, row.categoryType) }
    }

    fun getAnimeById(id: String): Anime? {
        return database.animeQueries
            .getAnimeById(id)
            .executeAsOneOrNull()
            ?.let { row -> mapRowToAnime(row.id, row.title, row.imagePath, row.episodes, row.rating, row.orderIndex, row.dateAdded, row.isFavorite, row.categoryType) }
    }

    private fun mapRowToAnime(
        id: String,
        title: String,
        imagePath: String?,
        episodes: Long,
        rating: Long,
        orderIndex: Long,
        dateAdded: Long,
        isFavorite: Long,
        categoryType: String?
    ): Anime = Anime(
        id = id,
        title = title,
        episodes = episodes.toInt(),
        rating = rating.toInt(),
        imageFileName = imagePath,
        orderIndex = orderIndex.toInt(),
        dateAdded = dateAdded,
        isFavorite = isFavorite == 1L,
        tags = getTagsForAnime(id),
        categoryType = categoryType ?: ""
    )

    suspend fun insertAnime(anime: Anime) {
        database.animeQueries.transaction {
            database.animeQueries.insertAnime(
                id = anime.id,
                title = anime.title,
                imagePath = anime.imageFileName,
                episodes = anime.episodes.toLong(),
                rating = anime.rating.toLong(),
                status = "watching",
                isFavorite = if (anime.isFavorite) 1L else 0L,
                updatedAt = System.currentTimeMillis(),
                orderIndex = anime.orderIndex.toLong(),
                dateAdded = anime.dateAdded,
                categoryType = anime.categoryType
            )
            
            // Insert tags
            anime.tags.forEach { tag ->
                database.animeQueries.insertAnimeTag(
                    anime_id = anime.id,
                    tag = tag
                )
            }
        }
    }

    suspend fun updateAnime(anime: Anime) {
        database.animeQueries.transaction {
            database.animeQueries.updateAnime(
                title = anime.title,
                imagePath = anime.imageFileName,
                episodes = anime.episodes.toLong(),
                rating = anime.rating.toLong(),
                status = "watching",
                isFavorite = if (anime.isFavorite) 1L else 0L,
                updatedAt = System.currentTimeMillis(),
                orderIndex = anime.orderIndex.toLong(),
                categoryType = anime.categoryType,
                id = anime.id
            )
            
            // Update tags
            database.animeQueries.deleteAnimeTags(anime.id)
            anime.tags.forEach { tag ->
                database.animeQueries.insertAnimeTag(
                    anime_id = anime.id,
                    tag = tag
                )
            }
        }
    }

    suspend fun deleteAnime(id: String) {
        database.animeQueries.transaction {
            database.animeQueries.deleteAnimeTags(id)
            database.animeQueries.deleteAnime(id)
        }
    }

    suspend fun insertAllAnime(list: List<Anime>) {
        database.animeQueries.transaction {
            list.forEach { anime ->
                database.animeQueries.insertAnime(
                    id = anime.id,
                    title = anime.title,
                    imagePath = anime.imageFileName,
                    episodes = anime.episodes.toLong(),
                    rating = anime.rating.toLong(),
                    status = "watching",
                    isFavorite = if (anime.isFavorite) 1L else 0L,
                    updatedAt = System.currentTimeMillis(),
                    orderIndex = anime.orderIndex.toLong(),
                    dateAdded = anime.dateAdded,
                    categoryType = anime.categoryType
                )
                anime.tags.forEach { tag ->
                    database.animeQueries.insertAnimeTag(
                        anime_id = anime.id,
                        tag = tag
                    )
                }
            }
        }
    }

    fun getUpdates(): List<AnimeUpdate> {
        return database.animeQueries.getAllUpdates()
            .executeAsList()
            .map { row ->
                AnimeUpdate(
                    animeId = row.anime_id,
                    title = row.title,
                    currentEpisodes = row.current_episodes.toInt(),
                    newEpisodes = row.new_episodes.toInt(),
                    source = row.source
                )
            }
    }

    fun getIgnoredMap(): Map<String, Int> {
        return database.animeQueries.getIgnoredMap()
            .executeAsList()
            .associate { row -> row.anime_id to row.new_episodes.toInt() }
    }

    suspend fun setUpdates(updates: List<AnimeUpdate>) {
        database.animeQueries.transaction {
            database.animeQueries.deleteAllUpdates()
            updates.forEach { u ->
                database.animeQueries.insertUpdate(
                    anime_id = u.animeId,
                    title = u.title,
                    current_episodes = u.currentEpisodes.toLong(),
                    new_episodes = u.newEpisodes.toLong(),
                    source = u.source
                )
            }
        }
    }

    suspend fun addIgnored(animeId: String, newEpisodes: Int) {
        database.animeQueries.setIgnored(
            anime_id = animeId,
            new_episodes = newEpisodes.toLong()
        )
    }

    suspend fun removeUpdate(animeId: String) {
        database.animeQueries.deleteUpdateByAnimeId(animeId)
    }

    private fun getTagsForAnime(animeId: String): List<String> {
        return database.animeQueries
            .getAnimeTags(animeId)
            .executeAsList()
    }
}

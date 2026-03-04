package com.example.myapplication.domain.addedit

import com.example.myapplication.data.local.AnimeLocalDataSource
import com.example.myapplication.data.models.Anime

class GetAnimeForEditUseCase(
    private val localDataSource: AnimeLocalDataSource
) {
    operator fun invoke(animeId: String?): Result<Anime?> = runCatching {
        animeId?.let { localDataSource.getAnimeById(it) }
    }
}

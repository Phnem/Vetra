package com.example.myapplication.domain.addedit

import com.example.myapplication.data.local.AnimeLocalDataSource

class UpdateCommentUseCase(
    private val localDataSource: AnimeLocalDataSource
) {
    suspend operator fun invoke(animeId: String, comment: String): Result<Unit> = runCatching {
        localDataSource.updateAnimeComment(animeId, comment)
    }
}

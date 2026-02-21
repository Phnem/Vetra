package com.example.myapplication.network

/**
 * Network API contract. All methods return Result for idiomatic Kotlin error handling.
 */
interface ApiService {
    suspend fun fetchDetails(title: String, language: AppLanguage): Result<AnimeDetails?>
    suspend fun findTotalEpisodes(
        title: String,
        categoryType: String,
        appContentType: AppContentType
    ): Result<Pair<Int, String>?>
    suspend fun checkGithubUpdate(): Result<GithubReleaseInfo?>
}

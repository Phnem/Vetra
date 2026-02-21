package com.example.myapplication.di

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.http.KtorHttpEngine
import com.example.myapplication.data.repository.AnimeRepository
import com.example.myapplication.network.AniListRemoteDataSource
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.VetroApiService
import com.example.myapplication.network.ShikimoriRemoteDataSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named

val networkModule = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(UserAgent) {
                agent = "VetroApp/1.0 (https://github.com/2004i/Vetro)"
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("Ktor", message)
                    }
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
            }
        }
    }
    single {
        ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .httpEngine(KtorHttpEngine(get<HttpClient>()))
            .build()
    }
    single { ShikimoriRemoteDataSource(get<HttpClient>()) }
    single { AniListRemoteDataSource(get<ApolloClient>()) }
    single<ApiService> {
        VetroApiService(
            httpClient = get<HttpClient>(),
            shikimori = get(),
            aniList = get()
        )
    }
    single<AnimeRepository> { AnimeRepository(apiService = get(), localDataSource = get()) }
}

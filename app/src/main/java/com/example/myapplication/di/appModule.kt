package com.example.myapplication.di

import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.utils.ApiRateLimiter
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named

val appModule = module {
    single<GenreRepository> { GenreRepository() }
    single<ApiRateLimiter> { ApiRateLimiter() }
}

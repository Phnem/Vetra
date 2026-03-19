package com.example.myapplication.di

import com.example.myapplication.data.repository.GenreRepository
import com.example.myapplication.domain.search.AddFromApiUseCase
import com.example.myapplication.notifications.AnimeNotifier
import com.example.myapplication.notifications.AnimeNotifierImpl
import com.example.myapplication.utils.ApiRateLimiter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named

val appModule = module {
    single<GenreRepository> { GenreRepository() }
    single { AddFromApiUseCase(get(), get(), get(), get()) }
    single<ApiRateLimiter> { ApiRateLimiter() }
    single<AnimeNotifier> { AnimeNotifierImpl(context = androidContext()) }
}

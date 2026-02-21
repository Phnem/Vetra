package com.example.myapplication.data.repository

import com.example.myapplication.data.models.*

class GenreRepository {
    val allGenres = listOf(
        GenreDefinition("Isekai", "Исекай", "Isekai", listOf(GenreCategory.ANIME)),
        GenreDefinition("Games", "Игры", "Games", listOf(GenreCategory.ANIME)),
        GenreDefinition("Martial Arts", "Боевые искусства", "Martial Arts", listOf(GenreCategory.ANIME)),
        GenreDefinition("Magic", "Магия", "Magic", listOf(GenreCategory.ANIME)),
        GenreDefinition("Mecha", "Мехи", "Mecha", listOf(GenreCategory.ANIME)),
        GenreDefinition("Shounen", "Сёнен", "Shounen", listOf(GenreCategory.ANIME)),
        GenreDefinition("Shoujo", "Сёдзё", "Shoujo", listOf(GenreCategory.ANIME)),
        GenreDefinition("Seinen", "Сэйнен", "Seinen", listOf(GenreCategory.ANIME)),
        GenreDefinition("Slice of Life", "Повседневность", "Slice of Life", listOf(GenreCategory.ANIME)),
        GenreDefinition("Ecchi", "Этти", "Ecchi", listOf(GenreCategory.ANIME)),
        GenreDefinition("Hentai", "Хентай", "Hentai", listOf(GenreCategory.ANIME)),
        GenreDefinition("School", "Школа", "School", listOf(GenreCategory.ANIME)),
        GenreDefinition("Detective", "Детектив", "Detective", listOf(GenreCategory.ANIME, GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Sports", "Спорт", "Sports", listOf(GenreCategory.ANIME, GenreCategory.MOVIE)),
        GenreDefinition("Action", "Экшен", "Action", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Adventure", "Приключения", "Adventure", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Comedy", "Комедия", "Comedy", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Drama", "Драма", "Drama", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Fantasy", "Фэнтези", "Fantasy", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Romance", "Романтика", "Romance", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Horror", "Ужасы", "Horror", listOf(GenreCategory.ANIME, GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Sci-Fi", "Фантастика", "Sci-Fi", listOf(GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Thriller", "Триллер", "Thriller", listOf(GenreCategory.MOVIE, GenreCategory.SERIES)),
        GenreDefinition("Mystery", "Мистика", "Mystery", listOf(GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Crime", "Криминал", "Crime", listOf(GenreCategory.SERIES, GenreCategory.MOVIE)),
        GenreDefinition("Western", "Вестерн", "Western", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Animation", "Анимация", "Animation", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Documentary", "Документальный", "Documentary", listOf(GenreCategory.MOVIE)),
        GenreDefinition("Sitcom", "Ситком", "Sitcom", listOf(GenreCategory.SERIES)),
        GenreDefinition("Reality", "Реалити-шоу", "Reality", listOf(GenreCategory.SERIES))
    )

    fun getGenresForCategory(category: GenreCategory): List<GenreDefinition> {
        return allGenres.filter { it.categories.contains(category) }.sortedBy { it.id }
    }

    fun getLabel(id: String, lang: com.example.myapplication.network.AppLanguage): String {
        val def = allGenres.find { it.id.equals(id, ignoreCase = true) }
        return when (lang) {
            com.example.myapplication.network.AppLanguage.RU -> def?.ru ?: id
            com.example.myapplication.network.AppLanguage.EN -> def?.en ?: id
        }
    }
}

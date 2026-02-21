package com.example.myapplication.data.models

import com.example.myapplication.network.AnimeDetails

sealed class DetailsState {
    object Idle : DetailsState()
    object Loading : DetailsState()
    data class Success(val details: AnimeDetails) : DetailsState()
    object Error : DetailsState()
}

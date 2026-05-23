package com.example.kinopoisk.domain.model

data class Movie(
    val id: Int = 0,
    val title: String,
    val year: String,
    val posterUrl: String,
    val imdbId: String,
    val type: String = ""
)
package com.example.kinopoisk.domain.repository

import com.example.kinopoisk.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getAllMovies(): Flow<List<Movie>>
    suspend fun addMovie(movie: Movie)
    suspend fun deleteMovies(movies: List<Movie>)
    suspend fun searchMovies(apiKey: String, query: String): List<Movie>
}
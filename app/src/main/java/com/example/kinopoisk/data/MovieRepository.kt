package com.example.kinopoisk.data

import kotlinx.coroutines.flow.Flow

class MovieRepository(
    private val api: OmdbApi,
    private val dao: MovieDao
) {
    // Работа с локальной БД
    val allMovies: Flow<List<MovieEntity>> = dao.getAllMovies()

    suspend fun addMovie(movie: MovieEntity) {
        dao.insertMovie(movie)
    }

    suspend fun deleteMovies(movies: List<MovieEntity>) {
        dao.deleteMovies(movies)
    }

    // Работа с API
    suspend fun searchMovies(apiKey: String, query: String): OmdbSearchResponse {
        return api.searchMovies(apiKey, query)
    }
}
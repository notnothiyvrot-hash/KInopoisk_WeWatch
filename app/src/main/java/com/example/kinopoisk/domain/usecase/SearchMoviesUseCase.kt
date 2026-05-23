package com.example.kinopoisk.domain.usecase

import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.repository.MovieRepository

class SearchMoviesUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke(apiKey: String, query: String): List<Movie> = 
        repository.searchMovies(apiKey, query)
}
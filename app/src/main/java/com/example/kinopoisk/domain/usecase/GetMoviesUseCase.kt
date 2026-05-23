package com.example.kinopoisk.domain.usecase

import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow

class GetMoviesUseCase(private val repository: MovieRepository) {
    operator fun invoke(): Flow<List<Movie>> = repository.getAllMovies()
}
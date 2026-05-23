package com.example.kinopoisk.domain.usecase

import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.repository.MovieRepository

class DeleteMoviesUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke(movies: List<Movie>) = repository.deleteMovies(movies)
}
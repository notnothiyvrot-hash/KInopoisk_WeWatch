package com.example.kinopoisk.domain.usecase

import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.repository.MovieRepository

class AddMovieUseCase(private val repository: MovieRepository) {
    suspend operator fun invoke(movie: Movie) = repository.addMovie(movie)
}
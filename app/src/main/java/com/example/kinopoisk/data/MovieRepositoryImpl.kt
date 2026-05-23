package com.example.kinopoisk.data

import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MovieRepositoryImpl(
    private val api: OmdbApi,
    private val dao: MovieDao
) : MovieRepository {

    override fun getAllMovies(): Flow<List<Movie>> {
        return dao.getAllMovies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addMovie(movie: Movie) {
        dao.insertMovie(movie.toEntity())
    }

    override suspend fun deleteMovies(movies: List<Movie>) {
        dao.deleteMovies(movies.map { it.toEntity() })
    }

    override suspend fun searchMovies(apiKey: String, query: String): List<Movie> {
        val response = api.searchMovies(apiKey, query)
        return if (response.response == "True") {
            response.search?.map { it.toDomain() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    // Mappers
    private fun MovieEntity.toDomain() = Movie(
        id = id,
        title = title,
        year = year,
        posterUrl = posterUrl,
        imdbId = imdbId
    )

    private fun Movie.toEntity() = MovieEntity(
        id = id,
        title = title,
        year = year,
        posterUrl = posterUrl,
        imdbId = imdbId
    )

    private fun OmdbMovieDto.toDomain() = Movie(
        title = title,
        year = year,
        posterUrl = poster,
        imdbId = imdbID,
        type = type
    )
}
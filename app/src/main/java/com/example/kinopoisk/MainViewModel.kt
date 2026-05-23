package com.example.kinopoisk

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinopoisk.data.AppContainer
import com.example.kinopoisk.data.MovieEntity
import com.example.kinopoisk.data.MovieRepository
import com.example.kinopoisk.data.OmdbMovieDto
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MovieRepository = AppContainer.movieRepository
) : ViewModel() {

    val movies = repository.allMovies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moviesToDelete = mutableStateListOf<MovieEntity>()

    var isSearching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    private val _searchResults = mutableStateListOf<OmdbMovieDto>()
    val searchResults: List<OmdbMovieDto> get() = _searchResults

    fun toggleSelection(movie: MovieEntity, isSelected: Boolean) {
        if (isSelected) moviesToDelete.add(movie) else moviesToDelete.remove(movie)
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteMovies(moviesToDelete.toList())
            moviesToDelete.clear()
        }
    }

    fun addMovie(title: String, year: String, poster: String, imdbId: String) {
        viewModelScope.launch {
            repository.addMovie(
                MovieEntity(title = title, year = year, posterUrl = poster, imdbId = imdbId)
            )
        }
    }

    fun searchOnline(query: String, apiKey: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            isSearching = true
            searchError = null
            try {
                val response = repository.searchMovies(apiKey, query)
                _searchResults.clear()
                if (response.response == "True") {
                    response.search?.let { _searchResults.addAll(it) }
                } else {
                    searchError = response.error ?: "Фильмы не найдены"
                }
            } catch (e: Exception) {
                searchError = "Ошибка сети: ${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                isSearching = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.clear()
        searchError = null
    }
}
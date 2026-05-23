package com.example.kinopoisk

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinopoisk.data.AppContainer
import com.example.kinopoisk.data.MovieEntity
import com.example.kinopoisk.data.MovieRepository
import com.example.kinopoisk.data.OmdbMovieDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- MVI CONTRACT ---

@Immutable
data class MainState(
    val movies: List<MovieEntity> = emptyList(),
    val searchResults: List<OmdbMovieDto> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedMovies: Set<MovieEntity> = emptySet()
)

sealed class MainIntent {
    data class SearchMovies(val query: String, val apiKey: String) : MainIntent()
    data class AddMovie(val title: String, val year: String, val poster: String, val imdbId: String) : MainIntent()
    data class ToggleSelection(val movie: MovieEntity) : MainIntent()
    object DeleteSelected : MainIntent()
    object ClearSearch : MainIntent()
}

class MainViewModel(
    private val repository: MovieRepository = AppContainer.movieRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        // Наблюдаем за базой данных и обновляем состояние
        viewModelScope.launch {
            repository.allMovies.collect { movies ->
                _state.update { it.copy(movies = movies) }
            }
        }
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SearchMovies -> searchOnline(intent.query, intent.apiKey)
            is MainIntent.AddMovie -> addMovie(intent.title, intent.year, intent.poster, intent.imdbId)
            is MainIntent.ToggleSelection -> toggleSelection(intent.movie)
            is MainIntent.DeleteSelected -> deleteSelected()
            is MainIntent.ClearSearch -> clearSearch()
        }
    }

    private fun searchOnline(query: String, apiKey: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null) }
            try {
                val response = repository.searchMovies(apiKey, query)
                if (response.response == "True") {
                    _state.update { it.copy(searchResults = response.search ?: emptyList(), isSearching = false) }
                } else {
                    _state.update { it.copy(searchError = response.error, isSearching = false, searchResults = emptyList()) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(searchError = "Ошибка сети", isSearching = false) }
            }
        }
    }

    private fun addMovie(title: String, year: String, poster: String, imdbId: String) {
        viewModelScope.launch {
            repository.addMovie(MovieEntity(title = title, year = year, posterUrl = poster, imdbId = imdbId))
        }
    }

    private fun toggleSelection(movie: MovieEntity) {
        _state.update { currentState ->
            val newSelection = currentState.selectedMovies.toMutableSet()
            if (newSelection.contains(movie)) newSelection.remove(movie) else newSelection.add(movie)
            currentState.copy(selectedMovies = newSelection)
        }
    }

    private fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteMovies(_state.value.selectedMovies.toList())
            _state.update { it.copy(selectedMovies = emptySet()) }
        }
    }

    private fun clearSearch() {
        _state.update { it.copy(searchResults = emptyList(), searchError = null) }
    }
}
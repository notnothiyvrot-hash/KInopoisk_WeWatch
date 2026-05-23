package com.example.kinopoisk

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kinopoisk.data.AppContainer
import com.example.kinopoisk.data.OmdbMovieDto
import com.example.kinopoisk.domain.model.Movie
import com.example.kinopoisk.domain.usecase.AddMovieUseCase
import com.example.kinopoisk.domain.usecase.DeleteMoviesUseCase
import com.example.kinopoisk.domain.usecase.GetMoviesUseCase
import com.example.kinopoisk.domain.usecase.SearchMoviesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- MVI CONTRACT ---

@Immutable
data class MainState(
    val movies: List<Movie> = emptyList(),
    val searchResults: List<Movie> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val selectedMovies: Set<Movie> = emptySet()
)

sealed class MainIntent {
    data class SearchMovies(val query: String, val apiKey: String) : MainIntent()
    data class AddMovie(val movie: Movie) : MainIntent()
    data class ToggleSelection(val movie: Movie) : MainIntent()
    object DeleteSelected : MainIntent()
    object ClearSearch : MainIntent()
}

class MainViewModel(
    private val getMoviesUseCase: GetMoviesUseCase = AppContainer.getMoviesUseCase,
    private val addMovieUseCase: AddMovieUseCase = AppContainer.addMovieUseCase,
    private val deleteMoviesUseCase: DeleteMoviesUseCase = AppContainer.deleteMoviesUseCase,
    private val searchMoviesUseCase: SearchMoviesUseCase = AppContainer.searchMoviesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getMoviesUseCase().collect { movies ->
                _state.update { it.copy(movies = movies) }
            }
        }
    }

    fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.SearchMovies -> searchOnline(intent.query, intent.apiKey)
            is MainIntent.AddMovie -> addMovie(intent.movie)
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
                val results = searchMoviesUseCase(apiKey, query)
                if (results.isNotEmpty()) {
                    _state.update { it.copy(searchResults = results, isSearching = false) }
                } else {
                    _state.update { it.copy(searchError = "Фильмы не найдены", isSearching = false, searchResults = emptyList()) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(searchError = "Ошибка сети", isSearching = false) }
            }
        }
    }

    private fun addMovie(movie: Movie) {
        viewModelScope.launch {
            addMovieUseCase(movie)
        }
    }

    private fun toggleSelection(movie: Movie) {
        _state.update { currentState ->
            val newSelection = currentState.selectedMovies.toMutableSet()
            if (newSelection.contains(movie)) newSelection.remove(movie) else newSelection.add(movie)
            currentState.copy(selectedMovies = newSelection)
        }
    }

    private fun deleteSelected() {
        viewModelScope.launch {
            deleteMoviesUseCase(_state.value.selectedMovies.toList())
            _state.update { it.copy(selectedMovies = emptySet()) }
        }
    }

    private fun clearSearch() {
        _state.update { it.copy(searchResults = emptyList(), searchError = null) }
    }
}
package com.example.kinopoisk

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kinopoisk.data.MovieEntity

const val API_KEY = "12ba6e9d"

// --- 1. MAIN SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel,
    state: MainState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои фильмы") },
                actions = {
                    IconButton(onClick = { viewModel.handleIntent(MainIntent.DeleteSelected) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить выбранные")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_screen") }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { padding ->
        val movies = state.movies
        if (movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.Gray)
                    Text("Список фильмов пуст", style = MaterialTheme.typography.headlineSmall)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(movies) { movie ->
                    val isSelected = state.selectedMovies.contains(movie)
                    MovieItem(
                        movie = movie, 
                        isSelected = isSelected,
                        onToggleSelection = { viewModel.handleIntent(MainIntent.ToggleSelection(movie)) }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieItem(
    movie: MovieEntity, 
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            
            if (movie.posterUrl == "N/A" || movie.posterUrl.isEmpty()) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).padding(8.dp),
                    tint = Color.Gray
                )
            } else {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(text = movie.title, style = MaterialTheme.typography.titleMedium)
                Text(text = movie.year, style = MaterialTheme.typography.bodyMedium)
            }
            
            if (movie.imdbId.isNotEmpty()) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.imdb.com/title/${movie.imdbId}/"))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Watch", tint = Color.Green)
                }
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        }
    }
}

// --- 2. ADD SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(navController: NavController, viewModel: MainViewModel, state: MainState) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var imdbId by remember { mutableStateOf("") }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val selectedTitle = savedStateHandle?.get<String>("title")
    val selectedYear = savedStateHandle?.get<String>("year")
    val selectedPoster = savedStateHandle?.get<String>("poster")
    val selectedImdbId = savedStateHandle?.get<String>("imdbId")

    LaunchedEffect(selectedTitle) {
        if (selectedTitle != null) {
            title = selectedTitle
            year = selectedYear ?: ""
            posterUrl = selectedPoster ?: ""
            imdbId = selectedImdbId ?: ""
            savedStateHandle?.remove<String>("title")
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Добавить фильм") }) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (на английском)") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (title.isNotBlank()) {
                        val encodedQuery = Uri.encode(title)
                        navController.navigate("search_screen/$encodedQuery")
                    }
                }) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = year,
                onValueChange = { year = it },
                label = { Text("Год") },
                modifier = Modifier.fillMaxWidth()
            )

            if (posterUrl.isNotEmpty() && posterUrl != "N/A") {
                Spacer(modifier = Modifier.height(16.dp))
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "Poster",
                    modifier = Modifier.height(200.dp).align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.handleIntent(MainIntent.AddMovie(title, year, posterUrl, imdbId))
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить фильм")
            }
        }
    }
}

// --- 3. SEARCH SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: MainViewModel,
    state: MainState,
    query: String
) {
    LaunchedEffect(query) {
        viewModel.handleIntent(MainIntent.SearchMovies(query, API_KEY))
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Результаты поиска") },
                navigationIcon = {
                    TextButton(onClick = { 
                        viewModel.handleIntent(MainIntent.ClearSearch)
                        navController.popBackStack() 
                    }) {
                        Text("Назад")
                    }
                }
            ) 
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isSearching) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.searchError != null) {
                Text(
                    text = state.searchError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.searchResults) { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    navController.previousBackStackEntry?.savedStateHandle?.set("title", movie.title)
                                    navController.previousBackStackEntry?.savedStateHandle?.set("year", movie.year)
                                    navController.previousBackStackEntry?.savedStateHandle?.set("poster", movie.poster)
                                    navController.previousBackStackEntry?.savedStateHandle?.set("imdbId", movie.imdbID)
                                    viewModel.handleIntent(MainIntent.ClearSearch)
                                    navController.popBackStack()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (movie.poster == "N/A") {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp).padding(8.dp),
                                    tint = Color.Gray
                                )
                            } else {
                                AsyncImage(
                                    model = movie.poster,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(text = movie.title, style = MaterialTheme.typography.titleMedium)
                                Text(text = "${movie.year} | ${movie.type}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
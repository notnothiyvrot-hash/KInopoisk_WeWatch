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
import com.example.kinopoisk.domain.model.Movie

const val API_KEY = "12ba6e9d"

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
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
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
        if (state.movies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.Gray)
                    Text("Список пуст", style = MaterialTheme.typography.headlineSmall)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(state.movies) { movie ->
                    MovieItem(
                        movie = movie,
                        isSelected = state.selectedMovies.contains(movie),
                        onToggleSelection = { viewModel.handleIntent(MainIntent.ToggleSelection(movie)) }
                    )
                }
            }
        }
    }
}

@Composable
fun MovieItem(movie: Movie, isSelected: Boolean, onToggleSelection: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (movie.posterUrl == "N/A" || movie.posterUrl.isEmpty()) {
                Icon(Icons.Default.Movie, null, modifier = Modifier.size(60.dp).padding(8.dp), tint = Color.Gray)
            } else {
                AsyncImage(movie.posterUrl, null, modifier = Modifier.size(60.dp), contentScale = ContentScale.Crop)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(movie.title, style = MaterialTheme.typography.titleMedium)
                Text(movie.year, style = MaterialTheme.typography.bodyMedium)
            }
            if (movie.imdbId.isNotEmpty()) {
                IconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.imdb.com/title/${movie.imdbId}/")))
                }) {
                    Icon(Icons.Default.PlayArrow, "Watch", tint = Color.Green)
                }
            }
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(navController: NavController, viewModel: MainViewModel, state: MainState) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var posterUrl by remember { mutableStateOf("") }
    var imdbId by remember { mutableStateOf("") }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(Unit) {
        savedStateHandle?.get<String>("title")?.let { title = it }
        savedStateHandle?.get<String>("year")?.let { year = it }
        savedStateHandle?.get<String>("poster")?.let { posterUrl = it }
        savedStateHandle?.get<String>("imdbId")?.let { imdbId = it }
        savedStateHandle?.remove<String>("title")
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Добавить фильм") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (title.isNotBlank()) navController.navigate("search_screen/${Uri.encode(title)}") }) {
                    Icon(Icons.Default.Search, "Поиск")
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(year, { year = it }, label = { Text("Год") }, modifier = Modifier.fillMaxWidth())
            if (posterUrl.isNotEmpty() && posterUrl != "N/A") {
                AsyncImage(posterUrl, null, modifier = Modifier.height(200.dp).align(Alignment.CenterHorizontally))
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                if (title.isNotBlank()) {
                    viewModel.handleIntent(MainIntent.AddMovie(Movie(title = title, year = year, posterUrl = posterUrl, imdbId = imdbId)))
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: MainViewModel, state: MainState, query: String) {
    LaunchedEffect(query) { viewModel.handleIntent(MainIntent.SearchMovies(query, API_KEY)) }
    Scaffold(topBar = { 
        TopAppBar(title = { Text("Поиск") }, navigationIcon = {
            TextButton(onClick = { viewModel.handleIntent(MainIntent.ClearSearch); navController.popBackStack() }) { Text("Назад") }
        }) 
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isSearching) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else if (state.searchError != null) Text(state.searchError, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            else LazyColumn {
                items(state.searchResults) { movie ->
                    Row(Modifier.fillMaxWidth().padding(8.dp).clickable {
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set("title", movie.title); set("year", movie.year); set("poster", movie.posterUrl); set("imdbId", movie.imdbId)
                        }
                        viewModel.handleIntent(MainIntent.ClearSearch); navController.popBackStack()
                    }, verticalAlignment = Alignment.CenterVertically) {
                        if (movie.posterUrl == "N/A") Icon(Icons.Default.Movie, null, Modifier.size(60.dp).padding(8.dp), Color.Gray)
                        else AsyncImage(movie.posterUrl, null, Modifier.size(60.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(movie.title, style = MaterialTheme.typography.titleMedium)
                            Text("${movie.year} | ${movie.type}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
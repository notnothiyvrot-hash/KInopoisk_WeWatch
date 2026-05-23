package com.example.kinopoisk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kinopoisk.data.AppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация БД
        AppContainer.init(applicationContext)

        setContent {
            val navController = rememberNavController()
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            NavHost(navController = navController, startDestination = "main_screen") {

                // Экран 1: Список
                composable("main_screen") {
                    MainScreen(navController, viewModel, state)
                }

                // Экран 2: Добавление
                composable("add_screen") {
                    AddScreen(navController, viewModel, state)
                }

                // Экран 3: Поиск
                composable(
                    route = "search_screen/{query}",
                    arguments = listOf(navArgument("query") { type = NavType.StringType })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("query") ?: ""
                    SearchScreen(navController, viewModel, state, query)
                }
            }
        }
    }
}
// ui/navigation/YumeReaderNavigation.kt (обновляем)
package com.yume.reader.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yume.reader.ui.screens.*
import com.yume.reader.ui.screens.BookDetailsScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun YumeReaderNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showBottomBar by rememberSaveable { mutableStateOf(true) }

    // Определяем, нужно ли показывать нижнюю панель навигации
    showBottomBar = when (currentRoute) {
        "library", "reading_now", "favorites", "globSettings" -> true
        else -> false
    }

    // Используем Scaffold с BottomNavigation
    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                com.yume.reader.ui.components.BottomNavigationBar(
                    navController = navController,
                    currentDestination = navBackStackEntry?.destination
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "library",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Главные экраны
            composable("library") {
                LibraryScreen(navController = navController)
            }
            composable("reading_now") {
                ReadingNowScreen(navController = navController)
            }
            composable("favorites") {
                FavoritesScreen(navController = navController)
            }
            composable("globSettings") {
                SettingsScreen(navController = navController)
            }

            composable(
                "reader/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                val chapter = backStackEntry.arguments?.getString("chapter")?.toIntOrNull()
                ReadingScreen(
                    navController = navController,
                    bookId = bookId,
                    chapter = chapter
                )
            }

            // ДОБАВЛЯЕМ МАРШРУТ ДЛЯ ДЕТАЛЕЙ КНИГИ
            composable(
                "book_details/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
                BookDetailsScreen(
                    navController = navController,
                    bookId = bookId
                )
            }

            composable("import_epub") {
                ImportEpubScreen(navController = navController)
            }
        }
    }
}
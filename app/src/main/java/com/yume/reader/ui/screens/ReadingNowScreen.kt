package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yume.reader.ui.components.BookCard
import com.yume.reader.ui.viewmodels.BookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingNowScreen(
    navController: NavController,
    viewModel: BookViewModel = hiltViewModel()
) {
    // Получаем книги в процессе чтения из ViewModel
    val readingBooks by viewModel.readingBooks.collectAsState()

    // Состояния для поиска
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Фильтруем по поисковому запросу, если есть
    val filteredBooks = if (searchQuery.isNotBlank()) {
        readingBooks.filter { book ->
            book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)
        }
    } else {
        readingBooks
    }

    // Функция для форматирования числа книг
    fun formatBookCount(count: Int): String {
        val lastDigit = count % 10
        val lastTwoDigits = count % 100

        return when {
            lastTwoDigits in 11..14 -> "$count книг"
            lastDigit == 1 -> "$count книга"
            lastDigit in 2..4 -> "$count книги"
            else -> "$count книг"
        }
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            showSearchBar = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Поиск по читаемым книгам...") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            shape = MaterialTheme.shapes.large,
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Читаю сейчас",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatBookCount(readingBooks.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    },
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Навигация на экран добавления книги
                    navController.navigate("import_epub")
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить книгу"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Список книг в процессе чтения
            if (filteredBooks.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBooks) { book ->
                        BookCard(
                            book = book,
                            onReadClick = {
                                navController.navigate("reader/${book.id}")
                            },
                            onDetailsClick = {
                                navController.navigate("book_details/${book.id}")
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(book.id, !book.isFavorite)
                            }
                        )
                    }

                    // Добавляем отступ внизу для FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> "По запросу '$searchQuery' ничего не найдено"
                            else -> "Нет активных книг"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> "Попробуйте другой запрос"
                            else -> "Начните читать книгу из библиотеки"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
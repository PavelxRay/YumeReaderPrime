package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
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
fun FavoritesScreen(
    navController: NavController,
    viewModel: BookViewModel = hiltViewModel()
) {
    // Получаем данные из ViewModel
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()

    // Состояния
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Все") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedBookId by remember { mutableLongStateOf(-1L) }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedBookId = -1L
            },
            title = { Text("Удалить книгу") },
            text = {
                Text("Вы уверены, что хотите удалить эту книгу? Это действие нельзя отменить. Все данные книги будут удалены.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedBookId != -1L) {
                            viewModel.deleteBook(selectedBookId)
                        }
                        showDeleteDialog = false
                        selectedBookId = -1L
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedBookId = -1L
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Фильтры
    val filters = listOf("Все", "Читаю", "Прочитано")

    // Фильтруем книги
    val filteredBooks = remember(favoriteBooks, selectedFilter, searchQuery) {
        val byStatus = when (selectedFilter) {
            "Читаю" -> favoriteBooks.filter { it.isReading }
            "Прочитано" -> favoriteBooks.filter { it.isFinished }
            else -> favoriteBooks
        }

        if (searchQuery.isNotBlank()) {
            byStatus.filter { book ->
                book.title.contains(searchQuery, ignoreCase = true) ||
                        book.author.contains(searchQuery, ignoreCase = true)
            }
        } else {
            byStatus
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
                            placeholder = { Text("Поиск в избранном...") },
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
                                text = "Избранное",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${favoriteBooks.size} книг в избранном",
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Фильтры (только если не ведем поиск)
            if (searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            leadingIcon = if (filter == "Все") null else {
                                {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Список избранных книг
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
                            },
                            // ДОБАВИТЬ ПАРАМЕТР onLongClick
                            onLongClick = {
                                selectedBookId = book.id
                                showDeleteDialog = true
                            }
                        )
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
                            selectedFilter != "Все" -> "Нет книг с выбранным фильтром"
                            else -> "Нет избранных книг"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> "Попробуйте другой запрос"
                            selectedFilter != "Все" -> "Попробуйте другой фильтр"
                            else -> "Добавьте книги в избранное из библиотеки"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
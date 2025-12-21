package com.yume.reader.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yume.reader.ui.components.BookCard
import com.yume.reader.ui.viewmodels.BookViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: BookViewModel = hiltViewModel()
) {
    // Получаем данные из ViewModel
    val books by viewModel.books.collectAsState()
    val readingBooks by viewModel.readingBooks.collectAsState()
    val favoriteBooks by viewModel.favoriteBooks.collectAsState()

    // Состояния
    var showSearchBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
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

    // Исправленная статистика - автоматически обновляется при изменении книг
    val statistics = remember(books) {
        val finishedBooks = books.count { it.isFinished }
        val readingBooksCount = books.count { it.isReading && !it.isFinished }
        val totalChapters = books.sumOf { book ->
            // Если книга завершена, считаем все главы
            if (book.isFinished) book.totalPages
            // Иначе считаем только прочитанные главы
            else book.currentPage
        }
        mapOf(
            "finishedBooks" to finishedBooks,
            "readingBooks" to readingBooksCount,
            "totalChapters" to totalChapters
        )
    }

    // Функция для форматирования чисел книг
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

    // Фильтруем по поисковому запросу, если есть
    val filteredBooks = if (searchQuery.isNotBlank()) {
        books.filter { book ->
            book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true)
        }
    } else {
        books
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
                            placeholder = { Text("Поиск по библиотеке...") },
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
                                text = "Моя библиотека",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            // Всегда показываем статистику в заголовке
                            Text(
                                text = "${formatBookCount(books.size)}, ${readingBooks.size} в процессе",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                onClick = { navController.navigate("import_epub") },
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Показываем статистику, если не ведем поиск
            if (searchQuery.isBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Статистика: прочитано книг
                    StatisticCard(
                        title = "Прочитано книг",
                        value = (statistics["finishedBooks"] as? Int ?: 0).toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Статистика: читаю сейчас
                    StatisticCard(
                        title = "Читаю сейчас",
                        value = (statistics["readingBooks"] as? Int ?: 0).toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Статистика: глав прочитано
                    StatisticCard(
                        title = "Прочитано глав",
                        value = (statistics["totalChapters"] as? Int ?: 0).toString(),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Список книг
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
                            onLongClick = {
                                selectedBookId = book.id
                                showDeleteDialog = true
                            }
                        )
                    }

                    // Добавляем отступ внизу для FAB
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // Сообщение, если книг нет
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Пустая библиотека",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            searchQuery.isNotBlank() -> "По запросу '$searchQuery' ничего не найдено"
                            else -> "Библиотека пуста"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (books.isEmpty()) {
                        Text(
                            text = "Добавьте книги с помощью кнопки ниже",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = isSystemInDarkTheme()

    // Для темной темы используем более светлый и контрастный фон
    val backgroundColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    // Для темной темы делаем круг более контрастным
    val circleBackgroundAlpha = if (isDarkTheme) 0.25f else 0.2f
    val circleBackgroundColor = if (isDarkTheme) {
        color.copy(alpha = circleBackgroundAlpha)
    } else {
        color.copy(alpha = circleBackgroundAlpha)
    }

    // Для текста в круге в темной теме используем более яркую версию цвета
    val circleTextColor = if (isDarkTheme) {
        // Делаем цвет ярче для темной темы
        when (color) {
            MaterialTheme.colorScheme.primary -> MaterialTheme.colorScheme.primary.copy(alpha = 1f)
            MaterialTheme.colorScheme.secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 1f)
            MaterialTheme.colorScheme.tertiary -> MaterialTheme.colorScheme.tertiary.copy(alpha = 1f)
            else -> color
        }
    } else {
        color
    }

    // Цвет заголовка
    val titleColor = if (isDarkTheme) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 1.1f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDarkTheme) 3.dp else 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // Увеличил размер круга для лучшей видимости
                    .clip(CircleShape)
                    .background(circleBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = circleTextColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) // Увеличил отступ
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isDarkTheme) FontWeight.Medium else FontWeight.Normal
                ),
                color = titleColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
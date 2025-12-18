package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yume.reader.ui.viewmodels.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    navController: NavController,
    bookId: Long,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    // Состояние загрузки
    var isLoading by remember { mutableStateOf(true) }
    var showEmptyState by remember { mutableStateOf(false) }

    // Инициализируем ViewModel с bookId
    LaunchedEffect(bookId) {
        viewModel.setBookId(bookId)
        isLoading = true
        showEmptyState = false
    }

    val chapters by viewModel.chapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapter.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()

    // Получаем текущую главу
    val currentChapter = remember(chapters, currentChapterIndex) {
        chapters.getOrNull(currentChapterIndex - 1)
    }

    // Когда главы загрузились
    LaunchedEffect(chapters) {
        if (chapters.isNotEmpty()) {
            isLoading = false
            showEmptyState = chapters.all { it.content.isBlank() }
        } else if (!isLoading) {
            showEmptyState = true
        }
    }

    // Показать состояние загрузки
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Загрузка книги...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    // Показать пустое состояние
    if (showEmptyState || chapters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Книга",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Книга не найдена или повреждена",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Попробуйте переимпортировать книгу",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text(text = "Вернуться к библиотеке")
                }
            }
        }
        return
    }

    // Проверить, что текущая глава существует и имеет контент
    val validChapter = if (currentChapter == null || currentChapter.content.isBlank()) {
        // Найти первую главу с контентом
        chapters.firstOrNull { it.content.isNotBlank() } ?: chapters.firstOrNull()
    } else {
        currentChapter
    }

    val chapterContent = remember(validChapter) {
        val content = validChapter?.content ?: ""
        if (content.isEmpty()) {
            emptyList()
        } else {
            // Разбиваем на абзацы, фильтруем короткие строки
            content.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.length > 3 }
                .filterNot { it.matches(Regex("^[\\d\\s.:/-]+$")) } // Фильтруем номера страниц
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = validChapter?.title ?: "Без названия",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Индикатор прогресса
                    Text(
                        text = "${(readingProgress * 100).toInt()}%",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousChapter() },
                        enabled = currentChapterIndex > 1
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущая глава")
                    }

                    // Навигация по главам
                    Text(
                        text = "$currentChapterIndex/${chapters.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(
                        onClick = { viewModel.nextChapter() },
                        enabled = currentChapterIndex < chapters.size
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Следующая глава")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (chapterContent.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Текст главы пуст",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Попробуйте перейти к следующей главе",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(chapterContent) { paragraph ->
                        Text(
                            text = paragraph,
                            modifier = Modifier.padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
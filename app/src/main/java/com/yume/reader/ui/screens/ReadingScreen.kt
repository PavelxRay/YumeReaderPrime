package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
    LaunchedEffect(bookId) {
        viewModel.setBookId(bookId)
    }

    val chapters by viewModel.chapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapter.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()

    val currentChapter = remember(chapters, currentChapterIndex) {
        chapters.getOrNull(currentChapterIndex - 1)
    }

    val chapterContent = remember(currentChapter) {
        val content = currentChapter?.content ?: ""
        if (content.isEmpty()) {
            listOf(
                "❌ Текст главы не найден.",
                "📖 Заголовок: ${currentChapter?.title ?: "Неизвестно"}",
                "🔢 Номер главы: $currentChapterIndex",
                "📊 Всего глав: ${chapters.size}",
                "💾 Длина текста в БД: ${currentChapter?.content?.length ?: 0} символов",
                "",
                "🔄 Пожалуйста, переимпортируйте книгу с обновленным парсером.",
                "👉 Или попробуйте перейти к следующей главе."
            )
        } else {
            // Просто разбиваем на абзацы без фильтрации
            content.split("\n\n", "\n").filter { it.trim().isNotBlank() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentChapter?.title ?: "Загрузка...",
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
            // Отладочная информация
            if (currentChapter?.content.isNullOrEmpty()) {
                Text(
                    text = "⚠️ Отладка: Глава ${currentChapterIndex}/${chapters.size}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp
                )
            }

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
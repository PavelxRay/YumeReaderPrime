package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val chapters by viewModel.chapters.collectAsState()
    val currentChapterText by viewModel.currentChapterText.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Глава $currentChapter",
                        style = MaterialTheme.typography.titleMedium
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
                        enabled = currentChapter > 1
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Предыдущая глава")
                    }

                    // Навигация по главам
                    Text(
                        text = "$currentChapter/${chapters.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    IconButton(
                        onClick = { viewModel.nextChapter() },
                        enabled = currentChapter < chapters.size
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
            // Текст главы
            Text(
                text = currentChapterText,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
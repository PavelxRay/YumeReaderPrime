// ui/screens/BookDetailsScreen.kt
package com.yume.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yume.reader.ui.components.ChapterItem
import com.yume.reader.ui.viewmodels.BookDetailsViewModel
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsScreen(
    navController: NavController,
    bookId: Long,
    viewModel: BookDetailsViewModel = hiltViewModel()
) {
    // Инициализируем ViewModel
    LaunchedEffect(bookId) {
        viewModel.setBookId(bookId)
    }

    // Получаем данные
    val book by viewModel.book.collectAsState()
    val chapters by viewModel.filteredChapters.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val currentChapter by viewModel.currentChapter.collectAsState()
    val totalReadingTime by remember { derivedStateOf { viewModel.getTotalReadingTime() } }

    // Состояния
    var showSearchBar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchTopBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onBackClick = {
                        showSearchBar = false
                        viewModel.updateSearchQuery("")
                    },
                    onClearClick = { viewModel.updateSearchQuery("") }
                )
            } else {
                DetailsTopBar(
                    book = book,
                    onBackClick = { navController.popBackStack() },
                    onSearchClick = { showSearchBar = true },
                    onFavoriteClick = { viewModel.toggleFavorite() }
                )
            }
        }
    ) { paddingValues ->
        if (book == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Шапка с информацией о книге
            BookHeader(
                book = book!!,
                totalChapters = chapters.size,
                totalReadingTime = totalReadingTime,
                progress = readingProgress?.progressPercent ?: 0f,
                onReadClick = {
                    navController.navigate("reader/${book!!.id}")
                },
                onContinueClick = {
                    // Продолжаем с сохраненной главы
                    val continueChapter = readingProgress?.currentChapter ?: 1
                    navController.navigate("reader/${book!!.id}?chapter=$continueChapter")
                }
            )

            // Панель фильтров и поиска
            FilterSection(
                selectedFilter = selectedFilter,
                onFilterChange = { viewModel.updateFilter(it) },
                showSearchButton = !showSearchBar && searchQuery.isEmpty(),
                onSearchClick = { showSearchBar = true }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            // Список глав
            if (chapters.isNotEmpty()) {
                ChaptersList(
                    chapters = chapters,
                    currentChapter = currentChapter,
                    onChapterClick = { chapter ->
                        // Теперь точно переходим к указанной главе
                        navController.navigate("reader/${book!!.id}?chapter=${chapter.chapterNumber}")
                    },
                    onToggleRead = { chapterId, isRead ->
                        viewModel.toggleChapterReadStatus(chapterId, !isRead)
                    }
                )
            } else {
                EmptyChaptersState(
                    modifier = Modifier.weight(1f),
                    searchQuery = searchQuery
                )
            }

            // Пагинация (если глав много)
            if (chapters.size > 10) {
                PaginationBar(
                    totalChapters = chapters.size,
                    onPageClick = { pageNumber ->
                        // Можно реализовать скролл к определенной странице
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    book: com.yume.reader.domain.models.Book?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = book?.title ?: "Загрузка...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Поиск")
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (book?.isFavorite == true) Icons.Filled.Favorite
                    else Icons.Default.FavoriteBorder,
                    contentDescription = if (book?.isFavorite == true) "Убрать из избранного"
                    else "Добавить в избранное",
                    tint = if (book?.isFavorite == true) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun SearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
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
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Поиск по главам...") },
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
                        IconButton(onClick = onClearClick) {
                            Icon(Icons.Default.Close, contentDescription = "Очистить")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun BookHeader(
    book: com.yume.reader.domain.models.Book,
    totalChapters: Int,
    totalReadingTime: String,
    progress: Float,
    onReadClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Название и автор
        Column {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Прогресс чтения
        if (progress > 0) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Прогресс",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // Статистика
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatItem(
                value = "$totalChapters глав",
                label = "Всего глав",
                modifier = Modifier.weight(1f)
            )
            StatItem(
                value = totalReadingTime,
                label = "Время чтения",
                modifier = Modifier.weight(1f)
            )
        }

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onContinueClick,
                modifier = Modifier.weight(1f),
                enabled = progress > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Продолжить чтение")
            }
            Button(
                onClick = onReadClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Начать с начала")
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterSection(
    selectedFilter: String,
    onFilterChange: (String) -> Unit,
    showSearchButton: Boolean,
    onSearchClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Фильтры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("По порядку", "Прочитанные", "Непрочитанные")

            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (showSearchButton) {
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            }
        }
    }
}

@Composable
private fun ChaptersList(
    chapters: List<com.yume.reader.data.local.entity.ChapterEntity>,
    currentChapter: Int,
    onChapterClick: (com.yume.reader.data.local.entity.ChapterEntity) -> Unit,
    onToggleRead: (Long, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        itemsIndexed(chapters) { index, chapter ->
            ChapterItem(
                chapterNumber = chapter.chapterNumber,
                title = chapter.title,
                durationMinutes = chapter.durationMinutes,
                isRead = chapter.isRead,
                isCurrent = chapter.chapterNumber == currentChapter,
                onChapterClick = { onChapterClick(chapter) },
                onToggleRead = { onToggleRead(chapter.id, chapter.isRead) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptyChaptersState(
    modifier: Modifier = Modifier,
    searchQuery: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = "Нет глав",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchQuery.isNotEmpty()) {
                "По запросу '$searchQuery' глав не найдено"
            } else {
                "В этой книге пока нет глав"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun PaginationBar(
    totalChapters: Int,
    onPageClick: (Int) -> Unit
) {
    val itemsPerPage = 10
    val totalPages = ceil(totalChapters.toDouble() / itemsPerPage).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Буквенные индикаторы
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "А-Я",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // Числовые индикаторы страниц
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (page in 1..totalPages) {
                val startChapter = (page - 1) * itemsPerPage + 1
                val endChapter = minOf(page * itemsPerPage, totalChapters)

                FilterChip(
                    selected = false,
                    onClick = { onPageClick(page) },
                    label = { Text("$startChapter-$endChapter") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
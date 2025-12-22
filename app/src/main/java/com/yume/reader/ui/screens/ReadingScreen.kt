package com.yume.reader.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.yume.reader.R
import com.yume.reader.data.models.TextSettings
import com.yume.reader.ui.viewmodels.ReadingViewModel
import kotlinx.coroutines.launch

// Модели для элементов контента
sealed class ContentItem {
    data class Text(val text: String) : ContentItem()
    data class Image(val imageUrl: String, val altText: String? = null) : ContentItem()
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    navController: NavController,
    bookId: Long,
    chapter: Int? = null,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    // Инициализируем ViewModel с bookId
    LaunchedEffect(bookId, chapter) {
        Log.d("ReadingScreen", "Инициализация экрана чтения, bookId: $bookId, chapter: $chapter")
        viewModel.setBookId(bookId)
        chapter?.let { viewModel.goToChapter(it) }
    }

    // Состояния
    var showSettings by remember { mutableStateOf(false) }
    var showProgressIndicator by remember { mutableStateOf(true) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    // Настройки текста
    val textSettings by viewModel.textSettings.collectAsState()

    // Данные из ViewModel
    val chapters by viewModel.chapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapter.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentContent by viewModel.currentContent.collectAsState()

    // Состояние для изображений
    var chapterImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var contentItems by remember { mutableStateOf<List<ContentItem>>(emptyList()) }

    val currentChapter = remember(chapters, currentChapterIndex) {
        chapters.getOrNull(currentChapterIndex - 1)
    }

    // Загружаем изображения при изменении главы
    LaunchedEffect(currentChapterIndex) {
        if (currentChapterIndex > 0) {
            try {
                Log.d("ReadingScreen", "Загрузка изображений для главы $currentChapterIndex")
                val (_, images) = viewModel.getChapterContentWithImages(currentChapterIndex)
                chapterImages = images
                Log.d("ReadingScreen", "Загружено ${images.size} изображений")
            } catch (e: Exception) {
                Log.e("ReadingScreen", "Ошибка загрузки изображений: ${e.message}")
                chapterImages = emptyList()
            }
        }
    }

    // Разбираем контент на элементы при изменении контента или изображений
    LaunchedEffect(currentContent, chapterImages) {
        val content = currentContent ?: ""
        contentItems = parseContentWithImages(content, chapterImages)
        Log.d("ReadingScreen", "Разобрано ${contentItems.size} элементов контента")
    }

    // Определяем фон в зависимости от темы
    val backgroundColor = when (textSettings.theme) {
        "dark" -> Color(0xFF121212)
        "sepia" -> Color(0xFFF8F0E3)
        "contrast" -> Color.Black
        else -> Color.White
    }

    val textColor = when (textSettings.theme) {
        "dark" -> Color.White
        "sepia" -> Color(0xFF5C4B37)
        "contrast" -> Color(0xFF00FF00)
        else -> Color.Black
    }

    val fontFamily = when (textSettings.fontFamily) {
        "Georgia" -> FontFamily.Serif
        "Arial" -> FontFamily.SansSerif
        "Courier" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    // Показать состояние загрузки
    if (isLoading || chapters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (chapters.isEmpty()) "Загрузка глав..." else "Загрузка главы...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Scaffold(
        topBar = {
            if (showProgressIndicator) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentChapter?.title ?: "Без названия",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = textColor
                            )
                            // Линейный индикатор прогресса
                            LinearProgressIndicator(
                                progress = { readingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .padding(top = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = textColor
                            )
                        }
                    },
                    actions = {
                        // Кнопка настроек
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor,
                        titleContentColor = textColor,
                        actionIconContentColor = textColor,
                        navigationIconContentColor = textColor
                    )
                )
            }
        },
        bottomBar = {
            if (showProgressIndicator) {
                BottomAppBar(
                    containerColor = backgroundColor
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousChapter() },
                            enabled = currentChapterIndex > 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Предыдущая глава",
                                tint = textColor
                            )
                        }

                        // Навигация по главам с прогрессом
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Глава $currentChapterIndex/${chapters.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            Text(
                                text = "Прогресс: ${(readingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.nextChapter() },
                            enabled = currentChapterIndex < chapters.size
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Следующая глава",
                                tint = textColor
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            lastTapPosition = offset
                            // При тапе по центру экрана - показать/скрыть UI
                            val screenWidth = size.width
                            val tapX = offset.x

                            if (tapX > screenWidth * 0.3 && tapX < screenWidth * 0.7) {
                                showProgressIndicator = !showProgressIndicator
                            }
                        },
                        onLongPress = {
                            // Долгое нажатие для быстрой настройки
                            showSettings = true
                        }
                    )
                }
        ) {
            // Основной контент с изображениями
            if (contentItems.isEmpty()) {
                // Состояние "пустая глава" или загрузка
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentContent == null) {
                        // Загрузка
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Загрузка главы...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                        }
                    } else {
                        // Пустая глава
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Текст главы отсутствует",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Попробуйте перейти к следующей главе",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = textSettings.margins.dp,
                            end = textSettings.margins.dp,
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding()
                        )
                ) {
                    items(contentItems.size) { index ->
                        val item = contentItems[index]
                        when (item) {
                            is ContentItem.Text -> {
                                // Разбиваем текст на параграфы для лучшего отображения
                                val paragraphs = item.text.split("\n\n", "\n")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() && it.length > 3 }
                                    .filterNot { it.matches(Regex("^[\\d\\s.:/-]+$")) }

                                Column {
                                    paragraphs.forEach { paragraph ->
                                        Text(
                                            text = paragraph,
                                            modifier = Modifier.padding(
                                                vertical = textSettings.paragraphSpacing.dp
                                            ),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = textSettings.fontSize.sp,
                                                lineHeight = (textSettings.fontSize * textSettings.lineHeight).sp,
                                                fontFamily = fontFamily
                                            ),
                                            color = textColor
                                        )
                                    }
                                }
                            }
                            is ContentItem.Image -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.altText ?: "Изображение из книги",
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = 0.9f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Fit,
                                        placeholder = rememberAsyncImagePainter(R.drawable.ic_image_placeholder)
                                    )
                                }
                            }
                        }
                    }
                }

                // Автосохранение прогресса при прокрутке
                LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                    if (lazyListState.isScrollInProgress) {
                        viewModel.saveReadingProgress()
                    }
                }
            }

            // Быстрое меню при долгом нажатии
            if (lastTapPosition != Offset.Zero && showSettings.not()) {
                QuickSettingsMenu(
                    position = lastTapPosition,
                    textSettings = textSettings,
                    onBrightnessChange = { brightness ->
                        viewModel.updateTextSettings(
                            textSettings.copy(brightness = brightness)
                        )
                    },
                    onFontSizeChange = { fontSize ->
                        viewModel.updateTextSettings(
                            textSettings.copy(fontSize = fontSize)
                        )
                    },
                    onDismiss = { lastTapPosition = Offset.Zero }
                )
            }

            // Диалог настроек
            if (showSettings) {
                Dialog(
                    onDismissRequest = { showSettings = false }
                ) {
                    CompactSettingsDialog(
                        textSettings = textSettings,
                        onSettingsUpdate = { newSettings ->
                            viewModel.updateTextSettings(newSettings)
                        },
                        onClose = { showSettings = false }
                    )
                }
            }
        }
    }
}

// Функция для разбора контента с изображениями
private fun parseContentWithImages(content: String, images: List<String>): List<ContentItem> {
    val items = mutableListOf<ContentItem>()

    if (content.isEmpty()) {
        return items
    }

    // Ищем маркеры [IMAGE:N] в тексте
    val regex = Regex("\\[IMAGE:(\\d+)\\]")
    var lastIndex = 0

    regex.findAll(content).forEach { matchResult ->
        val startIndex = matchResult.range.first
        val endIndex = matchResult.range.last + 1

        // Добавляем текст до изображения
        if (startIndex > lastIndex) {
            val text = content.substring(lastIndex, startIndex).trim()
            if (text.isNotEmpty()) {
                items.add(ContentItem.Text(text))
            }
        }

        // Добавляем изображение
        val imageIndex = matchResult.groupValues[1].toIntOrNull()
        if (imageIndex != null && imageIndex < images.size) {
            items.add(ContentItem.Image(images[imageIndex]))
        } else if (imageIndex != null) {
            // Если индекс изображения выходит за пределы, добавляем плейсхолдер
            items.add(ContentItem.Image("", "Изображение $imageIndex"))
        }

        lastIndex = endIndex
    }

    // Добавляем оставшийся текст
    if (lastIndex < content.length) {
        val text = content.substring(lastIndex).trim()
        if (text.isNotEmpty()) {
            items.add(ContentItem.Text(text))
        }
    }

    // Если нет маркеров, просто добавляем весь текст
    if (items.isEmpty() && content.isNotEmpty()) {
        items.add(ContentItem.Text(content))
    }

    return items
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactSettingsDialog(
    textSettings: TextSettings,
    onSettingsUpdate: (TextSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Заголовок с кнопкой закрытия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Настройки чтения",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Содержимое настроек со скроллом
            Box(
                modifier = Modifier.weight(1f)
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp)
                ) {
                    // Размер шрифта
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Размер шрифта",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Размер шрифта",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${textSettings.fontSize.toInt()}sp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (textSettings.fontSize > 12) {
                                    onSettingsUpdate(textSettings.copy(fontSize = textSettings.fontSize - 2))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = textSettings.fontSize > 12
                        ) {
                            Text("A-")
                        }

                        Slider(
                            value = textSettings.fontSize,
                            onValueChange = { onSettingsUpdate(textSettings.copy(fontSize = it)) },
                            valueRange = 12f..30f,
                            modifier = Modifier.weight(3f)
                        )

                        IconButton(
                            onClick = {
                                if (textSettings.fontSize < 30) {
                                    onSettingsUpdate(textSettings.copy(fontSize = textSettings.fontSize + 2))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = textSettings.fontSize < 30
                        ) {
                            Text("A+")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Цветовая тема
                    Text(
                        text = "Цветовая тема",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            "light" to "Светлая",
                            "dark" to "Тёмная",
                            "sepia" to "Сепия",
                            "contrast" to "Контраст"
                        )

                        themes.forEach { (themeValue, themeName) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                val isSelected = textSettings.theme == themeValue
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                        .padding(if (isSelected) 2.dp else 0.dp)
                                        .background(
                                            color = when (themeValue) {
                                                "light" -> Color.White
                                                "dark" -> Color(0xFF121212)
                                                "sepia" -> Color(0xFFF8F0E3)
                                                "contrast" -> Color.Black
                                                else -> Color.White
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable { onSettingsUpdate(textSettings.copy(theme = themeValue)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (themeValue == "contrast") {
                                        Text(
                                            text = "A",
                                            color = Color.Green,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = themeName,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Шрифт
                    Text(
                        text = "Шрифт",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fonts = listOf("Georgia", "Arial", "Courier")
                        fonts.forEach { font ->
                            FilterChip(
                                selected = textSettings.fontFamily == font,
                                onClick = { onSettingsUpdate(textSettings.copy(fontFamily = font)) },
                                label = {
                                    Text(
                                        text = font,
                                        fontSize = 14.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Межстрочный интервал
                    Text(
                        text = "Межстрочный интервал",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val lineHeights = listOf(
                            "Узкий" to 1.2f,
                            "Средний" to 1.5f,
                            "Широкий" to 2.0f
                        )

                        lineHeights.forEach { (label, value) ->
                            FilterChip(
                                selected = textSettings.lineHeight == value,
                                onClick = { onSettingsUpdate(textSettings.copy(lineHeight = value)) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Отступы страницы
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FormatIndentIncrease,
                            contentDescription = "Отступы",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Отступы страницы",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${textSettings.margins.toInt()}dp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = textSettings.margins,
                        onValueChange = { onSettingsUpdate(textSettings.copy(margins = it)) },
                        valueRange = 8f..32f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Кнопка сброса настроек
                    OutlinedButton(
                        onClick = { onSettingsUpdate(TextSettings()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Сбросить настройки")
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка "Готово"
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Готово")
            }
        }
    }


@Composable
fun QuickSettingsMenu(
    position: Offset,
    textSettings: TextSettings,
    onBrightnessChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .offset(position.x.dp, position.y.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            // Быстрая регулировка яркости
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Brightness6, contentDescription = null, modifier = Modifier.size(16.dp))
                Slider(
                    value = textSettings.brightness,
                    onValueChange = onBrightnessChange,
                    valueRange = 0.5f..1f,
                    modifier = Modifier.width(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Быстрая регулировка размера шрифта
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                Slider(
                    value = textSettings.fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 12f..30f,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}
package com.yume.reader.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.yume.reader.data.models.TextSettings
import com.yume.reader.ui.viewmodels.ReadingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUnitApi::class)
@Composable
fun ReadingScreen(
    navController: NavController,
    bookId: Long,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    // Инициализируем ViewModel с bookId
    LaunchedEffect(bookId) {
        viewModel.setBookId(bookId)
    }

    // Состояния
    var isLoading by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showProgressIndicator by remember { mutableStateOf(true) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    // Настройки текста
    val textSettings by viewModel.textSettings.collectAsState(initial = TextSettings())

    // Данные из ViewModel
    val chapters by viewModel.chapters.collectAsState()
    val currentChapterIndex by viewModel.currentChapter.collectAsState()
    val readingProgress by viewModel.readingProgress.collectAsState()
    val isLoadingState by viewModel.isLoading.collectAsState()

    val currentChapter = remember(chapters, currentChapterIndex) {
        chapters.getOrNull(currentChapterIndex - 1)
    }

    // Обновляем локальное состояние загрузки
    LaunchedEffect(isLoadingState, chapters) {
        isLoading = isLoadingState
    }

    // Автосохранение прогресса при прокрутке
    val lazyListState = rememberLazyListState()
    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        if (lazyListState.isScrollInProgress) {
            viewModel.saveReadingProgress()
        }
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
                    text = if (chapters.isEmpty()) "Загрузка глав..." else "Загрузка...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
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
        "contrast" -> Color(0xFF00FF00) // Зеленый для контраста
        else -> Color.Black
    }

    val fontFamily = when (textSettings.fontFamily) {
        "Georgia" -> FontFamily.Serif
        "Arial" -> FontFamily.SansSerif
        "Courier" -> FontFamily.Monospace
        else -> FontFamily.Default
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
                            onClick = { showSettings = !showSettings },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Настройки",
                                tint = textColor
                            )
                        }

                        // Процент прочтения
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(readingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    },
                    // ВАЖНО: Добавляем цвета для TopAppBar
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
            // Основной контент
            val chapterContent = remember(currentChapter) {
                currentChapter?.content?.let { content ->
                    content.split("\n\n", "\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it.length > 3 }
                        .filterNot { it.matches(Regex("^[\\d\\s.:/-]+$")) }
                } ?: emptyList()
            }

            if (chapterContent.isEmpty()) {
                // Состояние "пустая глава"
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
            } else {
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
                    items(chapterContent) { paragraph ->
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

            // Оверлей настроек
            AnimatedVisibility(
                visible = showSettings,
                enter = fadeIn() + slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = fadeOut() + slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                SettingsOverlay(
                    textSettings = textSettings,
                    onSettingsUpdate = { newSettings ->
                        viewModel.updateTextSettings(newSettings)
                    },
                    onClose = { showSettings = false },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(LocalConfiguration.current.screenWidthDp.dp * 0.85f)
                        .align(Alignment.CenterEnd)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                )
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
        }
    }
}

@Composable
fun SettingsOverlay(
    textSettings: TextSettings,
    onSettingsUpdate: (TextSettings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Заголовок
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Настройки чтения",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Размер шрифта
        SettingSection(title = "Размер шрифта") {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        onSettingsUpdate(textSettings.copy(fontSize = textSettings.fontSize - 2))
                    },
                    enabled = textSettings.fontSize > 12
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Уменьшить")
                }

                Text(
                    text = "${textSettings.fontSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                IconButton(
                    onClick = {
                        onSettingsUpdate(textSettings.copy(fontSize = textSettings.fontSize + 2))
                    },
                    enabled = textSettings.fontSize < 30
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Увеличить")
                }
            }
        }

        // Цветовая тема
        SettingSection(title = "Цветовая тема") {
            val themes = listOf("Светлая", "Тёмная", "Сепия", "Контраст")
            val themeValues = listOf("light", "dark", "sepia", "contrast")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                themeValues.forEachIndexed { index, theme ->
                    FilterChip(
                        selected = textSettings.theme == theme,
                        onClick = {
                            onSettingsUpdate(textSettings.copy(theme = theme))
                        },
                        label = { Text(themes[index]) },
                        modifier = Modifier.weight(1f)
                    )
                    if (index < themes.lastIndex) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }

        // Шрифт
        SettingSection(title = "Шрифт") {
            val fonts = listOf("Georgia", "Arial", "Courier")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                fonts.forEach { font ->
                    FilterChip(
                        selected = textSettings.fontFamily == font,
                        onClick = {
                            onSettingsUpdate(textSettings.copy(fontFamily = font))
                        },
                        label = { Text(font) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        // Межстрочный интервал
        SettingSection(title = "Межстрочный интервал") {
            val lineHeights = listOf("Узкий" to 1.2f, "Средний" to 1.5f, "Широкий" to 2.0f)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                lineHeights.forEach { (label, value) ->
                    FilterChip(
                        selected = textSettings.lineHeight == value,
                        onClick = {
                            onSettingsUpdate(textSettings.copy(lineHeight = value))
                        },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }

        // Яркость
        SettingSection(title = "Яркость экрана") {
            Slider(
                value = textSettings.brightness,
                onValueChange = { newValue ->
                    onSettingsUpdate(textSettings.copy(brightness = newValue))
                },
                valueRange = 0.5f..1f,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${(textSettings.brightness * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }

        // Отступы
        SettingSection(title = "Отступы страницы") {
            Slider(
                value = textSettings.margins,
                onValueChange = { newValue ->
                    onSettingsUpdate(textSettings.copy(margins = newValue))
                },
                valueRange = 8f..32f,
                steps = 6,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${textSettings.margins.toInt()}dp",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка сброса
        Button(
            onClick = {
                onSettingsUpdate(TextSettings())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text("Сбросить настройки")
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
package com.yume.reader.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.models.ReadingProgress
import com.yume.reader.data.models.TextSettings
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ChapterRepository
import com.yume.reader.data.repository.ReadingProgressRepository
import com.yume.reader.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val settingsRepository: SettingsRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow<Long?>(null)
    private val currentBookId: Long? get() = _currentBookId.value

    // Состояние загрузки
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Главы (только метаданные)
    val chapters: StateFlow<List<com.yume.reader.data.local.entity.ChapterEntity>> =
        _currentBookId.flatMapLatest { bookId ->
            if (bookId != null) {
                chapterRepository.getChaptersByBookId(bookId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Текущая глава (номер)
    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    // Контент текущей главы
    private val _currentContent = MutableStateFlow<String?>(null)
    val currentContent: StateFlow<String?> = _currentContent.asStateFlow()

    // Прогресс чтения (0-1)
    private val _readingProgress = MutableStateFlow(0f)
    val readingProgress: StateFlow<Float> = _readingProgress.asStateFlow()

    // Настройки текста
    private val _textSettings = MutableStateFlow(TextSettings())
    val textSettings: StateFlow<TextSettings> = _textSettings.asStateFlow()

    // Автосохранение
    private var autoSaveJob: Job? = null
    private var needsSave = false

    // Для отслеживания, была ли уже загружена книга
    private var bookLoaded = false

    init {
        // Наблюдаем за изменениями главы и обновляем прогресс
        viewModelScope.launch {
            currentChapter.collect { chapter ->
                if (currentBookId != null && bookLoaded) {
                    updateReadingProgress()
                    saveProgress()
                    // При смене главы загружаем ее контент
                    loadChapterContent(chapter)
                }
            }
        }

        // Запускаем автосохранение
        startAutoSave()
    }

    fun setBookId(bookId: Long) {
        if (_currentBookId.value == bookId && bookLoaded) {
            // Уже загружена та же книга
            return
        }

        _currentBookId.value = bookId
        _currentChapter.value = 1
        _isLoading.value = true
        bookLoaded = false

        viewModelScope.launch {
            try {
                // Загружаем сохраненный прогресс
                loadSavedProgress(bookId)

                // Загружаем настройки
                loadTextSettings(bookId)

                // Ждем загрузки хотя бы одной главы
                val firstChapter = chapters.firstOrNull { it.isNotEmpty() }
                if (firstChapter != null) {
                    bookLoaded = true
                }

                // Загружаем контент текущей главы
                loadChapterContent(_currentChapter.value)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSavedProgress(bookId: Long) {
        try {
            readingProgressRepository.getReadingProgress(bookId)
                .firstOrNull()
                ?.let { progress ->
                    _currentChapter.value = progress.currentChapter.coerceAtLeast(1)
                    _readingProgress.value = progress.progressPercent
                }
        } catch (e: Exception) {
            // Игнорируем ошибку при первом запуске
            _currentChapter.value = 1
            _readingProgress.value = 0f
        }
    }

    private suspend fun loadTextSettings(bookId: Long) {
        try {
            // ЗАГРУЖАЕМ НАСТРОЙКИ КОНКРЕТНОЙ КНИГИ (bookId > 0)
            settingsRepository.getTextSettings(bookId)
                .firstOrNull()
                ?.let { settings ->
                    _textSettings.value = settings
                } ?: run {
                // Если нет настроек для книги, создаем дефолтные
                _textSettings.value = TextSettings.defaultForBook(bookId)
            }
        } catch (e: Exception) {
            // Используем настройки по умолчанию для книги
            _textSettings.value = TextSettings.defaultForBook(bookId)
        }
    }

    // Метод для загрузки контента главы
    private fun loadChapterContent(chapterNumber: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                currentBookId?.let { bookId ->
                    val content = bookRepository.getChapterContent(bookId, chapterNumber)
                    _currentContent.value = content

                    // Обновляем статус прочтения текущей главы
                    updateChapterReadStatus(chapterNumber, true)

                    // Предзагружаем следующие главы для плавной навигации
                    if (chapterNumber < chapters.value.size) {
                        val start = chapterNumber + 1
                        val end = minOf(chapterNumber + 3, chapters.value.size)
                        bookRepository.preloadChapters(bookId, start, end)
                    }
                }
            } catch (e: Exception) {
                _currentContent.value = """
                    Ошибка загрузки главы $chapterNumber
                    
                    ${e.message ?: "Неизвестная ошибка"}
                    
                    Попробуйте перейти к другой главе или перезагрузить книгу.
                """.trimIndent()
                println("Ошибка загрузки контента: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun nextChapter() {
        val next = _currentChapter.value + 1
        if (next <= chapters.value.size) {
            _currentChapter.value = next
        }
    }

    fun previousChapter() {
        val prev = _currentChapter.value - 1
        if (prev >= 1) {
            _currentChapter.value = prev
        }
    }

    fun goToChapter(chapterNumber: Int) {
        if (chapterNumber in 1..chapters.value.size) {
            _currentChapter.value = chapterNumber
        }
    }

    fun updateTextSettings(newSettings: TextSettings) {
        _textSettings.value = newSettings
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                settingsRepository.updateTextSettings(bookId, newSettings)
            }
        }
    }

    private fun updateReadingProgress() {
        val total = chapters.value.size
        val current = _currentChapter.value

        if (total > 0 && current in 1..total) {
            _readingProgress.value = current.toFloat() / total
        } else {
            _readingProgress.value = 0f
        }
    }

    fun saveReadingProgress() {
        saveProgress()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveProgress() {
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                try {
                    val progress = ReadingProgress(
                        bookId = bookId,
                        currentChapter = _currentChapter.value,
                        progressPercent = _readingProgress.value,
                        lastReadAt = LocalDateTime.now(),
                        totalChapters = chapters.value.size
                    )

                    // Сохраняем в таблице прогресса чтения
                    readingProgressRepository.saveReadingProgress(progress)

                    // Также обновляем в таблице книг
                    val totalChapters = chapters.value.size
                    val currentChapter = _currentChapter.value
                    val progressPercent = if (totalChapters > 0) {
                        (currentChapter.toFloat() / totalChapters) * 100
                    } else {
                        0f
                    }

                    bookRepository.updateReadingProgress(
                        bookId,
                        currentChapter,
                        progressPercent.toInt()
                    )

                    // Помечаем текущую главу как прочитанную, если прогресс > 90%
                    if (_readingProgress.value > 0.9f) {
                        updateChapterReadStatus(currentChapter, true)
                    }

                    needsSave = false
                } catch (e: Exception) {
                    println("Ошибка сохранения прогресса: ${e.message}")
                }
            }
        }
    }

    // Обновление статуса прочтения главы
    private fun updateChapterReadStatus(chapterNumber: Int, isRead: Boolean) {
        viewModelScope.launch {
            try {
                currentBookId?.let { bookId ->
                    val chapter = chapterRepository.getChapter(bookId, chapterNumber)
                    chapter?.let {
                        chapterRepository.updateReadStatus(it.id, isRead)
                    }
                }
            } catch (e: Exception) {
                println("Ошибка обновления статуса главы: ${e.message}")
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(30000) // 30 секунд
                if (needsSave) {
                    saveProgress()
                }
            }
        }
    }

    // Вспомогательные методы для UI
    fun getProgressPercentage(): Int {
        return (_readingProgress.value * 100).toInt()
    }

    fun getChapterProgressText(): String {
        return "${_currentChapter.value}/${chapters.value.size}"
    }

    // Методы для управления настройками текста
    fun increaseFontSize() {
        val newSize = _textSettings.value.fontSize + 1
        if (newSize <= 30) {
            updateTextSettings(_textSettings.value.copy(fontSize = newSize))
        }
    }

    fun decreaseFontSize() {
        val newSize = _textSettings.value.fontSize - 1
        if (newSize >= 12) {
            updateTextSettings(_textSettings.value.copy(fontSize = newSize))
        }
    }

    fun changeTheme(theme: String) {
        updateTextSettings(_textSettings.value.copy(theme = theme))
    }

    fun changeFontFamily(fontFamily: String) {
        updateTextSettings(_textSettings.value.copy(fontFamily = fontFamily))
    }

    fun updateLineHeight(lineHeight: Float) {
        updateTextSettings(_textSettings.value.copy(lineHeight = lineHeight))
    }

    fun updateMargins(margins: Float) {
        updateTextSettings(_textSettings.value.copy(margins = margins))
    }

    fun updateBrightness(brightness: Float) {
        updateTextSettings(_textSettings.value.copy(brightness = brightness))
    }

    fun resetToDefaultSettings() {
        updateTextSettings(TextSettings())
    }

    override fun onCleared() {
        super.onCleared()
        // Сохраняем прогресс перед закрытием
        saveProgress()
        autoSaveJob?.cancel()
    }
}
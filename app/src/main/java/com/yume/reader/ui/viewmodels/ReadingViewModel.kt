package com.yume.reader.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.models.ReadingProgress
import com.yume.reader.data.models.TextSettings
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ReadingProgressRepository
import com.yume.reader.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow<Long?>(null)
    private val currentBookId: Long? get() = _currentBookId.value

    // Состояние загрузки
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Главы
    val chapters: StateFlow<List<com.yume.reader.data.local.entity.ChapterEntity>> =
        _currentBookId.flatMapLatest { bookId ->
            if (bookId != null) {
                bookRepository.getChaptersForBook(bookId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Текущая глава
    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

    // Прогресс чтения (0-1)
    private val _readingProgress = MutableStateFlow(0f)
    val readingProgress: StateFlow<Float> = _readingProgress.asStateFlow()

    // Настройки текста
    private val _textSettings = MutableStateFlow(TextSettings())
    val textSettings: StateFlow<TextSettings> = _textSettings.asStateFlow()

    // Автосохранение
    private var autoSaveJob: Job? = null
    private var needsSave = false

    init {
        // Наблюдаем за изменениями главы и обновляем прогресс
        viewModelScope.launch {
            currentChapter.collect { chapter ->
                if (currentBookId != null) {
                    updateReadingProgress()
                    saveProgress()
                }
            }
        }

        // Запускаем автосохранение
        startAutoSave()
    }

    fun setBookId(bookId: Long) {
        _currentBookId.value = bookId
        _currentChapter.value = 1
        _isLoading.value = true

        viewModelScope.launch {
            // Загружаем сохраненный прогресс
            loadSavedProgress(bookId)

            // Загружаем настройки
            loadTextSettings(bookId)

            _isLoading.value = false
        }
    }

    private suspend fun loadSavedProgress(bookId: Long) {
        readingProgressRepository.getReadingProgress(bookId)
            .take(1)
            .collect { progress ->
                progress?.let {
                    _currentChapter.value = it.currentChapter.coerceAtLeast(1)
                    _readingProgress.value = it.progressPercent
                }
            }
    }

    private suspend fun loadTextSettings(bookId: Long) {
        settingsRepository.getTextSettings(bookId)
            .take(1)
            .collect { settings ->
                _textSettings.value = settings
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
    // В методе saveProgress добавляем сохранение в BookEntity
    private fun saveProgress() {
        currentBookId?.let { bookId ->
            viewModelScope.launch {
                val progress = ReadingProgress(
                    bookId = bookId,
                    currentChapter = _currentChapter.value,
                    progressPercent = _readingProgress.value,
                    lastReadAt = LocalDateTime.now(),
                    totalChapters = chapters.value.size
                )

                // Сохраняем в таблице прогресса чтения через репозиторий
                readingProgressRepository.saveReadingProgress(progress) // Используем saveReadingProgress

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

                needsSave = false
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

    fun getCurrentChapterText(): String {
        return chapters.value.getOrNull(_currentChapter.value - 1)?.content ?: ""
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
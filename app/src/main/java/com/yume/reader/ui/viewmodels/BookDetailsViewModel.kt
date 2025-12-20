package com.yume.reader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ChapterRepository
import com.yume.reader.data.repository.ReadingProgressRepository
import com.yume.reader.domain.models.Book
import com.yume.reader.domain.models.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val readingProgressRepository: ReadingProgressRepository
) : ViewModel() {

    private val _bookId = MutableStateFlow<Long?>(null)

    // Загружаем книгу
    val book: StateFlow<Book?> = _bookId.flatMapLatest { bookId ->
        if (bookId != null) {
            bookRepository.getAllBooks()
                .map { books -> books.find { it.id == bookId }?.toDomain() }
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Загружаем главы
    val chapters: StateFlow<List<com.yume.reader.data.local.entity.ChapterEntity>> =
        _bookId.flatMapLatest { bookId ->
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

    // Состояния для поиска и фильтрации
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ИЗМЕНЕНО: Начальное значение фильтра - "Все"
    private val _selectedFilter = MutableStateFlow("Все")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Прогресс чтения
    val readingProgress: StateFlow<com.yume.reader.data.models.ReadingProgress?> =
        _bookId.flatMapLatest { bookId ->
            if (bookId != null) {
                readingProgressRepository.getReadingProgress(bookId)
            } else {
                flowOf(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setBookId(bookId: Long) {
        _bookId.value = bookId
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _selectedFilter.value = filter
    }

    // Переключение статуса "Избранное"
    fun toggleFavorite() {
        _bookId.value?.let { bookId ->
            viewModelScope.launch {
                book.value?.let { book ->
                    bookRepository.toggleFavorite(bookId, !book.isFavorite)
                }
            }
        }
    }

    // Переключение статуса "Читаю сейчас"
    fun toggleReadingStatus() {
        _bookId.value?.let { bookId ->
            viewModelScope.launch {
                book.value?.let { book ->
                    bookRepository.toggleReadingStatus(bookId, !book.isReading)
                }
            }
        }
    }

    // Получение общего времени чтения книги
    fun getTotalReadingTime(): String {
        val totalMinutes = chapters.value.sumOf { it.durationMinutes }
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
            hours > 0 -> "$hours ч"
            else -> "$minutes мин"
        }
    }

    // Получение отфильтрованных глав
    val filteredChapters: StateFlow<List<com.yume.reader.data.local.entity.ChapterEntity>> =
        combine(chapters, searchQuery, selectedFilter) { chapters, query, filter ->
            var result = chapters

            // Применяем поиск
            if (query.isNotBlank()) {
                result = result.filter { chapter ->
                    chapter.title.contains(query, ignoreCase = true)
                }
            }

            // ИЗМЕНЕНО: Применяем фильтр с правильными названиями
            result = when (filter) {
                "Прочитанно" -> result.filter { it.isRead }
                "Непрочитанно" -> result.filter { !it.isRead }
                else -> result // "Все"
            }

            result
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Обновление статуса прочтения главы
    fun toggleChapterReadStatus(chapterId: Long, isRead: Boolean) {
        viewModelScope.launch {
            chapterRepository.updateReadStatus(chapterId, isRead)
        }
    }

    // Получение текущей главы из прогресса чтения
    val currentChapter: StateFlow<Int> = readingProgress.map { progress ->
        progress?.currentChapter ?: 1
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1
    )

    // Проверка доступности главы в кеше
    suspend fun isChapterCached(chapterNumber: Int): Boolean {
        return _bookId.value?.let { bookId ->
            bookRepository.isChapterCached(bookId, chapterNumber)
        } ?: false
    }

    // Получение информации о кешированных главах
    suspend fun getCachedChaptersInfo(): Map<Int, Boolean> {
        return _bookId.value?.let { bookId ->
            val cached = bookRepository.getCachedChapters(bookId)
            chapters.value.associate { chapter ->
                chapter.chapterNumber to cached.contains(chapter.chapterNumber)
            }
        } ?: emptyMap()
    }
}
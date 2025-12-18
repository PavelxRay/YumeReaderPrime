package com.yume.reader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow<Long?>(null)

    private val _currentChapter = MutableStateFlow(1)
    val currentChapter: StateFlow<Int> = _currentChapter.asStateFlow()

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

    private val _readingProgress = MutableStateFlow(0f)
    val readingProgress: StateFlow<Float> = _readingProgress.asStateFlow()

    init {
        viewModelScope.launch {
            chapters.collect { chaptersList ->
                updateReadingProgress()
            }
        }

        viewModelScope.launch {
            currentChapter.collect {
                updateReadingProgress()
            }
        }
    }

    fun setBookId(bookId: Long) {
        _currentBookId.value = bookId
        _currentChapter.value = 1 // Сбрасываем на первую главу
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

    private fun updateReadingProgress() {
        val total = chapters.value.size
        val current = _currentChapter.value

        if (total > 0 && current in 1..total) {
            _readingProgress.value = current.toFloat() / total
        } else {
            _readingProgress.value = 0f
        }
    }

    fun getCurrentChapterText(): String {
        return chapters.value.getOrNull(_currentChapter.value - 1)?.content ?: ""
    }
}
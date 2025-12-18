package com.yume.reader.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ChapterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bookId = savedStateHandle.get<Long>("bookId") ?: 0L

    // Текущая глава
    private val _currentChapter = MutableStateFlow(1)
    val currentChapter = _currentChapter.asStateFlow()

    // Список глав
    val chapters = chapterRepository.getChaptersByBookId(bookId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Текущий текст главы
    val currentChapterText = combine(
        chapters,
        currentChapter
    ) { chaptersList, chapterNum ->
        chaptersList.find { it.chapterNumber == chapterNum }?.content ?: ""
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Прогресс чтения книги
    val readingProgress = combine(
        chapters,
        currentChapter
    ) { chaptersList, chapterNum ->
        if (chaptersList.isEmpty()) 0f
        else chapterNum.toFloat() / chaptersList.size.toFloat()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    fun nextChapter() {
        viewModelScope.launch {
            val chaptersCount = chapterRepository.getTotalChaptersCount(bookId)
            if (_currentChapter.value < chaptersCount) {
                _currentChapter.value++
                updateReadingProgress()
            }
        }
    }

    fun previousChapter() {
        if (_currentChapter.value > 1) {
            _currentChapter.value--
            updateReadingProgress()
        }
    }

    fun goToChapter(chapterNumber: Int) {
        viewModelScope.launch {
            val maxChapter = chapterRepository.getTotalChaptersCount(bookId)
            if (chapterNumber in 1..maxChapter) {
                _currentChapter.value = chapterNumber
                updateReadingProgress()
            }
        }
    }

    private fun updateReadingProgress() {
        viewModelScope.launch {
            val totalChapters = chapterRepository.getTotalChaptersCount(bookId)
            val readChapters = chapterRepository.getReadChaptersCount(bookId)
            val progress = ((readChapters + 1).toFloat() / totalChapters.toFloat() * 100).toInt()
            bookRepository.updateReadingProgress(bookId, _currentChapter.value, progress)
        }
    }

    fun markChapterAsRead() {
        viewModelScope.launch {
            chapters.value.find { it.chapterNumber == _currentChapter.value }?.let { chapter ->
                chapterRepository.updateReadStatus(chapter.id, true)
            }
        }
    }
}
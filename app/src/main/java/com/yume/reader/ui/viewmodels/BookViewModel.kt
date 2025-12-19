// ui/viewmodels/BookViewModel.kt
package com.yume.reader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.domain.models.Book
import com.yume.reader.domain.models.toDomain
import com.yume.reader.domain.models.toEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val readingBooks: StateFlow<List<Book>> = bookRepository.getReadingBooks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteBooks: StateFlow<List<Book>> = bookRepository.getFavoriteBooks()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Получение прогресса для конкретной книги
    suspend fun getBookProgress(bookId: Long): Float {
        return bookRepository.getReadingProgressForBook(bookId)
    }

    // Обновление прогресса книги
    fun updateBookProgress(bookId: Long, currentChapter: Int, progressPercent: Float) {
        viewModelScope.launch {
            bookRepository.updateBookReadingProgress(bookId, currentChapter, progressPercent)
        }
    }

    fun addBook(book: Book) = viewModelScope.launch {
        bookRepository.addBook(book.toEntity())
    }

    fun updateBook(book: Book) = viewModelScope.launch {
        bookRepository.updateBook(book.toEntity())
    }

    fun updateReadingProgress(bookId: Long, page: Int, progress: Int) = viewModelScope.launch {
        bookRepository.updateReadingProgress(bookId, page, progress)
    }

    fun toggleFavorite(bookId: Long, isFavorite: Boolean) = viewModelScope.launch {
        bookRepository.toggleFavorite(bookId, isFavorite)
    }

    fun toggleReadingStatus(bookId: Long, isReading: Boolean) = viewModelScope.launch {
        bookRepository.toggleReadingStatus(bookId, isReading)
    }

    // Для поиска
    fun searchBooks(query: String): StateFlow<List<Book>> {
        return bookRepository.searchBooks(query)
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Получение статистики
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "finishedBooks" to books.value.count { it.isFinished },
            "readingBooks" to books.value.count { it.isReading && !it.isFinished },
            "totalChapters" to books.value.sumOf { book ->
                // Если книга завершена, считаем все главы
                if (book.isFinished) book.totalPages
                // Иначе считаем только прочитанные главы
                else book.currentPage
            }
        )
    }
}
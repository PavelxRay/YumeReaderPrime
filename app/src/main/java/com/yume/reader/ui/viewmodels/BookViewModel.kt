package com.yume.reader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.domain.models.Book
import com.yume.reader.domain.models.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    // Получаем все книги
    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .map { books -> books.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Получаем книги, которые читаются
    val readingBooks: StateFlow<List<Book>> = bookRepository.getReadingBooks()
        .map { books -> books.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Получаем избранные книги
    val favoriteBooks: StateFlow<List<Book>> = bookRepository.getFavoriteBooks()
        .map { books -> books.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Переключение статуса "Избранное"
    fun toggleFavorite(bookId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            bookRepository.toggleFavorite(bookId, isFavorite)
        }
    }

    // Добавляем функцию удаления книги
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            bookRepository.getBookById(bookId)?.let { bookEntity ->
                bookRepository.deleteBook(bookEntity)
            }
        }
    }
}
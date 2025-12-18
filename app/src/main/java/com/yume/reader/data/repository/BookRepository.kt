package com.yume.reader.data.repository

import android.util.Log
import com.yume.reader.data.local.dao.BookDao
import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.entity.BookEntity
import com.yume.reader.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao
) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()
        .map { books ->
            Log.d("BookRepository", "getAllBooks: получено ${books.size} книг")
            books.forEach { book ->
                Log.d("BookRepository", " - ${book.title}, isReading=${book.isReading}, isFinished=${book.isFinished}")
            }
            books
        }

    fun getReadingBooks(): Flow<List<BookEntity>> = bookDao.getReadingBooks()

    fun getFinishedBooks(): Flow<List<BookEntity>> = bookDao.getFinishedBooks()

    fun getFavoriteBooks(): Flow<List<BookEntity>> = bookDao.getFavoriteBooks()

    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)

    suspend fun initializeIfEmpty() {
        Log.d("BookRepository", "🚀 Начинаем инициализацию базы...")

        try {
            val count = bookDao.getTotalBooksCount()
            Log.d("BookRepository", "📊 Текущее количество книг в базе: $count")

            if (count == 0) {
                Log.d("BookRepository", "📝 База пуста, добавляем тестовые книги")

                val testBooks = listOf(
                    BookEntity(
                        title = "Мастер и Маргарита",
                        author = "Михаил Булгаков",
                        totalPages = 384,
                        currentPage = 250,
                        progress = 65,
                        isReading = true,
                        isFavorite = true,
                        addedDate = Date(),
                        lastReadDate = Date()
                    ),
                    BookEntity(
                        title = "1984",
                        author = "Джордж Оруэлл",
                        totalPages = 328,
                        currentPage = 328,
                        progress = 100,
                        isFinished = true,
                        isFavorite = true,
                        addedDate = Date(),
                        lastReadDate = Date()
                    ),
                    BookEntity(
                        title = "Преступление и наказание",
                        author = "Фёдор Достоевский",
                        totalPages = 672,
                        currentPage = 200,
                        progress = 30,
                        isReading = true,
                        addedDate = Date(),
                        lastReadDate = Date()
                    ),
                    BookEntity(
                        title = "Маленький принц",
                        author = "Антуан де Сент-Экзюпери",
                        totalPages = 96,
                        currentPage = 96,
                        progress = 100,
                        isFinished = true,
                        addedDate = Date()
                    )
                )

                testBooks.forEach { book ->
                    try {
                        val id = bookDao.insertBook(book)
                        Log.d("BookRepository", "✅ Добавлена книга: ${book.title}, ID = $id")
                    } catch (e: Exception) {
                        Log.e("BookRepository", "❌ Ошибка при добавлении книги ${book.title}: ${e.message}")
                    }
                }

                Log.d("BookRepository", "🎉 Инициализация завершена. Добавлено ${testBooks.size} книг")
            } else {
                Log.d("BookRepository", "📚 База уже содержит $count книг, пропускаем инициализацию")
            }
        } catch (e: Exception) {
            Log.e("BookRepository", "💥 Критическая ошибка в initializeIfEmpty(): ${e.message}", e)
        }
    }

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    suspend fun addBook(book: BookEntity): Long = bookDao.insertBook(book)

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    suspend fun updateReadingProgress(id: Long, page: Int, progress: Int) {
        bookDao.updateReadingProgress(id, page, progress)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        bookDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun toggleReadingStatus(id: Long, isReading: Boolean) {
        bookDao.updateReadingStatus(id, isReading)
    }

    suspend fun addTestChapters() {
        Log.d("BookRepository", "📖 Начинаем добавление тестовых глав...")

        val books = getAllBooks().firstOrNull() ?: emptyList()
        Log.d("BookRepository", "📚 Всего книг найдено: ${books.size}")

        books.forEach { book ->
            Log.d("BookRepository", " - ${book.title} (id=${book.id})")
        }

        val masterBook = books.find { it.title == "Мастер и Маргарита" }

        if (masterBook == null) {
            Log.e("BookRepository", "❌ Книга 'Мастер и Маргарита' не найдена!")
            return
        }

        Log.d("BookRepository", "✅ Найдена книга 'Мастер и Маргарита' с id=${masterBook.id}")

        val chapters = listOf(
            ChapterEntity(
                bookId = masterBook.id,
                chapterNumber = 1,
                title = "Пролог",
                content = "Однажды весною, в час небывало жаркого заката, в Москве, на Патриарших прудах, появились два гражданина...",
                wordCount = 1500,
                durationMinutes = 8
            ),
            ChapterEntity(
                bookId = masterBook.id,
                chapterNumber = 2,
                title = "Никогда не разговаривайте с неизвестными",
                content = "Да, нужно отметить первую странность этого страшного майского вечера...",
                wordCount = 2000,
                durationMinutes = 10
            ),
            ChapterEntity(
                bookId = masterBook.id,
                chapterNumber = 3,
                title = "Седьмое доказательство",
                content = "На закате солнца высоко над городом на каменной террасе одного из самых красивых зданий в Москве...",
                wordCount = 1800,
                durationMinutes = 9
            )
        )

        chapters.forEach { chapter ->
            try {
                val id = chapterDao.insertChapter(chapter)
                Log.d("BookRepository", "✅ Добавлена глава: ${chapter.title} (id=$id)")
            } catch (e: Exception) {
                Log.e("BookRepository", "❌ Ошибка при добавлении главы ${chapter.title}: ${e.message}")
            }
        }

        Log.d("BookRepository", "🎉 Добавлено ${chapters.size} глав для книги 'Мастер и Маргарита'")
    }

    fun getFinishedBooksCount(): Flow<Int> = bookDao.getFinishedBooksCount()

    fun getTotalPagesRead(): Flow<Int?> = bookDao.getTotalPagesRead()

    fun getReadingBooksCount(): Flow<Int> = bookDao.getReadingBooksCount()
}
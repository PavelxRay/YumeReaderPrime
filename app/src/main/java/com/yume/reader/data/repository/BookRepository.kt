package com.yume.reader.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.yume.reader.data.local.dao.BookDao
import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.entity.BookEntity
import com.yume.reader.data.local.entity.ChapterEntity
import com.yume.reader.domain.models.epub.EpubBook
import com.yume.reader.data.epub.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    val context: Context
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

    // EPUB-специфичные методы
    suspend fun importEpubBook(epubBook: EpubBook): Long {
        Log.d("BookRepository", "Импортируем EPUB: ${epubBook.title}")

        return withContext(Dispatchers.IO) {
            try {
                // Сохраняем обложку
                val coverPath = epubBook.coverImage?.let {
                    saveCoverImage(it, epubBook.title)
                }

                // Создаем сущность книги
                val bookEntity = BookEntity(
                    title = epubBook.title,
                    author = epubBook.author,
                    coverUrl = coverPath,
                    description = epubBook.description,
                    totalPages = epubBook.metadata.totalPages,
                    filePath = epubBook.filePath,
                    fileFormat = "epub",
                    addedDate = Date(),
                    currentPage = 0,
                    progress = 0,
                    isReading = true // Автоматически начинаем читать
                )

                // Сохраняем книгу в базу
                val bookId = bookDao.insertBook(bookEntity)
                Log.d("BookRepository", "Книга сохранена с ID: $bookId")

                // Сохраняем главы
                epubBook.chapters.forEachIndexed { index, epubChapter ->
                    val chapterEntity = ChapterEntity(
                        bookId = bookId,
                        chapterNumber = epubChapter.chapterNumber,
                        title = epubChapter.title,
                        content = epubChapter.content,
                        wordCount = epubChapter.wordCount,
                        durationMinutes = calculateReadingTime(epubChapter.wordCount)
                    )
                    chapterDao.insertChapter(chapterEntity)
                }

                Log.d("BookRepository", "Добавлено ${epubBook.chapters.size} глав")
                bookId
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка импорта EPUB: ${e.message}", e)
                throw e // Пробрасываем исключение дальше
            }
        }
    }

    private fun saveCoverImage(imageData: ByteArray, bookTitle: String): String {
        return try {
            val fileName = "cover_${System.currentTimeMillis()}_${bookTitle.hashCode()}.jpg"
            val coversDir = File(context.filesDir, "covers")
            coversDir.mkdirs() // Создаем папку, если не существует

            val coverFile = File(coversDir, fileName)

            // Конвертируем ByteArray в Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: throw IllegalStateException("Не удалось декодировать обложку")

            // Сохраняем сжатое изображение
            coverFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }

            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e("BookRepository", "Ошибка сохранения обложки: ${e.message}")
            "" // Возвращаем пустую строку вместо null
        }
    }

    private fun calculateReadingTime(wordCount: Int): Int {
        // Средняя скорость чтения: 150-200 слов в минуту
        // Используем 180 как среднее значение
        return if (wordCount > 0) {
            maxOf(1, (wordCount / 180.0).toInt())
        } else {
            1 // Минимальное время для пустых глав
        }
    }

    suspend fun scanForEpubFiles(directoryPath: String): List<File> {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(directoryPath)
                if (!directory.exists() || !directory.isDirectory) {
                    return@withContext emptyList()
                }

                directory.listFiles { file ->
                    file.isFile && file.extension.equals("epub", ignoreCase = true)
                }?.toList() ?: emptyList()
            } catch (e: SecurityException) {
                Log.e("BookRepository", "Нет доступа к папке: ${e.message}")
                emptyList()
            }
        }
    }

    // Метод для проверки существования книги по пути
    suspend fun doesBookExist(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Простой способ - проверка в базе данных
                // В будущем можно добавить поле filePath в BookEntity для проверки
                val allBooks = bookDao.getAllBooks().firstOrNull() ?: emptyList()
                allBooks.any { it.filePath == filePath }
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка проверки книги: ${e.message}")
                false
            }
        }
    }

    // Метод для удаления импортированной книги
    suspend fun deleteImportedBook(bookId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Удаляем обложку, если есть
                val book = bookDao.getBookById(bookId)
                book?.coverUrl?.takeIf { it.isNotEmpty() }?.let { coverPath ->
                    File(coverPath).delete()
                }

                // Удаляем книгу из базы (каскадно удалятся главы)
                book?.let { bookDao.deleteBook(it) }

                Log.d("BookRepository", "Удалена книга с ID: $bookId")
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка удаления книги: ${e.message}")
            }
        }
    }

    // Дополнительные методы для BookRepository.kt

    // Получение информации об EPUB без импорта
    suspend fun getEpubBookInfo(filePath: String): EpubBook? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = File(filePath).inputStream()
                val epubParser = EpubParser(context)
                epubParser.parseEpub(inputStream, filePath)
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка чтения EPUB: ${e.message}")
                null
            }
        }
    }

    // Пакетный импорт нескольких EPUB
    suspend fun importMultipleEpubBooks(epubBooks: List<EpubBook>): List<Long> {
        return epubBooks.mapNotNull { epubBook ->
            try {
                importEpubBook(epubBook)
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка импорта ${epubBook.title}: ${e.message}")
                null
            }
        }
    }

    // Обновление прогресса чтения для EPUB
    suspend fun updateEpubReadingProgress(bookId: Long, chapterNumber: Int, progressPercent: Int) {
        withContext(Dispatchers.IO) {
            try {
                // Получаем книгу и обновляем, если она существует
                bookDao.getBookById(bookId)?.let { book ->
                    bookDao.updateReadingProgress(bookId, chapterNumber, progressPercent)

                    // Помечаем главу как прочитанную, если прогресс > 90%
                    if (progressPercent >= 90) {
                        chapterDao.getChaptersByBookId(bookId).firstOrNull()?.let { chapters ->
                            chapters.find { it.chapterNumber == chapterNumber }?.let { chapter ->
                                chapterDao.updateReadStatus(chapter.id, true)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка обновления прогресса: ${e.message}")
            }
        }
    }
}
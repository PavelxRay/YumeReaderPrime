package com.yume.reader.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.yume.reader.data.local.dao.ReadingProgressDao
import com.yume.reader.data.local.dao.BookDao
import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.entity.BookEntity
import com.yume.reader.data.local.entity.ChapterEntity
import com.yume.reader.domain.models.epub.EpubBook
import com.yume.reader.data.epub.EpubParser
import com.yume.reader.data.images.BookImageLoader
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
    private val readingProgressDao: ReadingProgressDao,
    private val chapterContentRepo: ChapterContentRepository, // Добавляем
    private val context: Context,
    private val imageLoader: BookImageLoader,
) {

    // Храним загруженные EPUB книги в памяти для быстрого доступа
    private val loadedEpubBooks = mutableMapOf<Long, EpubBook>()

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

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    // Обновляем метод deleteBook для очистки кеша изображений
    suspend fun deleteBook(book: BookEntity) {
        withContext(Dispatchers.IO) {
            try {
                // Удаляем кешированные главы
                chapterContentRepo.deleteBookChapters(book.id)

                // Удаляем кеш изображений
                imageLoader.clearCacheForBook(book.id)

                // Удаляем обложку
                book.coverUrl?.takeIf { it.isNotEmpty() }?.let { coverPath ->
                    File(coverPath).delete()
                }

                // Удаляем книгу из базы (каскадно удалятся главы)
                bookDao.deleteBook(book)

                // Удаляем из памяти
                loadedEpubBooks.remove(book.id)

                Log.d("BookRepository", "Удалена книга: ${book.title}")
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка удаления книги: ${e.message}")
            }
        }
    }

    suspend fun updateReadingProgress(id: Long, page: Int, progress: Int) {
        bookDao.updateReadingProgress(id, page, progress)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        bookDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun toggleReadingStatus(id: Long, isReading: Boolean) {
        bookDao.updateReadingStatus(id, isReading)
    }

    // EPUB-специфичные методы
    suspend fun importEpubBook(epubBook: EpubBook): Long {
        Log.d("BookRepository", "Импортируем EPUB: ${epubBook.title}")
        Log.d("BookRepository", "Количество глав: ${epubBook.chapters.size}")

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
                    totalPages = epubBook.chapters.size,
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

                // Сохраняем EPUB в память для быстрого доступа
                loadedEpubBooks[bookId] = epubBook

                // Сохраняем метаданные глав в БД
                epubBook.chapters.forEachIndexed { index, epubChapter ->
                    Log.d("BookRepository", "Глава ${index + 1}: ${epubChapter.title}, " +
                            "символов: ${epubChapter.content.length}, " +
                            "слов: ${epubChapter.wordCount}")

                    // Создаем сущность главы без контента
                    val chapterEntity = ChapterEntity.fromEpubChapter(bookId, epubChapter)
                    val chapterId = chapterDao.insertChapter(chapterEntity)
                    Log.d("BookRepository", "✅ Сохранена глава ${epubChapter.chapterNumber} с ID: $chapterId")

                    // Сохраняем контент первой главы для быстрого старта
                    if (index == 0) {
                        val contentPath = chapterContentRepo.saveChapterContent(
                            bookId,
                            epubChapter.chapterNumber,
                            epubChapter.content
                        )

                        // Обновляем путь к контенту
                        chapterDao.updateChapter(
                            chapterEntity.copy(id = chapterId, contentPath = contentPath)
                        )

                        // Кешируем соседние главы
                        chapterContentRepo.cacheAdjacentChapters(
                            bookId,
                            epubChapter.chapterNumber,
                            epubBook.chapters
                        )
                    }
                }

                Log.d("BookRepository", "🎉 Импорт завершен. Добавлено ${epubBook.chapters.size} глав")
                bookId
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка импорта EPUB: ${e.message}", e)
                throw e
            }
        }
    }

    private fun saveCoverImage(imageData: ByteArray, bookTitle: String): String {
        return try {
            val fileName = "cover_${System.currentTimeMillis()}_${bookTitle.hashCode()}.jpg"
            val coversDir = File(context.filesDir, "covers")
            coversDir.mkdirs()

            val coverFile = File(coversDir, fileName)

            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: throw IllegalStateException("Не удалось декодировать обложку")

            coverFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }

            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e("BookRepository", "Ошибка сохранения обложки: ${e.message}")
            ""
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
                val allBooks = bookDao.getAllBooks().firstOrNull() ?: emptyList()
                allBooks.any { it.filePath == filePath }
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка проверки книги: ${e.message}")
                false
            }
        }
    }

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

    // Получение контента главы с ленивой загрузкой
    suspend fun getChapterContent(bookId: Long, chapterNumber: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Проверяем кеш в файловой системе
                val cachedContent = chapterContentRepo.loadChapterContent(bookId, chapterNumber)
                if (cachedContent != null) {
                    return@withContext cachedContent
                }

                // 2. Если нет в кеше, получаем EPUB книгу
                val epubBook = loadedEpubBooks[bookId] ?: run {
                    // Если книги нет в памяти, загружаем из файла
                    val bookEntity = bookDao.getBookById(bookId)
                    bookEntity?.filePath?.let { filePath ->
                        getEpubBookInfo(filePath)
                    }?.also { epub ->
                        loadedEpubBooks[bookId] = epub
                    }
                }

                if (epubBook == null) {
                    throw Exception("EPUB книга не найдена")
                }

                // 3. Ищем нужную главу в EPUB
                val chapter = epubBook.chapters.find { it.chapterNumber == chapterNumber }
                    ?: throw Exception("Глава $chapterNumber не найдена")

                // 4. Сохраняем в кеш для будущего использования
                chapterContentRepo.saveChapterContent(bookId, chapterNumber, chapter.content)

                // 5. Кешируем соседние главы
                chapterContentRepo.cacheAdjacentChapters(bookId, chapterNumber, epubBook.chapters)

                // 6. Обновляем путь в БД
                val chapterEntity = chapterDao.getChapter(bookId, chapterNumber)
                chapterEntity?.let {
                    val contentPath = chapterContentRepo.saveChapterContent(bookId, chapterNumber, chapter.content)
                    chapterDao.updateChapter(it.copy(contentPath = contentPath))
                }

                Log.d("BookRepository", "Глава $chapterNumber загружена из EPUB и сохранена в кеш")
                chapter.content
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка загрузки контента главы: ${e.message}")
                throw Exception("Не удалось загрузить контент главы $chapterNumber: ${e.message}")
            }
        }
    }

    // Получение информации о доступности главы в кеше
    suspend fun isChapterCached(bookId: Long, chapterNumber: Int): Boolean {
        return chapterContentRepo.hasChapterInCache(bookId, chapterNumber)
    }

    // Получение всех закешированных глав
    suspend fun getCachedChapters(bookId: Long): List<Int> {
        return chapterContentRepo.getCachedChapters(bookId)
    }

    // Предзагрузка глав для быстрой навигации
    suspend fun preloadChapters(bookId: Long, startChapter: Int, endChapter: Int) {
        withContext(Dispatchers.IO) {
            try {
                val epubBook = loadedEpubBooks[bookId] ?: return@withContext

                for (chapter in epubBook.chapters) {
                    if (chapter.chapterNumber in startChapter..endChapter) {
                        if (!chapterContentRepo.hasChapterInCache(bookId, chapter.chapterNumber)) {
                            chapterContentRepo.saveChapterContent(
                                bookId,
                                chapter.chapterNumber,
                                chapter.content
                            )
                            Log.d("BookRepository", "Предзагружена глава ${chapter.chapterNumber}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BookRepository", "Ошибка предзагрузки глав: ${e.message}")
            }
        }
    }

    // Обновляем метод для получения контента с изображениями
    suspend fun getChapterContentWithImages(bookId: Long, chapterNumber: Int): Pair<String, List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val epubBook = loadedEpubBooks[bookId] ?: run {
                    val bookEntity = bookDao.getBookById(bookId)
                    bookEntity?.filePath?.let { filePath ->
                        getEpubBookInfo(filePath)
                    }?.also { epub ->
                        loadedEpubBooks[bookId] = epub
                    }
                }

                if (epubBook == null) {
                    throw Exception("EPUB книга не найдена")
                }

                val chapter = epubBook.chapters.find { it.chapterNumber == chapterNumber }
                    ?: throw Exception("Глава $chapterNumber не найдена")

                // Получаем контент
                val content = getChapterContent(bookId, chapterNumber)

                // Обрабатываем изображения
                val imageUrls = mutableListOf<String>()
                var processedContent = content

                // Если в главе есть изображения, обрабатываем их
                chapter.images.forEachIndexed { index, image ->
                    if (image.data != null) {
                        val imageUrl = imageLoader.createDataUrlFromBytes(image.data)
                        if (imageUrl.isNotEmpty()) {
                            imageUrls.add(imageUrl)
                            // Добавляем маркер для изображения в текст
                            processedContent += "\n\n[IMAGE:$index]\n\n"
                        }
                    }
                }

                // Сохраняем обновленный контент в кеш
                chapterContentRepo.saveChapterContent(bookId, chapterNumber, processedContent)

                Pair(processedContent, imageUrls)
            } catch (e: Exception) {
                // Fallback: возвращаем обычный контент
                val content = getChapterContent(bookId, chapterNumber)
                Pair(content, emptyList())
            }
        }
    }
}
package com.yume.reader.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterContentRepository @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val CHAPTERS_DIR = "chapters"
        private const val CACHE_SIZE = 5 // Кешируем текущую + 4 соседних главы
    }

    private val chaptersDir by lazy {
        File(context.filesDir, CHAPTERS_DIR).apply {
            if (!exists()) mkdirs()
            Log.d("ChapterContentRepo", "Chapters directory: $absolutePath")
        }
    }

    // Сохраняем контент главы в файл
    suspend fun saveChapterContent(bookId: Long, chapterNumber: Int, content: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "book_${bookId}_chapter_$chapterNumber.txt"
                val file = File(chaptersDir, fileName)

                file.writeText(content, Charsets.UTF_8)
                Log.d("ChapterContentRepo", "Saved chapter $chapterNumber to: ${file.absolutePath}")

                file.absolutePath
            } catch (e: Exception) {
                Log.e("ChapterContentRepo", "Error saving chapter content: ${e.message}")
                throw e
            }
        }
    }

    // Загружаем контент главы из файла
    suspend fun loadChapterContent(bookId: Long, chapterNumber: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "book_${bookId}_chapter_$chapterNumber.txt"
                val file = File(chaptersDir, fileName)

                if (file.exists()) {
                    val content = file.readText(Charsets.UTF_8)
                    Log.d("ChapterContentRepo", "Loaded chapter $chapterNumber from cache")
                    content
                } else {
                    Log.d("ChapterContentRepo", "Chapter $chapterNumber not found in cache")
                    null
                }
            } catch (e: Exception) {
                Log.e("ChapterContentRepo", "Error loading chapter content: ${e.message}")
                null
            }
        }
    }

    // Кешируем соседние главы
    suspend fun cacheAdjacentChapters(
        bookId: Long,
        currentChapter: Int,
        chapters: List<com.yume.reader.domain.models.epub.EpubChapter>
    ) {
        withContext(Dispatchers.IO) {
            try {
                val currentIndex = chapters.indexOfFirst { it.chapterNumber == currentChapter }
                if (currentIndex == -1) return@withContext

                // Определяем диапазон для кеширования
                val start = maxOf(0, currentIndex - 2)
                val end = minOf(chapters.size - 1, currentIndex + 2)

                for (i in start..end) {
                    val chapter = chapters[i]
                    val fileName = "book_${bookId}_chapter_${chapter.chapterNumber}.txt"
                    val file = File(chaptersDir, fileName)

                    if (!file.exists()) {
                        file.writeText(chapter.content, Charsets.UTF_8)
                        Log.d("ChapterContentRepo", "Cached chapter ${chapter.chapterNumber}")
                    }
                }

                // Очищаем старые кешированные главы
                cleanupOldChapters(bookId, currentChapter)
            } catch (e: Exception) {
                Log.e("ChapterContentRepo", "Error caching chapters: ${e.message}")
            }
        }
    }

    private fun cleanupOldChapters(bookId: Long, currentChapter: Int) {
        try {
            val prefix = "book_${bookId}_chapter_"
            chaptersDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(prefix)) {
                    val chapterNum = file.name.removePrefix(prefix).removeSuffix(".txt").toIntOrNull()
                    chapterNum?.let {
                        if (Math.abs(it - currentChapter) > CACHE_SIZE) {
                            file.delete()
                            Log.d("ChapterContentRepo", "Cleaned up old chapter $it")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChapterContentRepo", "Error cleaning up chapters: ${e.message}")
        }
    }

    // Удаляем все файлы книги
    suspend fun deleteBookChapters(bookId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val prefix = "book_${bookId}_chapter_"
                chaptersDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(prefix)) {
                        file.delete()
                    }
                }
                Log.d("ChapterContentRepo", "Deleted all chapters for book $bookId")
            } catch (e: Exception) {
                Log.e("ChapterContentRepo", "Error deleting book chapters: ${e.message}")
            }
        }
    }

    // Проверяем, есть ли глава в кеше
    suspend fun hasChapterInCache(bookId: Long, chapterNumber: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val fileName = "book_${bookId}_chapter_$chapterNumber.txt"
            File(chaptersDir, fileName).exists()
        }
    }

    // Получаем список закешированных глав
    suspend fun getCachedChapters(bookId: Long): List<Int> {
        return withContext(Dispatchers.IO) {
            val prefix = "book_${bookId}_chapter_"
            chaptersDir.listFiles()?.mapNotNull { file ->
                if (file.name.startsWith(prefix)) {
                    file.name.removePrefix(prefix).removeSuffix(".txt").toIntOrNull()
                } else null
            } ?: emptyList()
        }
    }
}
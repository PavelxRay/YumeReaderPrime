package com.yume.reader.data.local.database

import android.util.Log
import com.yume.reader.data.local.entity.BookEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class DatabaseInitializer @Inject constructor(
    private val database: AppDatabase
) {

    fun populateDatabase() {
        Log.d("DatabaseInit", "Функция populateDatabase вызвана")
        CoroutineScope(Dispatchers.IO).launch {
            val bookDao = database.bookDao()
            Log.d("DatabaseInit", "Получен bookDao")

            // Проверяем, есть ли уже книги
            val count = bookDao.getReadingBooksCount()

            // Если база пустая, добавляем тестовые данные
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

            testBooks.forEach { bookDao.insertBook(it) }
        }
    }
}
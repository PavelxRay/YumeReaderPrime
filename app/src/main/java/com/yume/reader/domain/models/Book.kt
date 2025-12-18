package com.yume.reader.domain.models

import java.util.Date

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val description: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val progress: Int = 0,
    val isReading: Boolean = false,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    val addedDate: Date = Date(),
    val lastReadDate: Date? = null,
    val filePath: String? = null,
    val fileFormat: String? = null,
)

// Функции преобразования
fun Book.toEntity(): com.yume.reader.data.local.entity.BookEntity {
    return com.yume.reader.data.local.entity.BookEntity(
        id = id,
        title = title,
        author = author,
        coverUrl = coverUrl,
        description = description,
        totalPages = totalPages,
        currentPage = currentPage,
        progress = progress,
        isReading = isReading,
        isFinished = isFinished,
        isFavorite = isFavorite,
        addedDate = addedDate,
        lastReadDate = lastReadDate,
        filePath = filePath,
        fileFormat = fileFormat,
    )
}

fun com.yume.reader.data.local.entity.BookEntity.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        coverUrl = coverUrl,
        description = description,
        totalPages = totalPages,
        currentPage = currentPage,
        progress = progress,
        isReading = isReading,
        isFinished = isFinished,
        isFavorite = isFavorite,
        addedDate = addedDate,
        lastReadDate = lastReadDate,
        filePath = filePath,
        fileFormat = fileFormat,
    )
}
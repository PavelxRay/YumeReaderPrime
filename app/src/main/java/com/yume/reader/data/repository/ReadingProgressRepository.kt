// data/repository/ReadingProgressRepository.kt
package com.yume.reader.data.repository

import com.yume.reader.data.local.dao.ReadingProgressDao
import com.yume.reader.data.models.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingProgressRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao
) {

    fun getReadingProgress(bookId: Long): Flow<ReadingProgress?> {
        return readingProgressDao.getReadingProgress(bookId)
    }

    suspend fun saveReadingProgress(progress: ReadingProgress) {
        readingProgressDao.insertReadingProgress(progress)
    }
}
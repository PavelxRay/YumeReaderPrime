// data/local/dao/ReadingProgressDao.kt
package com.yume.reader.data.local.dao

import androidx.room.*
import com.yume.reader.data.models.ReadingProgress
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId")
    fun getReadingProgress(bookId: Long): Flow<ReadingProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingProgress(progress: ReadingProgress)

    @Update
    suspend fun updateReadingProgress(progress: ReadingProgress)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteReadingProgress(bookId: Long)

    @Query("UPDATE reading_progress SET currentChapter = :chapter, progressPercent = :progress, lastReadAt = :timestamp WHERE bookId = :bookId")
    suspend fun updateProgress(bookId: Long, chapter: Int, progress: Float, timestamp: LocalDateTime)
}
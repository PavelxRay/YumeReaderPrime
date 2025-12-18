package com.yume.reader.data.local.dao

import androidx.room.*
import com.yume.reader.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ReadingSessionDao {

    @Insert
    suspend fun insertSession(session: ReadingSessionEntity)

    @Update
    suspend fun updateSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE book_id = :bookId ORDER BY start_time DESC")
    fun getSessionsByBookId(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE start_time >= :startDate AND start_time < :endDate")
    fun getSessionsBetweenDates(startDate: Date, endDate: Date): Flow<List<ReadingSessionEntity>>

    @Query("SELECT SUM(duration_minutes) FROM reading_sessions WHERE start_time >= :startDate AND start_time < :endDate")
    fun getTotalReadingMinutesBetweenDates(startDate: Date, endDate: Date): Flow<Int?>
}
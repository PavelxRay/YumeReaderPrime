package com.yume.reader.data.local.dao

import androidx.room.*
import com.yume.reader.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY chapter_number")
    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE book_id = :bookId AND chapter_number = :chapterNumber")
    suspend fun getChapter(bookId: Long, chapterNumber: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET is_read = :isRead WHERE id = :chapterId")
    suspend fun updateReadStatus(chapterId: Long, isRead: Boolean)

    @Query("SELECT COUNT(*) FROM chapters WHERE book_id = :bookId AND is_read = 1")
    suspend fun getReadChaptersCount(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM chapters WHERE book_id = :bookId")
    suspend fun getTotalChaptersCount(bookId: Long): Int

    // Дополнительные методы для отладки
    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()

    @Query("SELECT COUNT(*) FROM chapters")
    suspend fun getTotalChaptersCountAll(): Int

    // УБЕРИТЕ ЭТОТ ДУБЛИРУЮЩИЙ МЕТОД:
    // @Insert(onConflict = OnConflictStrategy.REPLACE)
    // suspend fun insertChapter(chapter: ChapterEntity): Long
}
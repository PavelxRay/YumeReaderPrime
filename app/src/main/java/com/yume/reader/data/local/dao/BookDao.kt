package com.yume.reader.data.local.dao

import androidx.room.*
import com.yume.reader.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY last_read_date DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE is_reading = 1 ORDER BY last_read_date DESC")
    fun getReadingBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE is_finished = 1 ORDER BY last_read_date DESC")
    fun getFinishedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE is_favorite = 1 ORDER BY added_date DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET current_page = :page, progress = :progress, last_read_date = CURRENT_TIMESTAMP WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, page: Int, progress: Int)

    @Query("UPDATE books SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE books SET is_reading = :isReading WHERE id = :id")
    suspend fun updateReadingStatus(id: Long, isReading: Boolean)

    @Query("SELECT COUNT(*) FROM books WHERE is_finished = 1")
    fun getFinishedBooksCount(): Flow<Int>

    @Query("SELECT SUM(total_pages) FROM books WHERE is_finished = 1")
    fun getTotalPagesRead(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getTotalBooksCount(): Int

    @Query("SELECT COUNT(*) FROM books WHERE is_reading = 1")
    fun getReadingBooksCount(): Flow<Int>
}
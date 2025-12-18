package com.yume.reader.data.local.entity

import androidx.room.*

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("book_id")]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false
) {
    companion object {
        fun fromEpubChapter(bookId: Long, epubChapter: com.yume.reader.domain.models.epub.EpubChapter): ChapterEntity {
            return ChapterEntity(
                bookId = bookId,
                chapterNumber = epubChapter.chapterNumber,
                title = epubChapter.title,
                content = epubChapter.content,
                wordCount = epubChapter.wordCount,
                durationMinutes = calculateReadingTime(epubChapter.wordCount),
                isRead = false
            )
        }

        private fun calculateReadingTime(wordCount: Int): Int {
            return kotlin.math.max(1, (wordCount / 180.0).toInt())
        }
    }
}
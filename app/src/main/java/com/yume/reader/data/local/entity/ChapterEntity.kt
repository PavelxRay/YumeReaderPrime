package com.yume.reader.data.local.entity

import androidx.room.*
import androidx.room.ForeignKey.Companion.CASCADE

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],          // Столбец в родительской таблице BookEntity
            childColumns = ["book_id"],      // Столбец в этой таблице (ChapterEntity)
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"]),
        Index(value = ["book_id", "chapter_number"], unique = true)
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "book_id")
    val bookId: Long,

    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int,

    val title: String,
    val content: String,

    @ColumnInfo(name = "word_count")
    val wordCount: Int = 0,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0,

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false
)
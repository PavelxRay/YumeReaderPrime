package com.yume.reader.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "book_id")
    val bookId: Long? = null,

    @ColumnInfo(name = "start_time")
    val startTime: Date = Date(),

    @ColumnInfo(name = "end_time")
    val endTime: Date? = null,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int = 0,

    @ColumnInfo(name = "pages_read")
    val pagesRead: Int = 0,
)
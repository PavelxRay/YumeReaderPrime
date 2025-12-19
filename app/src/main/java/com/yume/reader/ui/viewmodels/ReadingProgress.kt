// data/models/ReadingProgress.kt
package com.yume.reader.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey val bookId: Long,
    val currentChapter: Int = 1,
    val progressPercent: Float = 0f,
    val lastReadAt: LocalDateTime = LocalDateTime.now(),
    val totalChapters: Int = 0
)
package com.yume.reader.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "author")
    val author: String,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "total_pages")
    val totalPages: Int = 0,

    @ColumnInfo(name = "current_page")
    val currentPage: Int = 0,

    @ColumnInfo(name = "progress")
    val progress: Int = 0,

    @ColumnInfo(name = "is_reading")
    val isReading: Boolean = false,

    @ColumnInfo(name = "is_finished")
    val isFinished: Boolean = false,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "added_date")
    val addedDate: Date = Date(),

    @ColumnInfo(name = "last_read_date")
    val lastReadDate: Date? = null,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "file_format")
    val fileFormat: String? = null,
)
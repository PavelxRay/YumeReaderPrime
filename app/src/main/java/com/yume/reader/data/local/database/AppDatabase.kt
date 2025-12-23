// data/local/database/AppDatabase.kt
package com.yume.reader.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yume.reader.data.local.dao.*
import com.yume.reader.data.local.database.converters.DateConverter
import com.yume.reader.data.local.database.converters.LocalDateTimeConverter
import com.yume.reader.data.local.entity.BookEntity
import com.yume.reader.data.local.entity.ChapterEntity
import com.yume.reader.data.local.entity.ReadingSessionEntity
import com.yume.reader.data.models.ReadingProgress
import com.yume.reader.data.models.TextSettings

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ReadingSessionEntity::class,
        TextSettings::class,
        ReadingProgress::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(DateConverter::class, LocalDateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun textSettingsDao(): TextSettingsDao
    abstract fun readingProgressDao(): ReadingProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yume_reader.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
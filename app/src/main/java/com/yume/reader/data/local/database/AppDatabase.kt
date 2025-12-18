package com.yume.reader.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yume.reader.data.local.dao.BookDao
import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.dao.ReadingSessionDao
import com.yume.reader.data.local.database.converters.DateConverter
import com.yume.reader.data.local.entity.BookEntity
import com.yume.reader.data.local.entity.ChapterEntity
import com.yume.reader.data.local.entity.ReadingSessionEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ReadingSessionEntity::class
    ],
    version = 3,  // Увеличьте версию до 3
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d("AppDatabase", "Запрос на получение базы данных (v3)")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yume_reader.db"
                )
                    .fallbackToDestructiveMigration()  // Это удалит старую базу
                    .build()
                Log.d("AppDatabase", "Создан новый экземпляр базы (v3)")
                INSTANCE = instance
                instance
            }
        }
    }
}
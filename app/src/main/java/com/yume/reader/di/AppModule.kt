// di/AppModule.kt
package com.yume.reader.di

import android.content.Context
import com.yume.reader.data.epub.EpubParser
import com.yume.reader.data.local.dao.BookDao
import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.dao.ReadingSessionDao
import com.yume.reader.data.local.database.AppDatabase
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ChapterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Контекст приложения
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    // База данных
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    // DAO объекты
    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao {
        return database.bookDao()
    }

    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao {
        return database.chapterDao()
    }

    @Provides
    @Singleton
    fun provideReadingSessionDao(database: AppDatabase): ReadingSessionDao {
        return database.readingSessionDao()
    }

    // Репозитории
    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        chapterDao: ChapterDao,
        @ApplicationContext context: Context
    ): BookRepository {
        return BookRepository(bookDao, chapterDao, context)
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        chapterDao: ChapterDao
    ): ChapterRepository {
        return ChapterRepository(chapterDao)
    }

    // EPUB парсер
    @Provides
    @Singleton
    fun provideEpubParser(@ApplicationContext context: Context): EpubParser {
        return EpubParser(context)
    }
}
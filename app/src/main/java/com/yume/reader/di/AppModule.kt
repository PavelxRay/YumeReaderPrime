package com.yume.reader.di

import android.content.Context
import com.yume.reader.data.epub.EpubParser
import com.yume.reader.data.local.dao.*
import com.yume.reader.data.local.database.AppDatabase
import com.yume.reader.data.repository.*
import com.yume.reader.ui.viewmodels.BookDetailsViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()

    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

    @Provides
    @Singleton
    fun provideReadingSessionDao(database: AppDatabase): ReadingSessionDao = database.readingSessionDao()

    @Provides
    @Singleton
    fun provideTextSettingsDao(database: AppDatabase): TextSettingsDao = database.textSettingsDao()

    @Provides
    @Singleton
    fun provideReadingProgressDao(database: AppDatabase): ReadingProgressDao = database.readingProgressDao()

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        chapterDao: ChapterDao,
        readingProgressDao: ReadingProgressDao,
        chapterContentRepo: ChapterContentRepository, // Добавляем
        @ApplicationContext context: Context
    ): BookRepository = BookRepository(bookDao, chapterDao, readingProgressDao, chapterContentRepo, context)

    @Provides
    @Singleton
    fun provideChapterContentRepository(
        @ApplicationContext context: Context
    ): ChapterContentRepository = ChapterContentRepository(context)

    @Provides
    @Singleton
    fun provideChapterRepository(chapterDao: ChapterDao): ChapterRepository = ChapterRepository(chapterDao)

    @Provides
    @Singleton
    fun provideReadingProgressRepository(
        readingProgressDao: ReadingProgressDao
    ): ReadingProgressRepository = ReadingProgressRepository(readingProgressDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        textSettingsDao: TextSettingsDao
    ): SettingsRepository = SettingsRepositoryImpl(textSettingsDao)

    @Provides
    @Singleton
    fun provideEpubParser(@ApplicationContext context: Context): EpubParser = EpubParser(context)

    // Дополнительные ViewModel провайдеры
    @Provides
    fun provideBookDetailsViewModel(
        bookRepository: BookRepository,
        chapterRepository: ChapterRepository,
        readingProgressRepository: ReadingProgressRepository
    ): BookDetailsViewModel {
        return BookDetailsViewModel(bookRepository, chapterRepository, readingProgressRepository)
    }
}
package com.yume.reader.di

import android.content.Context
import coil.ImageLoader as CoilImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.yume.reader.data.epub.EpubParser
import com.yume.reader.data.images.BookImageLoader // Обновляем импорт
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

    // Опционально: если нужен Coil ImageLoader для UI
    @Provides
    @Singleton
    fun provideCoilImageLoader(@ApplicationContext context: Context): CoilImageLoader {
        return CoilImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideBookImageLoader(@ApplicationContext context: Context): BookImageLoader {
        return BookImageLoader(context)
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        bookDao: BookDao,
        chapterDao: ChapterDao,
        readingProgressDao: ReadingProgressDao,
        chapterContentRepo: ChapterContentRepository,
        @ApplicationContext context: Context,
        bookImageLoader: BookImageLoader // Обновляем имя параметра
    ): BookRepository = BookRepository(bookDao, chapterDao, readingProgressDao, chapterContentRepo, context, bookImageLoader)

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

    @Provides
    fun provideBookDetailsViewModel(
        bookRepository: BookRepository,
        chapterRepository: ChapterRepository,
        readingProgressRepository: ReadingProgressRepository
    ): BookDetailsViewModel {
        return BookDetailsViewModel(bookRepository, chapterRepository, readingProgressRepository)
    }
}
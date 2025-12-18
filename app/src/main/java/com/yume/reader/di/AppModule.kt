package com.yume.reader.di

import android.content.Context
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideBookRepository(
        database: AppDatabase
    ): BookRepository {
        return BookRepository(
            bookDao = database.bookDao(),
            chapterDao = database.chapterDao()  // Добавьте эту строку
        )
    }

    @Provides
    @Singleton
    fun provideChapterRepository(
        database: AppDatabase
    ): ChapterRepository {
        return ChapterRepository(
            chapterDao = database.chapterDao()
        )
    }
}
// data/repository/ChapterRepository.kt
package com.yume.reader.data.repository

import com.yume.reader.data.local.dao.ChapterDao
import com.yume.reader.data.local.entity.ChapterEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepository @Inject constructor(
    private val chapterDao: ChapterDao
) {

    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>> =
        chapterDao.getChaptersByBookId(bookId)

    suspend fun getChapter(bookId: Long, chapterNumber: Int): ChapterEntity? =
        chapterDao.getChapter(bookId, chapterNumber)

    suspend fun updateReadStatus(chapterId: Long, isRead: Boolean) {
        chapterDao.updateReadStatus(chapterId, isRead)
    }
}
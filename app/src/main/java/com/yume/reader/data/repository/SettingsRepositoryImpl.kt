// data/repository/SettingsRepositoryImpl.kt
package com.yume.reader.data.repository

import com.yume.reader.data.local.dao.TextSettingsDao
import com.yume.reader.data.models.TextSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val textSettingsDao: TextSettingsDao
) : SettingsRepository {

    override fun getTextSettings(bookId: Long): Flow<TextSettings> {
        return textSettingsDao.getTextSettings(bookId).map { settings ->
            settings ?: TextSettings(bookId = bookId)
        }
    }

    override suspend fun updateTextSettings(bookId: Long, settings: TextSettings) {
        textSettingsDao.insertTextSettings(settings.copy(bookId = bookId))
    }

    override suspend fun getGlobalSettings(): Flow<TextSettings> {
        return textSettingsDao.getTextSettings(-1).map { settings ->
            settings ?: TextSettings(bookId = -1)
        }
    }
}
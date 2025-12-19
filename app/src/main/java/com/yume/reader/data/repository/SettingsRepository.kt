// data/repository/SettingsRepository.kt
package com.yume.reader.data.repository

import com.yume.reader.data.models.TextSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getTextSettings(bookId: Long): Flow<TextSettings>
    suspend fun updateTextSettings(bookId: Long, settings: TextSettings)
    suspend fun getGlobalSettings(): Flow<TextSettings>
}
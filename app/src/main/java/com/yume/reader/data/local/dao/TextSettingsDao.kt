// data/local/dao/TextSettingsDao.kt
package com.yume.reader.data.local.dao

import androidx.room.*
import com.yume.reader.data.models.TextSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface TextSettingsDao {
    @Query("SELECT * FROM text_settings WHERE bookId = :bookId")
    fun getTextSettings(bookId: Long): Flow<TextSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextSettings(settings: TextSettings)

    @Update
    suspend fun updateTextSettings(settings: TextSettings)

    @Query("DELETE FROM text_settings WHERE bookId = :bookId")
    suspend fun deleteTextSettings(bookId: Long)
}
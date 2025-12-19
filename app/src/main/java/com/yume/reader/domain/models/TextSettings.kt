// data/models/TextSettings.kt
package com.yume.reader.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_settings")
data class TextSettings(
    @PrimaryKey val bookId: Long = -1, // -1 для глобальных настроек
    val fontSize: Float = 16f,
    val lineHeight: Float = 1.5f,
    val fontFamily: String = "Georgia",
    val theme: String = "light",
    val brightness: Float = 1f,
    val margins: Float = 16f,
    val paragraphSpacing: Float = 8f
)

// или для общих настроек
@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 0,
    val textSettings: TextSettings = TextSettings(),
    val autoSaveProgress: Boolean = true,
    val swipeGestures: Boolean = true
)
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
    val theme: String = "system", // Для глобальных: light, dark, system
    val brightness: Float = 1f,
    val margins: Float = 16f,
    val paragraphSpacing: Float = 8f
) {
    // Функция для создания настроек по умолчанию для книги
    companion object {
        fun defaultForBook(bookId: Long): TextSettings {
            return TextSettings(bookId = bookId)
        }
    }
}
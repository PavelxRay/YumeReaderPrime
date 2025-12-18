package com.yume.reader.data.local.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Создаем новую таблицу с правильной структурой
        database.execSQL("""
            CREATE TABLE chapters_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                book_id INTEGER NOT NULL,
                chapter_number INTEGER NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                word_count INTEGER NOT NULL DEFAULT 0,
                duration_minutes INTEGER NOT NULL DEFAULT 0,
                is_read INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE
            )
        """)

        // Копируем данные из старой таблицы в новую (если есть)
        database.execSQL("""
            INSERT INTO chapters_new (id, book_id, chapter_number, title, content, word_count, duration_minutes, is_read)
            SELECT id, book_id, chapter_number, title, content, word_count, duration_minutes, is_read 
            FROM chapters
        """)

        // Удаляем старую таблицу
        database.execSQL("DROP TABLE chapters")

        // Переименовываем новую таблицу
        database.execSQL("ALTER TABLE chapters_new RENAME TO chapters")

        // Создаем индекс
        database.execSQL("CREATE INDEX index_chapters_book_id ON chapters (book_id)")
    }
}
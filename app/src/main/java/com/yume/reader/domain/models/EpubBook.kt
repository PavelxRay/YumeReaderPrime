// domain/models/EpubBook.kt
package com.yume.reader.domain.models.epub

data class EpubBook(
    val title: String,
    val author: String,
    val description: String,
    val coverImage: ByteArray? = null,
    val chapters: List<EpubChapter>,
    val metadata: EpubMetadata,
    val filePath: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EpubBook
        return filePath == other.filePath
    }

    override fun hashCode(): Int = filePath.hashCode()
}

data class EpubChapter(
    val id: String,
    val title: String,
    val content: String,
    val rawHtml: String,
    val chapterNumber: Int,
    val wordCount: Int = 0,
    val images: List<ChapterImage> = emptyList() // Добавляем изображения
)

data class ChapterImage(
    val id: String,
    val src: String,
    val altText: String? = null,
    val position: Int = 0,
    val data: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChapterImage
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class EpubMetadata(
    val language: String = "ru",
    val publisher: String? = null,
    val publishedDate: String? = null,
    val isbn: String? = null,
    val totalPages: Int = 0
)
// data/epub/EpubParser.kt
package com.yume.reader.data.epub

import android.content.Context
import com.yume.reader.domain.models.epub.EpubBook
import com.yume.reader.domain.models.epub.EpubChapter
import com.yume.reader.domain.models.epub.EpubMetadata
import nl.siegmann.epublib.domain.*
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpubParser @Inject constructor(
    private val context: Context
) {

    fun parseEpub(inputStream: InputStream, filePath: String): EpubBook {
        val epubReader = EpubReader()
        val epubLibBook = epubReader.readEpub(inputStream)
        val metadata = epubLibBook.metadata

        return EpubBook(
            title = extractTitle(metadata),
            author = extractAuthor(metadata),
            description = extractDescription(metadata),
            coverImage = extractCoverImage(epubLibBook),
            chapters = extractChapters(epubLibBook),
            metadata = extractMetadata(metadata, epubLibBook),
            filePath = filePath
        )
    }

    private fun extractTitle(metadata: Metadata): String {
        return metadata.titles.firstOrNull() ?: "Без названия"
    }

    private fun extractAuthor(metadata: Metadata): String {
        return metadata.authors.joinToString { author ->
            "${author.firstname ?: ""} ${author.lastname ?: ""}".trim().ifEmpty {
                author.toString()
            }
        }.ifEmpty { "Неизвестный автор" }
    }

    private fun extractDescription(metadata: Metadata): String {
        return metadata.descriptions.firstOrNull() ?: ""
    }

    private fun extractCoverImage(book: Book): ByteArray? {
        return try {
            book.coverImage?.data
        } catch (e: Exception) {
            null
        }
    }

    private fun extractChapters(book: Book): List<EpubChapter> {
        val chapters = mutableListOf<EpubChapter>()
        val tableOfContents = book.tableOfContents

        // Рекурсивно обходим все элементы оглавления
        fun processTocItems(tocItems: List<TOCReference>, chapterCounter: IntArray) {
            for (tocItem in tocItems) {
                val resource = tocItem.resource
                if (resource != null) {
                    chapterCounter[0]++ // Увеличиваем счетчик глав

                    val htmlContent = try {
                        String(resource.data, Charsets.UTF_8)
                    } catch (e: Exception) {
                        ""
                    }

                    val plainText = htmlToPlainText(htmlContent)

                    chapters.add(
                        EpubChapter(
                            id = resource.id,
                            title = tocItem.title ?: "Глава ${chapterCounter[0]}",
                            content = plainText,
                            rawHtml = htmlContent,
                            chapterNumber = chapterCounter[0],
                            wordCount = plainText.split("\\s+".toRegex()).size
                        )
                    )
                }

                // Обрабатываем вложенные элементы
                if (tocItem.children.isNotEmpty()) {
                    processTocItems(tocItem.children, chapterCounter)
                }
            }
        }

        processTocItems(tableOfContents.tocReferences, intArrayOf(0))
        return chapters
    }

    private fun htmlToPlainText(html: String): String {
        return try {
            Jsoup.parse(html).text()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "")
        }
    }

    private fun extractMetadata(metadata: Metadata, book: Book): EpubMetadata {
        // Получаем язык - используем metadata.language вместо metadata.languages
        val language = metadata.language ?: "ru"

        // Получаем издателя - используем первый publisher как строку
        val publisher = metadata.publishers.firstOrNull()?.toString()

        // Получаем дату публикации - используем первый объект Date
        val publishedDate = metadata.dates.firstOrNull()?.toString()

        // Получаем ISBN
        val isbn = metadata.identifiers
            .firstOrNull { it.scheme?.equals("ISBN", ignoreCase = true) == true }?.value

        return EpubMetadata(
            language = language,
            publisher = publisher,
            publishedDate = publishedDate,
            isbn = isbn,
            totalPages = book.tableOfContents.tocReferences.size
        )
    }
}
// data/epub/EpubParser.kt
package com.yume.reader.data.epub

import android.content.Context
import android.util.Log
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
        Log.d("EpubParser", "🚀 Начало парсинга EPUB: $filePath")

        val epubReader = EpubReader()
        val epubLibBook = epubReader.readEpub(inputStream)
        val metadata = epubLibBook.metadata

        Log.d("EpubParser", "📖 Заголовок книги: ${metadata.titles.firstOrNull()}")
        Log.d("EpubParser", "👤 Автор: ${metadata.authors.joinToString()}")
        Log.d("EpubParser", "🔢 Количество глав в оглавлении: ${epubLibBook.tableOfContents.tocReferences.size}")

        val chapters = extractChapters(epubLibBook)

        return EpubBook(
            title = extractTitle(metadata),
            author = extractAuthor(metadata),
            description = extractDescription(metadata),
            coverImage = extractCoverImage(epubLibBook),
            chapters = chapters,
            metadata = extractMetadata(metadata, epubLibBook),
            filePath = filePath
        )
    }

    private fun extractTitle(metadata: Metadata): String {
        return metadata.titles.firstOrNull()?.trim() ?: "Без названия"
    }

    private fun extractAuthor(metadata: Metadata): String {
        return metadata.authors.joinToString { author ->
            "${author.firstname ?: ""} ${author.lastname ?: ""}".trim()
        }.trim().ifEmpty { "Неизвестный автор" }
    }

    private fun extractDescription(metadata: Metadata): String {
        return metadata.descriptions.firstOrNull()?.trim() ?: ""
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

        Log.d("EpubParser", "📋 Начало извлечения глав...")
        Log.d("EpubParser", "📑 Элементов в оглавлении: ${tableOfContents.tocReferences.size}")

        // Рекурсивно обходим все элементы оглавления
        fun processTocItems(
            tocItems: List<TOCReference>,
            chapterCounter: IntArray,
            processedResources: MutableSet<String>
        ) {
            for (tocItem in tocItems) {
                val resource = tocItem.resource
                if (resource != null && resource.id !in processedResources) {
                    processedResources.add(resource.id)

                    try {
                        // Получаем содержимое ресурса
                        val htmlContent = String(resource.data, Charsets.UTF_8)

                        // Проверяем тип ресурса
                        val mediaType = resource.mediaType?.toString()?.lowercase() ?: ""

                        // Проверяем, является ли это реальной главой с текстом
                        if (shouldProcessAsChapter(resource, tocItem.title ?: "", htmlContent)) {
                            chapterCounter[0]++

                            Log.d("EpubParser", "📖 Обработка главы ${chapterCounter[0]}: ${tocItem.title}")

                            // Извлекаем текст с очисткой от дублирующихся заголовков
                            val (cleanText, filteredTitle) = extractAndCleanText(
                                html = htmlContent,
                                chapterTitle = tocItem.title ?: "Глава ${chapterCounter[0]}",
                                resourceId = resource.id
                            )

                            val wordCount = cleanText.split("\\s+".toRegex()).size

                            // Добавляем главу только если есть текст и это не обложка
                            if (cleanText.isNotBlank() && !isCoverOrMetadata(filteredTitle)) {
                                chapters.add(
                                    EpubChapter(
                                        id = resource.id,
                                        title = filteredTitle,
                                        content = cleanText,
                                        rawHtml = htmlContent,
                                        chapterNumber = chapterCounter[0],
                                        wordCount = wordCount
                                    )
                                )
                                Log.d("EpubParser", "✅ Добавлена глава '$filteredTitle' с $wordCount словами")
                            } else {
                                Log.w("EpubParser", "⚠️ Пропущена глава '${tocItem.title}' - мало текста или обложка")
                                chapterCounter[0]-- // Уменьшаем счетчик, так как главу не добавили
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("EpubParser", "❌ Ошибка обработки ресурса: ${e.message}")
                    }
                }

                // Обрабатываем вложенные элементы
                if (tocItem.children.isNotEmpty()) {
                    processTocItems(tocItem.children, chapterCounter, processedResources)
                }
            }
        }

        val chapterCounter = intArrayOf(0)
        val processedResources = mutableSetOf<String>()
        processTocItems(tableOfContents.tocReferences, chapterCounter, processedResources)

        // Если главы найдены, фильтруем пустые и ненужные
        val filteredChapters = chapters.filter {
                chapter ->
            chapter.content.isNotBlank() &&
                    !isCoverOrMetadata(chapter.title) &&
                    chapter.content.length > 50
        }

        // Перенумеруем главы после фильтрации
        val finalChapters = filteredChapters.mapIndexed { index, chapter ->
            chapter.copy(chapterNumber = index + 1)
        }

        Log.d("EpubParser", "🎉 Извлечено глав: ${finalChapters.size}")
        return finalChapters
    }

    private fun shouldProcessAsChapter(resource: Resource, title: String, htmlContent: String): Boolean {
        // Проверяем media type
        val mediaType = resource.mediaType?.toString()?.lowercase() ?: ""
        return mediaType.contains("html") || mediaType.contains("xhtml") ||
                resource.href.endsWith(".html", ignoreCase = true) ||
                resource.href.endsWith(".xhtml", ignoreCase = true)
    }

    private fun isCoverOrMetadata(title: String): Boolean {
        val lowerTitle = title.lowercase()
        return lowerTitle.contains("cover") ||
                lowerTitle.contains("обложка") ||
                lowerTitle.contains("титул") ||
                lowerTitle.contains("title") ||
                lowerTitle.contains("copyright") ||
                lowerTitle.contains("авторские права") ||
                lowerTitle.matches(Regex("^\\d+.*$")) // Начинается с цифр (номера страниц)
    }

    private fun extractAndCleanText(
        html: String,
        chapterTitle: String,
        resourceId: String
    ): Pair<String, String> {
        return try {
            val doc = Jsoup.parse(html)

            // Удаляем скрипты и стили
            doc.select("script, style, meta, link").remove()

            // Находим основной контент
            val body = doc.body()

            // Получаем весь текст
            val fullText = body.text().trim()

            // Извлекаем заголовок из текста (если есть)
            val extractedTitle = extractTitleFromText(fullText, chapterTitle)

            // Очищаем текст от дублирующегося заголовка
            val cleanText = removeDuplicateTitle(fullText, extractedTitle)

            Pair(cleanText, extractedTitle)

        } catch (e: Exception) {
            Log.e("EpubParser", "❌ Ошибка при обработке HTML: ${e.message}")
            // Fallback
            Pair(htmlToPlainText(html), chapterTitle)
        }
    }

    private fun extractTitleFromText(text: String, fallbackTitle: String): String {
        // Пытаемся извлечь заголовок из первых строк текста
        val lines = text.split("\n").take(3).map { it.trim() }

        for (line in lines) {
            if (line.isNotBlank() && line.length > 10 && line.length < 100) {
                // Проверяем, что строка похожа на заголовок
                if (!line.matches(Regex(".*\\d+/\\d+.*")) && // Не содержит номеров страниц
                    !line.matches(Regex(".*\\d{1,2}:\\d{2}.*")) && // Не содержит время
                    !line.matches(Regex("^[\\s\\d.:-]+$"))) { // Не только цифры и символы
                    return line
                }
            }
        }

        return fallbackTitle
    }

    private fun removeDuplicateTitle(text: String, title: String): String {
        var result = text

        // Удаляем заголовок из начала текста
        if (title.isNotBlank()) {
            // Удаляем точное совпадение в начале
            if (result.startsWith(title)) {
                result = result.substring(title.length).trimStart()
            }

            // Удаляем с разными окончаниями (точка, двоеточие, тире)
            val patterns = listOf(
                "$title.",
                "$title:",
                "$title -",
                "$title —",
                "$title\n"
            )

            for (pattern in patterns) {
                if (result.startsWith(pattern)) {
                    result = result.substring(pattern.length).trimStart()
                }
            }

            // Удаляем из первых строк, если встречается
            val lines = result.split("\n").toMutableList()
            if (lines.isNotEmpty() && lines[0].contains(title)) {
                lines.removeAt(0)
                result = lines.joinToString("\n")
            }
        }

        // Удаляем номера страниц (34/577)
        result = result.replace(Regex("\\d+/\\d+"), "")

        // Удаляем временные метки (12:30)
        result = result.replace(Regex("\\d{1,2}:\\d{2}"), "")

        // Удаляем лишние пробелы и пустые строки
        result = result.replace(Regex("\\s+"), " ")
            .replace(Regex("\\n\\s*\\n+"), "\n\n")
            .trim()

        return result
    }

    private fun htmlToPlainText(html: String): String {
        return try {
            val doc = Jsoup.parse(html)
            doc.select("script, style").remove()
            val text = doc.body().text()
            text.replace(Regex("\\s+"), " ")
                .replace(Regex("\\n\\s*\\n"), "\n\n")
                .trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private fun extractMetadata(metadata: Metadata, book: Book): EpubMetadata {
        return EpubMetadata(
            language = metadata.language ?: "ru",
            publisher = metadata.publishers.firstOrNull()?.toString(),
            publishedDate = metadata.dates.firstOrNull()?.toString(),
            isbn = metadata.identifiers
                .firstOrNull { it.scheme?.equals("ISBN", ignoreCase = true) == true }?.value,
            totalPages = book.tableOfContents.tocReferences.size
        )
    }
}
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
                Log.d("EpubParser", "📄 Обработка элемента оглавления: ${tocItem.title}")

                val resource = tocItem.resource
                if (resource != null) {
                    Log.d("EpubParser", "   📦 Ресурс найден: ${resource.id}, href: ${resource.href}")

                    if (resource.id !in processedResources) {
                        processedResources.add(resource.id)

                        try {
                            // Получаем содержимое ресурса
                            val htmlContent = String(resource.data, Charsets.UTF_8)
                            Log.d("EpubParser", "   📝 Длина HTML: ${htmlContent.length} символов")

                            // Проверяем тип ресурса
                            val mediaType = resource.mediaType?.toString() ?: "unknown"
                            Log.d("EpubParser", "   🏷️ Media type: $mediaType")

                            // Проверяем, является ли это HTML-ресурсом
                            if (mediaType.contains("html", ignoreCase = true) ||
                                mediaType.contains("xhtml", ignoreCase = true) ||
                                resource.href.endsWith(".html", ignoreCase = true) ||
                                resource.href.endsWith(".xhtml", ignoreCase = true)) {

                                chapterCounter[0]++

                                Log.d("EpubParser", "   🔍 Извлечение текста...")

                                // Простейший способ извлечь текст
                                val text = extractTextSimple(htmlContent)

                                Log.d("EpubParser", "   📊 Извлечено символов: ${text.length}")

                                if (text.isNotBlank()) {
                                    chapters.add(
                                        EpubChapter(
                                            id = resource.id,
                                            title = tocItem.title ?: "Глава ${chapterCounter[0]}",
                                            content = text,
                                            rawHtml = htmlContent,
                                            chapterNumber = chapterCounter[0],
                                            wordCount = text.split("\\s+".toRegex()).size
                                        )
                                    )
                                    Log.d("EpubParser", "   ✅ Глава добавлена: ${tocItem.title}")
                                } else {
                                    Log.w("EpubParser", "   ⚠️ Текст пустой, но глава добавлена")
                                    chapters.add(
                                        EpubChapter(
                                            id = resource.id,
                                            title = tocItem.title ?: "Глава ${chapterCounter[0]}",
                                            content = "Текст главы не найден или пуст",
                                            rawHtml = htmlContent,
                                            chapterNumber = chapterCounter[0],
                                            wordCount = 0
                                        )
                                    )
                                }
                            } else {
                                Log.d("EpubParser", "   ⏭️ Пропущен не-HTML ресурс: $mediaType")
                            }
                        } catch (e: Exception) {
                            Log.e("EpubParser", "   ❌ Ошибка обработки ресурса ${resource.id}: ${e.message}")
                        }
                    } else {
                        Log.d("EpubParser", "   🔄 Ресурс уже обработан: ${resource.id}")
                    }
                } else {
                    Log.w("EpubParser", "   ❌ У элемента нет ресурса: ${tocItem.title}")
                }

                // Обрабатываем вложенные элементы
                if (tocItem.children.isNotEmpty()) {
                    Log.d("EpubParser", "   📂 Вложенные элементы: ${tocItem.children.size}")
                    processTocItems(tocItem.children, chapterCounter, processedResources)
                }
            }
        }

        val chapterCounter = intArrayOf(0)
        val processedResources = mutableSetOf<String>()
        processTocItems(tableOfContents.tocReferences, chapterCounter, processedResources)

        Log.d("EpubParser", "🎉 Извлечено глав: ${chapters.size}")

        // Если глав нет, попробуем получить все HTML ресурсы
        if (chapters.isEmpty()) {
            Log.d("EpubParser", "⚠️ Главы не найдены через оглавление, пробуем все ресурсы...")
            extractAllHtmlResources(book, chapters)
        }

        return chapters
    }

    private fun extractAllHtmlResources(book: Book, chapters: MutableList<EpubChapter>) {
        val resources = book.resources.all
        Log.d("EpubParser", "🔍 Поиск HTML ресурсов среди всех ${resources.size} ресурсов")

        var chapterNumber = 1

        for (resource in resources) {
            val mediaType = resource.mediaType?.toString() ?: "unknown"
            val href = resource.href

            if (mediaType.contains("html", ignoreCase = true) ||
                mediaType.contains("xhtml", ignoreCase = true) ||
                href.endsWith(".html", ignoreCase = true) ||
                href.endsWith(".xhtml", ignoreCase = true)) {

                Log.d("EpubParser", "   📄 Найден HTML ресурс: $href, type: $mediaType")

                try {
                    val htmlContent = String(resource.data, Charsets.UTF_8)
                    val text = extractTextSimple(htmlContent)

                    if (text.isNotBlank()) {
                        // Пробуем извлечь заголовок из HTML
                        val title = extractTitleFromHtml(htmlContent) ?: "Глава $chapterNumber"

                        chapters.add(
                            EpubChapter(
                                id = resource.id,
                                title = title,
                                content = text,
                                rawHtml = htmlContent,
                                chapterNumber = chapterNumber,
                                wordCount = text.split("\\s+".toRegex()).size
                            )
                        )
                        Log.d("EpubParser", "   ✅ Добавлена глава: $title")
                        chapterNumber++
                    }
                } catch (e: Exception) {
                    Log.e("EpubParser", "   ❌ Ошибка обработки ресурса $href: ${e.message}")
                }
            }
        }
    }

    private fun extractTextSimple(html: String): String {
        return try {
            val doc = Jsoup.parse(html)

            // Самый простой способ - весь текст из body
            val text = doc.body().text()

            // Базовая очистка
            text.replace(Regex("\\s+"), " ")
                .replace(Regex("\\n\\s*\\n"), "\n\n")
                .trim()
        } catch (e: Exception) {
            Log.e("EpubParser", "Ошибка JSoup: ${e.message}")
            // Fallback: удаляем теги
            html.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }

    private fun extractTitleFromHtml(html: String): String? {
        return try {
            val doc = Jsoup.parse(html)
            // Ищем заголовок
            doc.selectFirst("h1, h2, h3, .title, .chapter-title")?.text()?.trim()
        } catch (e: Exception) {
            null
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
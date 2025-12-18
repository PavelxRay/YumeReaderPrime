// ui/viewmodels/ImportViewModel.kt
package com.yume.reader.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.epub.EpubParser
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.domain.models.epub.EpubBook
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val epubParser: EpubParser,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Состояние импорта
    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // Импорт EPUB файла по URI
    fun importEpubFromUri(uri: Uri, fileName: String) {
        _importState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Получаем InputStream из URI
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        throw Exception("Не удалось открыть файл")
                    }

                    inputStream.use { stream ->
                        // Парсим EPUB
                        val epubBook = epubParser.parseEpub(stream, fileName)

                        // Проверяем, не импортирована ли книга уже
                        val exists = bookRepository.doesBookExist(fileName)
                        if (exists) {
                            throw Exception("Книга уже добавлена в библиотеку")
                        }

                        // Импортируем в базу данных
                        val bookId = bookRepository.importEpubBook(epubBook)

                        // Обновляем состояние
                        _importState.update {
                            it.copy(
                                isLoading = false,
                                currentBook = epubBook,
                                importedBookId = bookId,
                                importProgress = 100
                            )
                        }

                        Log.d("ImportViewModel", "Книга импортирована: ${epubBook.title}, ID: $bookId")
                    }
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка импорта: ${e.message ?: "Неизвестная ошибка"}"
                    )
                }
                Log.e("ImportViewModel", "Ошибка импорта: ${e.message}", e)
            }
        }
    }

    // Импорт EPUB файла по пути
    fun importEpubFromPath(filePath: String) {
        _importState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    if (!file.exists()) {
                        throw Exception("Файл не найден: $filePath")
                    }

                    file.inputStream().use { stream ->
                        val epubBook = epubParser.parseEpub(stream, filePath)

                        // Проверяем, не импортирована ли книга уже
                        val exists = bookRepository.doesBookExist(filePath)
                        if (exists) {
                            throw Exception("Книга уже добавлена в библиотеку")
                        }

                        val bookId = bookRepository.importEpubBook(epubBook)

                        _importState.update {
                            it.copy(
                                isLoading = false,
                                currentBook = epubBook,
                                importedBookId = bookId,
                                importProgress = 100
                            )
                        }

                        Log.d("ImportViewModel", "Книга импортирована по пути: ${epubBook.title}")
                    }
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка импорта: ${e.message ?: "Неизвестная ошибка"}"
                    )
                }
                Log.e("ImportViewModel", "Ошибка импорта по пути: ${e.message}", e)
            }
        }
    }

    // Пакетный импорт нескольких файлов
    fun importMultipleEpubFiles(filePaths: List<String>) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val importedBooks = mutableListOf<Long>()

                    filePaths.forEachIndexed { index, filePath ->
                        try {
                            val progress = ((index + 1).toFloat() / filePaths.size * 100).toInt()

                            _importState.update {
                                it.copy(
                                    isLoading = true,
                                    importProgress = progress,
                                    currentStatus = "Импорт ${index + 1} из ${filePaths.size}"
                                )
                            }

                            val file = File(filePath)
                            if (file.exists()) {
                                file.inputStream().use { stream ->
                                    val epubBook = epubParser.parseEpub(stream, filePath)

                                    if (!bookRepository.doesBookExist(filePath)) {
                                        val bookId = bookRepository.importEpubBook(epubBook)
                                        importedBooks.add(bookId)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ImportViewModel", "Ошибка импорта $filePath: ${e.message}")
                        }
                    }

                    _importState.update {
                        it.copy(
                            isLoading = false,
                            importedBookIds = importedBooks,
                            importProgress = 100,
                            currentStatus = "Импорт завершен"
                        )
                    }
                }
            } catch (e: Exception) {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка пакетного импорта: ${e.message}"
                    )
                }
            }
        }
    }

    // Сканирование папки на наличие EPUB файлов
    suspend fun scanDirectoryForEpub(directoryPath: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val files = bookRepository.scanForEpubFiles(directoryPath)
                files.map { it.absolutePath }
            } catch (e: Exception) {
                Log.e("ImportViewModel", "Ошибка сканирования: ${e.message}")
                emptyList()
            }
        }
    }

    // Получение информации о книге без импорта (предпросмотр)
    suspend fun previewEpubBook(filePath: String): EpubBook? {
        return withContext(Dispatchers.IO) {
            try {
                File(filePath).inputStream().use { stream ->
                    epubParser.parseEpub(stream, filePath)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // Сброс состояния импорта
    fun resetState() {
        _importState.value = ImportState()
    }

    // Получение файла по URI
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            // Для file:// URI
            if (uri.scheme == "file") {
                File(uri.path ?: return null)
            }
            // Для content:// URI (например, из файлового менеджера)
            else {
                // Можно реализовать получение реального пути из ContentResolver
                // Для упрощения возвращаем null
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Состояние импорта
data class ImportState(
    val isLoading: Boolean = false,
    val currentBook: EpubBook? = null,
    val importedBookId: Long? = null,
    val importedBookIds: List<Long> = emptyList(),
    val errorMessage: String? = null,
    val importProgress: Int = 0,
    val currentStatus: String = "Готов к импорту"
)
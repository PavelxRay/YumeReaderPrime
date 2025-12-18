package com.yume.reader

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.yume.reader.data.repository.BookRepository
import com.yume.reader.data.repository.ChapterRepository
import com.yume.reader.ui.navigation.YumeReaderNavigation
import com.yume.reader.ui.theme.YumeReaderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bookRepository: BookRepository

    @Inject
    lateinit var chapterRepository: ChapterRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Запускаем инициализацию в фоне
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("MainActivity", "🔧 Начинаем инициализацию базы...")

            // Шаг 1: Инициализируем книги
            bookRepository.initializeIfEmpty()

            // Небольшая задержка
            delay(500)

            // Шаг 2: Добавляем тестовые главы
            bookRepository.addTestChapters()

            Log.d("MainActivity", "✅ Инициализация базы завершена")
        }

        setContent {
            YumeReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YumeReaderNavigation()
                }
            }
        }
    }
}
// ui/screens/ImportEpubScreen.kt
package com.yume.reader.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.yume.reader.ui.viewmodels.ImportViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImportEpubScreen(
    navController: NavController,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val importState by viewModel.importState.collectAsState()
    val context = LocalContext.current

    // Разрешение на чтение файлов
    val storagePermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Запуск файлового менеджера
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                // Получаем имя файла из URI
                val fileName = context.contentResolver.query(
                    uri, null, null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex("_display_name")
                        if (displayNameIndex != -1) {
                            cursor.getString(displayNameIndex)
                        } else {
                            "book.epub"
                        }
                    } else {
                        "book.epub"
                    }
                } ?: "book.epub"

                viewModel.importEpubFromUri(uri, fileName)
            }
        }
    )

    // Сброс состояния при входе на экран
    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    // Навигация после успешного импорта
    LaunchedEffect(importState.importedBookId) {
        importState.importedBookId?.let { bookId ->
            // Навигация к экрану чтения
            navController.navigate("reader/$bookId") {
                popUpTo("library") { saveState = true }
                launchSingleTop = true
            }
            // Сброс состояния после навигации
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт EPUB") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Основной контент
                when {
                    importState.isLoading -> {
                        LoadingContent(importState)
                    }

                    importState.errorMessage != null -> {
                        ErrorContent(importState.errorMessage!!) {
                            viewModel.resetState()
                        }
                    }

                    importState.importedBookId != null -> {
                        SuccessContent(importState.currentBook) {
                            navController.popBackStack()
                        }
                    }

                    else -> {
                        IdleContent {
                            if (storagePermissionState.status == PermissionStatus.Granted) {
                                filePickerLauncher.launch("application/epub+zip")
                            } else {
                                storagePermissionState.launchPermissionRequest()
                            }
                        }
                    }
                }

                // Информация о текущей книге
                importState.currentBook?.let { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Информация о книге",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "Название: ${book.title}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "Автор: ${book.author}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = "Глав: ${book.chapters.size}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleContent(onImportClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.FolderOpen,
            contentDescription = "Импорт",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Добавьте книгу в формате EPUB",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Поддерживаются файлы с расширением .epub\nВыберите файл для импорта в вашу библиотеку",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = true
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать EPUB файл")
        }
    }
}

@Composable
private fun LoadingContent(state: com.yume.reader.ui.viewmodels.ImportState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = state.currentStatus,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.importProgress > 0) {
            LinearProgressIndicator(
                progress = state.importProgress / 100f,
                modifier = Modifier.fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${state.importProgress}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorContent(errorMessage: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Ошибка",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ошибка импорта",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onRetry
            ) {
                Text("Попробовать снова")
            }
        }
    }
}

@Composable
private fun SuccessContent(book: com.yume.reader.domain.models.epub.EpubBook?, onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Успех",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Книга успешно импортирована!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        book?.let {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = it.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${it.chapters.size} глав",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onContinue
            ) {
                Text("Вернуться в библиотеку")
            }
        }
    }
}
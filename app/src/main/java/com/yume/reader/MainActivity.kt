package com.yume.reader

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yume.reader.ui.navigation.YumeReaderNavigation
import com.yume.reader.ui.theme.YumeReaderTheme
import com.yume.reader.ui.viewmodels.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Получаем настройки темы из ViewModel
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val appTheme by settingsViewModel.appTheme.collectAsState()

            YumeReaderTheme(
                appTheme = appTheme // Передаем выбранную тему
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    YumeReaderNavigation()
                }
            }
        }
    }
}
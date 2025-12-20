// ui/viewmodels/SettingsViewModel.kt
package com.yume.reader.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yume.reader.data.models.TextSettings
import com.yume.reader.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _appTheme = MutableStateFlow("system") // system, light, dark
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    init {
        loadGlobalTheme()
    }

    private fun loadGlobalTheme() {
        viewModelScope.launch {
            settingsRepository.getGlobalSettings().collect { globalSettings ->
                // Берем только тему из глобальных настроек
                _appTheme.value = globalSettings.theme
            }
        }
    }

    fun updateAppTheme(theme: String) {
        _appTheme.value = theme
        saveGlobalTheme(theme)
    }

    private fun saveGlobalTheme(theme: String) {
        viewModelScope.launch {
            // Получаем текущие глобальные настройки
            val currentSettings = settingsRepository.getGlobalSettings()
                .firstOrNull() ?: TextSettings(bookId = -1)

            // Обновляем только тему
            val updatedSettings = currentSettings.copy(
                theme = theme,
                bookId = -1
            )

            settingsRepository.saveGlobalSettings(updatedSettings)
        }
    }
}
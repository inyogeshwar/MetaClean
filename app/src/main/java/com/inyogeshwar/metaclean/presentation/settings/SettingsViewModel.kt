package com.inyogeshwar.metaclean.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsState

    fun setTheme(theme: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(theme)
        }
    }

    fun setExportFormat(format: ExportFormat) {
        viewModelScope.launch {
            settingsRepository.updateExportFormat(format)
        }
    }

    fun setNamingSuffix(suffix: String) {
        viewModelScope.launch {
            settingsRepository.updateOutputNamingSuffix(suffix)
        }
    }

    fun setConfirmBeforeProcessing(confirm: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateConfirmBeforeProcessing(confirm)
        }
    }

    fun setAutoClearTemp(autoClear: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoClearTempFiles(autoClear)
        }
    }

    fun setSaveDestinationMode(mode: SaveDestinationMode) {
        viewModelScope.launch {
            settingsRepository.updateSaveDestinationMode(mode)
        }
    }

    fun setAutoSaveOnPick(autoSave: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoSaveOnPick(autoSave)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun clearTempFilesNow(context: Context) {
        FileUtils.clearTempFiles(context)
    }
}

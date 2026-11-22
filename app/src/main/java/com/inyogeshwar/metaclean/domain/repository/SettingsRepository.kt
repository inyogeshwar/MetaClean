package com.inyogeshwar.metaclean.domain.repository

import kotlinx.coroutines.flow.StateFlow

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class ExportFormat {
    ORIGINAL,
    JPEG,
    PNG,
    WEBP
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val exportFormat: ExportFormat = ExportFormat.ORIGINAL,
    val outputNamingSuffix: String = "_clean",
    val confirmBeforeProcessing: Boolean = true,
    val autoClearTempFiles: Boolean = true
)

interface SettingsRepository {
    val settingsState: StateFlow<AppSettings>
    suspend fun updateThemeMode(themeMode: AppThemeMode)
    suspend fun updateExportFormat(exportFormat: ExportFormat)
    suspend fun updateOutputNamingSuffix(suffix: String)
    suspend fun updateConfirmBeforeProcessing(confirm: Boolean)
    suspend fun updateAutoClearTempFiles(autoClear: Boolean)
}

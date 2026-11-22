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

enum class SaveDestinationMode {
    GALLERY_ALBUM,     // Save to public Gallery (Pictures/MetaClean) - safe, instant, no overwrite
    OVERWRITE_ORIGINAL // Overwrite the original image in place
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val exportFormat: ExportFormat = ExportFormat.ORIGINAL,
    val outputNamingSuffix: String = "_clean",
    val confirmBeforeProcessing: Boolean = false,
    val autoClearTempFiles: Boolean = true,
    val saveDestinationMode: SaveDestinationMode = SaveDestinationMode.GALLERY_ALBUM,
    val autoSaveOnPick: Boolean = true
)

interface SettingsRepository {
    val settingsState: StateFlow<AppSettings>
    suspend fun updateThemeMode(themeMode: AppThemeMode)
    suspend fun updateExportFormat(exportFormat: ExportFormat)
    suspend fun updateOutputNamingSuffix(suffix: String)
    suspend fun updateConfirmBeforeProcessing(confirm: Boolean)
    suspend fun updateAutoClearTempFiles(autoClear: Boolean)
    suspend fun updateSaveDestinationMode(mode: SaveDestinationMode)
    suspend fun updateAutoSaveOnPick(autoSave: Boolean)
}

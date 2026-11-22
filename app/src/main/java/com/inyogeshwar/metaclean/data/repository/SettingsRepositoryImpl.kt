package com.inyogeshwar.metaclean.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.inyogeshwar.metaclean.domain.repository.AppSettings
import com.inyogeshwar.metaclean.domain.repository.AppThemeMode
import com.inyogeshwar.metaclean.domain.repository.ExportFormat
import com.inyogeshwar.metaclean.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("metaclean_prefs", Context.MODE_PRIVATE)

    private val _settingsState = MutableStateFlow(loadSettings())
    override val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val exportStr = prefs.getString("export_format", ExportFormat.ORIGINAL.name) ?: ExportFormat.ORIGINAL.name
        val suffix = prefs.getString("naming_suffix", "_clean") ?: "_clean"
        val confirm = prefs.getBoolean("confirm_before_processing", true)
        val autoClear = prefs.getBoolean("auto_clear_temp", true)

        return AppSettings(
            themeMode = try { AppThemeMode.valueOf(themeStr) } catch (e: Exception) { AppThemeMode.DARK },
            exportFormat = try { ExportFormat.valueOf(exportStr) } catch (e: Exception) { ExportFormat.ORIGINAL },
            outputNamingSuffix = suffix,
            confirmBeforeProcessing = confirm,
            autoClearTempFiles = autoClear
        )
    }

    override suspend fun updateThemeMode(themeMode: AppThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.name).apply()
        _settingsState.value = _settingsState.value.copy(themeMode = themeMode)
    }

    override suspend fun updateExportFormat(exportFormat: ExportFormat) {
        prefs.edit().putString("export_format", exportFormat.name).apply()
        _settingsState.value = _settingsState.value.copy(exportFormat = exportFormat)
    }

    override suspend fun updateOutputNamingSuffix(suffix: String) {
        prefs.edit().putString("naming_suffix", suffix).apply()
        _settingsState.value = _settingsState.value.copy(outputNamingSuffix = suffix)
    }

    override suspend fun updateConfirmBeforeProcessing(confirm: Boolean) {
        prefs.edit().putBoolean("confirm_before_processing", confirm).apply()
        _settingsState.value = _settingsState.value.copy(confirmBeforeProcessing = confirm)
    }

    override suspend fun updateAutoClearTempFiles(autoClear: Boolean) {
        prefs.edit().putBoolean("auto_clear_temp", autoClear).apply()
        _settingsState.value = _settingsState.value.copy(autoClearTempFiles = autoClear)
    }
}

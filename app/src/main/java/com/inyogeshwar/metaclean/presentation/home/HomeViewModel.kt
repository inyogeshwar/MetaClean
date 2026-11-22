package com.inyogeshwar.metaclean.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.repository.AppSettings
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import com.inyogeshwar.metaclean.domain.repository.QuickCleanResult
import com.inyogeshwar.metaclean.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickCleanUiState(
    val isRunning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val result: QuickCleanResult? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository,
    private val settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository
) : ViewModel() {

    val mediaQueue: StateFlow<List<MediaItem>> = metadataRepository.mediaQueue
    val settings: StateFlow<AppSettings> = settingsRepository.settingsState

    val historyRecords: StateFlow<List<HistoryRecord>> = historyRepository.getHistoryRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quickCleanState = MutableStateFlow(QuickCleanUiState())
    val quickCleanState: StateFlow<QuickCleanUiState> = _quickCleanState.asStateFlow()

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (settings.value.autoSaveOnPick) {
            quickCleanImages(uris)
        } else {
            viewModelScope.launch {
                metadataRepository.addMediaUris(uris)
            }
        }
    }

    fun quickCleanImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _quickCleanState.value = QuickCleanUiState(isRunning = true, current = 0, total = uris.size)
            val res = metadataRepository.quickCleanAndAutoSave(uris) { curr, tot ->
                _quickCleanState.value = _quickCleanState.value.copy(current = curr, total = tot)
            }
            _quickCleanState.value = QuickCleanUiState(isRunning = false, current = uris.size, total = uris.size, result = res)
        }
    }

    fun dismissQuickCleanResult() {
        _quickCleanState.value = QuickCleanUiState()
    }
}

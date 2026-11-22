package com.inyogeshwar.metaclean.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository,
    historyRepository: HistoryRepository
) : ViewModel() {

    val mediaQueue: StateFlow<List<MediaItem>> = metadataRepository.mediaQueue

    val historyRecords: StateFlow<List<HistoryRecord>> = historyRepository.getHistoryRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            metadataRepository.addMediaUris(uris)
        }
    }
}

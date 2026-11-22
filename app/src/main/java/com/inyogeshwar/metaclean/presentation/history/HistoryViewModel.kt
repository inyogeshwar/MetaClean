package com.inyogeshwar.metaclean.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val historyRecords: StateFlow<List<HistoryRecord>> = historyRepository.getHistoryRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryRecord(id)
        }
    }
}

package com.inyogeshwar.metaclean.presentation.batch

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.domain.model.CleanOptions
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.model.ProcessingStatus
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class BatchViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    val mediaQueue: StateFlow<List<MediaItem>> = metadataRepository.mediaQueue
    val isProcessing: StateFlow<Boolean> = metadataRepository.isProcessing

    fun toggleSelection(id: String) {
        metadataRepository.toggleItemSelected(id)
    }

    fun selectAll(select: Boolean) {
        metadataRepository.selectAll(select)
    }

    fun removeItem(id: String) {
        metadataRepository.removeMediaItem(id)
    }

    fun clearCompleted() {
        metadataRepository.clearCompleted()
    }

    fun clearQueue() {
        metadataRepository.clearQueue()
    }

    fun startBatchProcessing(options: CleanOptions = CleanOptions.ALL_PRIVACY) {
        viewModelScope.launch {
            metadataRepository.processAllSelected(options)
        }
    }

    fun cancelProcessing() {
        metadataRepository.cancelProcessing()
    }

    fun retryFailed(options: CleanOptions = CleanOptions.ALL_PRIVACY) {
        viewModelScope.launch {
            metadataRepository.retryFailed(options)
        }
    }

    fun exportZip(outputStream: OutputStream, onProgress: (Int, Int) -> Unit, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = metadataRepository.exportZipArchive(outputStream, onProgress)
            onComplete(success)
        }
    }

    fun exportSingleImage(itemId: String, destinationUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = metadataRepository.exportSingleImage(itemId, destinationUri)
            onComplete(success)
        }
    }

    fun selectItemForInspection(id: String) {
        metadataRepository.selectItemForInspection(id)
    }
}

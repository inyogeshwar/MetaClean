package com.inyogeshwar.metaclean.presentation.verification

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle["itemId"])

    val item: StateFlow<MediaItem?> = metadataRepository.mediaQueue.map { list ->
        list.find { it.id == itemId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun shareCleanedImage(context: Context) {
        val mediaItem = item.value ?: return
        val cleanedUri = mediaItem.cleanedUri ?: return

        try {
            val shareUri: Uri = if (cleanedUri.scheme == "file") {
                val file = File(cleanedUri.path!!)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                cleanedUri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mediaItem.mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Photo"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToDestination(destinationUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = metadataRepository.exportSingleImage(itemId, destinationUri)
            onComplete(success)
        }
    }
}

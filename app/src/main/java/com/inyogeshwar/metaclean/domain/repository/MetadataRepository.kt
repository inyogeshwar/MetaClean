package com.inyogeshwar.metaclean.domain.repository

import android.net.Uri
import com.inyogeshwar.metaclean.data.model.ImageMetadata
import com.inyogeshwar.metaclean.domain.model.CleanOptions
import com.inyogeshwar.metaclean.domain.model.MediaItem
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream

interface MetadataRepository {
    val mediaQueue: StateFlow<List<MediaItem>>
    val isProcessing: StateFlow<Boolean>
    val selectedItemForInspection: StateFlow<MediaItem?>

    suspend fun addMediaUris(uris: List<Uri>)
    fun removeMediaItem(id: String)
    fun toggleItemSelected(id: String)
    fun selectAll(select: Boolean)
    fun clearQueue()
    fun clearCompleted()
    fun selectItemForInspection(id: String?)

    suspend fun inspectItem(id: String): ImageMetadata?
    suspend fun processItem(id: String, options: CleanOptions)
    suspend fun processAllSelected(options: CleanOptions)
    fun cancelProcessing()
    suspend fun retryFailed(options: CleanOptions)

    suspend fun exportSingleImage(itemId: String, destinationUri: Uri): Boolean
    suspend fun exportZipArchive(outputStream: OutputStream, onProgress: (Int, Int) -> Unit): Boolean
}

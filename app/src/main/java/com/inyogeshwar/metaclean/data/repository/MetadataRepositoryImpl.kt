package com.inyogeshwar.metaclean.data.repository

import android.content.Context
import android.net.Uri
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.file.ZipUtils
import com.inyogeshwar.metaclean.core.image.MetadataExtractor
import com.inyogeshwar.metaclean.core.image.PrivacyCleaner
import com.inyogeshwar.metaclean.data.model.ImageMetadata
import com.inyogeshwar.metaclean.domain.model.CleanOptions
import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.model.ProcessingStatus
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import com.inyogeshwar.metaclean.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : MetadataRepository {

    private val _mediaQueue = MutableStateFlow<List<MediaItem>>(emptyList())
    override val mediaQueue: StateFlow<List<MediaItem>> = _mediaQueue.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    override val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _selectedItemForInspection = MutableStateFlow<MediaItem?>(null)
    override val selectedItemForInspection: StateFlow<MediaItem?> = _selectedItemForInspection.asStateFlow()

    private var processingJob: Job? = null

    override suspend fun addMediaUris(uris: List<Uri>) = withContext(Dispatchers.IO) {
        val newItems = uris.map { uri ->
            val fileName = FileUtils.getFileName(context, uri)
            val size = FileUtils.getFileSize(context, uri)
            val mime = FileUtils.getMimeType(context, uri)

            MediaItem(
                id = UUID.randomUUID().toString(),
                originalUri = uri,
                fileName = fileName,
                fileSizeBytes = size,
                formattedSize = FileUtils.formatFileSize(size),
                mimeType = mime,
                status = ProcessingStatus.PENDING,
                progress = 0f
            )
        }

        _mediaQueue.update { current ->
            // Prevent duplicate URIs
            val existingUris = current.map { it.originalUri }.toSet()
            val filtered = newItems.filter { it.originalUri !in existingUris }
            current + filtered
        }

        // If no item is currently selected for inspection, select the first newly added item
        if (_selectedItemForInspection.value == null && _mediaQueue.value.isNotEmpty()) {
            val firstId = _mediaQueue.value.first().id
            inspectItem(firstId)
        }
    }

    override fun removeMediaItem(id: String) {
        _mediaQueue.update { list -> list.filter { it.id != id } }
        if (_selectedItemForInspection.value?.id == id) {
            _selectedItemForInspection.value = _mediaQueue.value.firstOrNull()
        }
    }

    override fun toggleItemSelected(id: String) {
        _mediaQueue.update { list ->
            list.map { item ->
                if (item.id == id) item.copy(isSelected = !item.isSelected) else item
            }
        }
    }

    override fun selectAll(select: Boolean) {
        _mediaQueue.update { list ->
            list.map { it.copy(isSelected = select) }
        }
    }

    override fun clearQueue() {
        if (_isProcessing.value) {
            cancelProcessing()
        }
        _mediaQueue.value = emptyList()
        _selectedItemForInspection.value = null
        if (settingsRepository.settingsState.value.autoClearTempFiles) {
            FileUtils.clearTempFiles(context)
        }
    }

    override fun clearCompleted() {
        _mediaQueue.update { list ->
            list.filter { it.status != ProcessingStatus.COMPLETED }
        }
        if (_selectedItemForInspection.value?.status == ProcessingStatus.COMPLETED) {
            _selectedItemForInspection.value = _mediaQueue.value.firstOrNull()
        }
    }

    override fun selectItemForInspection(id: String?) {
        if (id == null) {
            _selectedItemForInspection.value = null
        } else {
            val item = _mediaQueue.value.find { it.id == id }
            _selectedItemForInspection.value = item
            if (item != null && item.originalMetadata == null) {
                CoroutineScope(Dispatchers.IO).launch {
                    inspectItem(id)
                }
            }
        }
    }

    override suspend fun inspectItem(id: String): ImageMetadata? = withContext(Dispatchers.IO) {
        val item = _mediaQueue.value.find { it.id == id } ?: return@withContext null

        updateItemStatus(id, ProcessingStatus.INSPECTING, 0.2f)
        try {
            val metadata = MetadataExtractor.extractMetadata(context, item.originalUri)
            _mediaQueue.update { list ->
                list.map {
                    if (it.id == id) {
                        it.copy(
                            originalMetadata = metadata,
                            width = metadata.fileInfo.width,
                            height = metadata.fileInfo.height,
                            status = if (it.status == ProcessingStatus.INSPECTING) ProcessingStatus.PENDING else it.status,
                            progress = 1.0f
                        )
                    } else it
                }
            }

            if (_selectedItemForInspection.value?.id == id) {
                _selectedItemForInspection.value = _mediaQueue.value.find { it.id == id }
            }
            metadata
        } catch (e: Exception) {
            updateItemError(id, "Failed to parse metadata: ${e.localizedMessage ?: "Unknown error"}")
            null
        }
    }

    override suspend fun processItem(id: String, options: CleanOptions) = withContext(Dispatchers.IO) {
        val item = _mediaQueue.value.find { it.id == id } ?: return@withContext
        updateItemStatus(id, ProcessingStatus.PROCESSING, 0.1f)

        try {
            val (cleanedFile, verification) = PrivacyCleaner.cleanImage(
                context = context,
                inputUri = item.originalUri,
                options = options,
                onProgress = { progress, statusMsg ->
                    updateItemProgress(id, progress)
                }
            )

            val cleanedUri = Uri.fromFile(cleanedFile)
            val cleanedMetadata = MetadataExtractor.extractMetadata(context, cleanedUri)

            _mediaQueue.update { list ->
                list.map {
                    if (it.id == id) {
                        it.copy(
                            status = ProcessingStatus.COMPLETED,
                            progress = 1f,
                            cleanedUri = cleanedUri,
                            cleanedMetadata = cleanedMetadata,
                            verificationResult = verification,
                            errorMessage = null
                        )
                    } else it
                }
            }

            if (_selectedItemForInspection.value?.id == id) {
                _selectedItemForInspection.value = _mediaQueue.value.find { it.id == id }
            }

        } catch (e: Exception) {
            updateItemError(id, "Processing failed: ${e.localizedMessage ?: "Corrupt or unsupported image"}")
        }
    }

    override suspend fun processAllSelected(options: CleanOptions) = withContext(Dispatchers.IO) {
        val itemsToProcess = _mediaQueue.value.filter { it.isSelected && it.status != ProcessingStatus.COMPLETED }
        if (itemsToProcess.isEmpty()) return@withContext

        _isProcessing.value = true
        var successCount = 0
        var failureCount = 0
        var totalBytesSaved: Long = 0
        val removedTagsSample = mutableSetOf<String>()

        processingJob = coroutineContext.job

        try {
            for (item in itemsToProcess) {
                if (!isActive) break

                processItem(item.id, options)

                val processed = _mediaQueue.value.find { it.id == item.id }
                if (processed?.status == ProcessingStatus.COMPLETED) {
                    successCount++
                    processed.verificationResult?.let { ver ->
                        totalBytesSaved += ver.sizeDifferenceBytes.coerceAtLeast(0L)
                        removedTagsSample.addAll(ver.removedFields)
                    }
                } else if (processed?.status == ProcessingStatus.FAILED) {
                    failureCount++
                }
            }

            // Record to Room history
            if (successCount > 0 || failureCount > 0) {
                val summary = if (removedTagsSample.isNotEmpty()) {
                    removedTagsSample.take(4).joinToString(", ") + if (removedTagsSample.size > 4) " +more" else ""
                } else {
                    "Privacy scrub complete"
                }

                historyRepository.saveHistoryRecord(
                    HistoryRecord(
                        timestamp = System.currentTimeMillis(),
                        formattedDate = "",
                        totalFiles = successCount + failureCount,
                        successCount = successCount,
                        failureCount = failureCount,
                        processingType = if (options == CleanOptions.ALL_PRIVACY) "Full Privacy Scrub" else "Custom Scrub",
                        removedTagsSummary = summary,
                        bytesSaved = totalBytesSaved
                    )
                )
            }
        } finally {
            _isProcessing.value = false
        }
    }

    override fun cancelProcessing() {
        processingJob?.cancel()
        _isProcessing.value = false
        _mediaQueue.update { list ->
            list.map {
                if (it.status == ProcessingStatus.PROCESSING || it.status == ProcessingStatus.INSPECTING) {
                    it.copy(status = ProcessingStatus.CANCELLED, progress = 0f)
                } else it
            }
        }
    }

    override suspend fun retryFailed(options: CleanOptions) = withContext(Dispatchers.IO) {
        val failed = _mediaQueue.value.filter { it.status == ProcessingStatus.FAILED || it.status == ProcessingStatus.CANCELLED }
        _mediaQueue.update { list ->
            list.map {
                if (it.status == ProcessingStatus.FAILED || it.status == ProcessingStatus.CANCELLED) {
                    it.copy(status = ProcessingStatus.PENDING, isSelected = true, errorMessage = null)
                } else it
            }
        }
        processAllSelected(options)
    }

    override suspend fun exportSingleImage(itemId: String, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val item = _mediaQueue.value.find { it.id == itemId } ?: return@withContext false
        val sourceUri = item.cleanedUri ?: item.originalUri
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    FileUtils.copyStream(input, output)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        false
    }

    override suspend fun exportZipArchive(
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val completedItems = _mediaQueue.value.filter { it.status == ProcessingStatus.COMPLETED && it.cleanedUri != null }
        if (completedItems.isEmpty()) return@withContext false

        val suffix = settingsRepository.settingsState.value.outputNamingSuffix
        val filesToZip = completedItems.map { item ->
            val ext = FileUtils.getExtensionFromMime(item.mimeType)
            val baseName = item.fileName.substringBeforeLast(".")
            val newName = "${baseName}${suffix}.${ext}"
            Pair(item.cleanedUri!!, newName)
        }

        ZipUtils.createZipArchive(context, filesToZip, outputStream, onProgress)
    }

    private fun updateItemStatus(id: String, status: ProcessingStatus, progress: Float) {
        _mediaQueue.update { list ->
            list.map {
                if (it.id == id) it.copy(status = status, progress = progress) else it
            }
        }
    }

    private fun updateItemProgress(id: String, progress: Float) {
        _mediaQueue.update { list ->
            list.map {
                if (it.id == id) it.copy(progress = progress) else it
            }
        }
    }

    private fun updateItemError(id: String, errorMsg: String) {
        _mediaQueue.update { list ->
            list.map {
                if (it.id == id) it.copy(status = ProcessingStatus.FAILED, errorMessage = errorMsg) else it
            }
        }
    }
}

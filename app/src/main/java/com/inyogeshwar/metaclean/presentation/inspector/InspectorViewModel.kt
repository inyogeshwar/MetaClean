package com.inyogeshwar.metaclean.presentation.inspector

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inyogeshwar.metaclean.data.model.ImageMetadata
import com.inyogeshwar.metaclean.domain.model.CleanOptions
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class InspectorViewModel @Inject constructor(
    private val metadataRepository: MetadataRepository
) : ViewModel() {

    val mediaQueue: StateFlow<List<MediaItem>> = metadataRepository.mediaQueue
    val selectedItem: StateFlow<MediaItem?> = metadataRepository.selectedItemForInspection

    fun selectItem(id: String) {
        metadataRepository.selectItemForInspection(id)
    }

    fun onSingleImageSelected(uri: Uri) {
        viewModelScope.launch {
            metadataRepository.addMediaUris(listOf(uri))
        }
    }

    fun cleanCurrentImage(options: CleanOptions = CleanOptions.ALL_PRIVACY) {
        val item = selectedItem.value ?: return
        viewModelScope.launch {
            metadataRepository.processItem(item.id, options)
        }
    }

    fun generateMetadataJson(metadata: ImageMetadata): String {
        val json = JSONObject()

        // File info
        val fileJson = JSONObject().apply {
            put("fileName", metadata.fileInfo.fileName)
            put("mimeType", metadata.fileInfo.mimeType)
            put("fileSizeBytes", metadata.fileInfo.fileSizeBytes)
            put("width", metadata.fileInfo.width)
            put("height", metadata.fileInfo.height)
            put("megapixels", metadata.fileInfo.megapixels)
            put("format", metadata.fileInfo.formatDescription)
        }
        json.put("fileInfo", fileJson)

        // EXIF
        val exifJson = JSONObject().apply {
            put("hasExif", metadata.exifData.hasExif)
            put("cameraMake", metadata.exifData.cameraMake)
            put("cameraModel", metadata.exifData.cameraModel)
            put("dateTimeOriginal", metadata.exifData.dateTimeOriginal)
            put("gpsLatitude", metadata.exifData.gpsLatitude)
            put("gpsLongitude", metadata.exifData.gpsLongitude)
            put("iso", metadata.exifData.iso)
            put("fNumber", metadata.exifData.fNumber)
            put("artist", metadata.exifData.artist)
            put("copyright", metadata.exifData.copyright)
        }
        json.put("exif", exifJson)

        // Hashes
        val techJson = JSONObject().apply {
            put("md5", metadata.technicalData.md5)
            put("sha1", metadata.technicalData.sha1)
            put("sha256", metadata.technicalData.sha256)
            put("crc32", metadata.technicalData.crc32)
            put("signature", metadata.technicalData.magicSignature)
        }
        json.put("technicalHashes", techJson)

        // Provenance
        val provJson = JSONObject().apply {
            put("hasProvenance", metadata.provenanceData.hasProvenance)
            put("isC2paDetected", metadata.provenanceData.isC2paDetected)
            put("aiSignals", metadata.provenanceData.aiSignals)
        }
        json.put("provenanceAndAiSignals", provJson)

        return json.toString(2)
    }
}

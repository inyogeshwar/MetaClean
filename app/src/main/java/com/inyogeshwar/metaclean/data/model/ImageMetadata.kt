package com.inyogeshwar.metaclean.data.model

data class ImageMetadata(
    val fileInfo: FileInfo,
    val exifData: ExifData,
    val iptcData: IptcData,
    val xmpData: XmpData,
    val pngData: PngData?,
    val iccData: IccData?,
    val provenanceData: ProvenanceData,
    val technicalData: TechnicalData
)

data class FileInfo(
    val fileName: String,
    val mimeType: String,
    val fileExtension: String,
    val fileSizeBytes: Long,
    val formattedFileSize: String,
    val width: Int,
    val height: Int,
    val megapixels: Double,
    val orientation: String,
    val formatDescription: String
)

data class ExifData(
    val hasExif: Boolean,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val software: String? = null,
    val dateTimeOriginal: String? = null,
    val dateTimeDigitized: String? = null,
    val offsetTime: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAltitude: Double? = null,
    val gpsCoordinateString: String? = null,
    val iso: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val focalLengthIn35mm: String? = null,
    val whiteBalance: String? = null,
    val flash: String? = null,
    val meteringMode: String? = null,
    val artist: String? = null,
    val copyright: String? = null,
    val userComment: String? = null,
    val hasThumbnail: Boolean = false,
    val allTags: Map<String, String> = emptyMap()
)

data class IptcData(
    val hasIptc: Boolean,
    val creator: String? = null,
    val copyrightNotice: String? = null,
    val caption: String? = null,
    val headline: String? = null,
    val keywords: List<String> = emptyList(),
    val contactInfo: String? = null,
    val city: String? = null,
    val country: String? = null
)

data class XmpData(
    val hasXmp: Boolean,
    val rawXmp: String? = null,
    val properties: Map<String, String> = emptyMap(),
    val namespaces: List<String> = emptyList()
)

data class PngChunk(
    val type: String,
    val length: Int,
    val description: String,
    val isCritical: Boolean
)

data class PngData(
    val hasPngChunks: Boolean,
    val bitDepth: Int? = null,
    val colorType: String? = null,
    val compressionMethod: String? = null,
    val filterMethod: String? = null,
    val interlaceMethod: String? = null,
    val chunks: List<PngChunk> = emptyList(),
    val textualData: Map<String, String> = emptyMap()
)

data class IccData(
    val hasProfile: Boolean,
    val profileName: String? = null,
    val colorSpace: String? = null,
    val profileSize: Int = 0,
    val profileClass: String? = null
)

data class ProvenanceData(
    val hasProvenance: Boolean,
    val isC2paDetected: Boolean,
    val isJumbfDetected: Boolean,
    val aiSignals: List<String> = emptyList(),
    val generatorSoftware: String? = null,
    val notes: String = "Informational and read-only. Provenance and Content Credentials cannot be modified."
)

data class TechnicalData(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val crc32: String,
    val magicSignature: String,
    val headerPreviewHex: String,
    val hexDumpPreview: String
)

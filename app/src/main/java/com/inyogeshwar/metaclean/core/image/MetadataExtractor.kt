package com.inyogeshwar.metaclean.core.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.file.FileUtils.readFully
import com.inyogeshwar.metaclean.core.file.FileUtils.skipFully
import com.inyogeshwar.metaclean.core.file.HashUtils
import com.inyogeshwar.metaclean.core.file.HexUtils
import com.inyogeshwar.metaclean.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MetadataExtractor {

    suspend fun extractMetadata(context: Context, uri: Uri, computeFullHashes: Boolean = true): ImageMetadata = withContext(Dispatchers.IO) {
        val fileName = FileUtils.getFileName(context, uri)
        val fileSizeBytes = FileUtils.getFileSize(context, uri)
        val formattedFileSize = FileUtils.formatFileSize(fileSizeBytes)
        val mimeType = FileUtils.getMimeType(context, uri)

        // Read first 512 bytes for header & format detection
        val headerBytes = ByteArray(512)
        var actualHeaderSize = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                actualHeaderSize = stream.read(headerBytes).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            // Ignore
        }
        val headerSlice = headerBytes.copyOf(actualHeaderSize)

        // Detect Format
        val detectedFormat = FormatDetector.detectFormat(headerSlice)

        // Read image bounds efficiently without full bitmap decode in memory
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
        } catch (e: Exception) {
            // Ignore
        }
        val width = boundsOptions.outWidth.coerceAtLeast(0)
        val height = boundsOptions.outHeight.coerceAtLeast(0)
        val megapixels = if (width > 0 && height > 0) {
            String.format("%.2f", (width.toDouble() * height.toDouble()) / 1_000_000.0).toDouble()
        } else 0.0

        // Hashes & Hex
        val checksums = if (computeFullHashes && fileSizeBytes < 100_000_000L) { // Only stream hash if file < 100MB
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    HashUtils.calculateHashes(stream)
                } ?: HashUtils.Checksums("N/A", "N/A", "N/A", "N/A")
            } catch (e: Exception) {
                HashUtils.Checksums("N/A", "N/A", "N/A", "N/A")
            }
        } else {
            HashUtils.Checksums("Pending inspection", "Pending inspection", "Pending inspection", "Pending inspection")
        }

        val magicSignature = HexUtils.formatHeaderHex(headerSlice, 8)
        val headerPreviewHex = HexUtils.formatHeaderHex(headerSlice, 16)
        val hexDumpPreview = HexUtils.formatHexDump(headerSlice, 256)

        // Technical Data
        val technicalData = TechnicalData(
            md5 = checksums.md5,
            sha1 = checksums.sha1,
            sha256 = checksums.sha256,
            crc32 = checksums.crc32,
            magicSignature = magicSignature,
            headerPreviewHex = headerPreviewHex,
            hexDumpPreview = hexDumpPreview
        )

        // EXIF Data
        val exifData = extractExifData(context, uri)

        // PNG Chunks if PNG
        val pngData = if (detectedFormat == ImageFormat.PNG) {
            extractPngData(context, uri)
        } else null

        // XMP, IPTC, ICC, Provenance inspection
        val (xmpData, iptcData, iccData, provenanceData) = inspectAdvancedMetadata(context, uri, detectedFormat, pngData)

        val fileInfo = FileInfo(
            fileName = fileName,
            mimeType = mimeType,
            fileExtension = FileUtils.getExtensionFromMime(mimeType),
            fileSizeBytes = fileSizeBytes,
            formattedFileSize = formattedFileSize,
            width = if (width > 0) width else (exifData.allTags[ExifInterface.TAG_IMAGE_WIDTH]?.toIntOrNull() ?: 0),
            height = if (height > 0) height else (exifData.allTags[ExifInterface.TAG_IMAGE_LENGTH]?.toIntOrNull() ?: 0),
            megapixels = megapixels,
            orientation = exifData.allTags[ExifInterface.TAG_ORIENTATION] ?: "1 (Normal)",
            formatDescription = detectedFormat.displayName
        )

        ImageMetadata(
            fileInfo = fileInfo,
            exifData = exifData,
            iptcData = iptcData,
            xmpData = xmpData,
            pngData = pngData,
            iccData = iccData,
            provenanceData = provenanceData,
            technicalData = technicalData
        )
    }

    private fun extractExifData(context: Context, uri: Uri): ExifData {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val allTags = mutableMapOf<String, String>()

                val tagsToCheck = listOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_LENS_MAKE,
                    ExifInterface.TAG_LENS_MODEL,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_OFFSET_TIME,
                    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, // ISO
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
                    ExifInterface.TAG_WHITE_BALANCE,
                    ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_METERING_MODE,
                    ExifInterface.TAG_ARTIST,
                    ExifInterface.TAG_COPYRIGHT,
                    ExifInterface.TAG_USER_COMMENT,
                    ExifInterface.TAG_IMAGE_DESCRIPTION,
                    ExifInterface.TAG_IMAGE_WIDTH,
                    ExifInterface.TAG_IMAGE_LENGTH,
                    ExifInterface.TAG_ORIENTATION
                )

                for (tag in tagsToCheck) {
                    val value = exif.getAttribute(tag)
                    if (!value.isNullOrBlank()) {
                        allTags[tag] = value
                    }
                }

                val latLong = exif.latLong
                val lat = latLong?.get(0)
                val lon = latLong?.get(1)
                val alt = exif.getAltitude(Double.NaN).let { if (it.isNaN()) null else it }

                val gpsString = if (lat != null && lon != null) {
                    val latDirection = if (lat >= 0) "N" else "S"
                    val lonDirection = if (lon >= 0) "E" else "W"
                    "%.5f° %s, %.5f° %s%s".format(
                        Math.abs(lat), latDirection,
                        Math.abs(lon), lonDirection,
                        alt?.let { " (Alt: %.1f m)".format(it) } ?: ""
                    )
                } else null

                val hasExif = allTags.isNotEmpty() || lat != null || exif.hasThumbnail()

                ExifData(
                    hasExif = hasExif,
                    cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE),
                    cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL),
                    lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL),
                    software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
                    dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                    dateTimeDigitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED),
                    offsetTime = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME),
                    gpsLatitude = lat,
                    gpsLongitude = lon,
                    gpsAltitude = alt,
                    gpsCoordinateString = gpsString,
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY),
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" },
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" },
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { "${it}mm" },
                    focalLengthIn35mm = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { "${it}mm eq." },
                    whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let {
                        if (it == "0") "Auto" else "Manual"
                    },
                    flash = exif.getAttribute(ExifInterface.TAG_FLASH),
                    meteringMode = exif.getAttribute(ExifInterface.TAG_METERING_MODE),
                    artist = exif.getAttribute(ExifInterface.TAG_ARTIST),
                    copyright = exif.getAttribute(ExifInterface.TAG_COPYRIGHT),
                    userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT),
                    hasThumbnail = exif.hasThumbnail(),
                    allTags = allTags
                )
            } ?: ExifData(hasExif = false)
        } catch (e: Exception) {
            ExifData(hasExif = false)
        }
    }

    /**
     * Bulletproof PNG chunk extraction.
     * Uses skipFully and readFully to guarantee no desynchronization or infinite loops on large files.
     */
    private fun extractPngData(context: Context, uri: Uri): PngData {
        val chunks = mutableListOf<PngChunk>()
        val textualData = mutableMapOf<String, String>()
        var bitDepth: Int? = null
        var colorType: String? = null
        var compressionMethod: String? = null
        var filterMethod: String? = null
        var interlaceMethod: String? = null

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val sig = ByteArray(8)
                if (!stream.readFully(sig) || !sig.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))) {
                    return PngData(false)
                }

                val lengthBuf = ByteArray(4)
                val typeBuf = ByteArray(4)

                var chunkCount = 0
                while (chunkCount < 100 && stream.readFully(lengthBuf) && stream.readFully(typeBuf)) {
                    chunkCount++
                    val length = ByteBuffer.wrap(lengthBuf).order(ByteOrder.BIG_ENDIAN).int
                    val type = String(typeBuf, Charsets.US_ASCII)

                    val isCritical = type.isNotEmpty() && type[0].isUpperCase()
                    val description = when (type) {
                        "IHDR" -> "Image Header (Dimensions, Bit depth, Color type)"
                        "PLTE" -> "Palette Table"
                        "IDAT" -> "Image Data Chunk"
                        "IEND" -> "Image Trailer / End of File"
                        "tEXt" -> "Uncompressed Textual Metadata"
                        "zTXt" -> "Compressed Textual Metadata"
                        "iTXt" -> "International Textual Metadata (XMP/C2PA/UTF-8)"
                        "iCCP" -> "Embedded ICC Color Profile"
                        "sRGB" -> "Standard RGB Color Space"
                        "gAMA" -> "Gamma Correction"
                        "cHRM" -> "Primary Chromaticities"
                        "pHYs" -> "Physical Pixel Dimensions"
                        "eXIf" -> "Embedded EXIF Metadata Chunk"
                        "caBX" -> "C2PA / Content Credentials Box"
                        "jumb" -> "JUMBF (JPEG Universal Metadata Box)"
                        "tIME" -> "Image Last Modification Time"
                        else -> "Auxiliary PNG Chunk ($type)"
                    }

                    chunks.add(PngChunk(type, length, description, isCritical))

                    // Parse chunk data if small (e.g. IHDR, tEXt, iTXt)
                    if (length in 1..65536 && type != "IDAT") {
                        val chunkData = ByteArray(length)
                        if (stream.readFully(chunkData)) {
                            if (type == "IHDR" && chunkData.size >= 13) {
                                bitDepth = chunkData[8].toInt() and 0xFF
                                colorType = when (chunkData[9].toInt() and 0xFF) {
                                    0 -> "Grayscale"
                                    2 -> "RGB Truecolor"
                                    3 -> "Indexed-color"
                                    4 -> "Grayscale with Alpha"
                                    6 -> "RGBA Truecolor with Alpha"
                                    else -> "Unknown (${chunkData[9]})"
                                }
                                compressionMethod = if (chunkData[10].toInt() == 0) "Deflate/Inflate" else "Unknown"
                                filterMethod = if (chunkData[11].toInt() == 0) "Adaptive filtering" else "Unknown"
                                interlaceMethod = if (chunkData[12].toInt() == 0) "Noninterlaced" else "Adam7 Interlaced"
                            } else if (type == "tEXt") {
                                val nullIndex = chunkData.indexOf(0.toByte())
                                if (nullIndex > 0) {
                                    val key = String(chunkData, 0, nullIndex, Charsets.ISO_8859_1)
                                    val value = String(chunkData, nullIndex + 1, chunkData.size - nullIndex - 1, Charsets.ISO_8859_1)
                                    textualData[key] = value
                                }
                            } else if (type == "iTXt") {
                                val nullIndex = chunkData.indexOf(0.toByte())
                                if (nullIndex > 0) {
                                    val key = String(chunkData, 0, nullIndex, Charsets.UTF_8)
                                    var idx = nullIndex + 3
                                    while (idx < chunkData.size && chunkData[idx] != 0.toByte()) idx++
                                    idx++
                                    while (idx < chunkData.size && chunkData[idx] != 0.toByte()) idx++
                                    idx++
                                    if (idx < chunkData.size) {
                                        val text = String(chunkData, idx, chunkData.size - idx, Charsets.UTF_8)
                                        textualData[key] = text.take(500)
                                    }
                                }
                            }
                        }
                        // Skip 4-byte CRC safely
                        stream.skipFully(4)
                    } else {
                        // Skip chunk payload + 4 bytes CRC safely
                        stream.skipFully(length.toLong() + 4)
                    }

                    if (type == "IEND") break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return PngData(
            hasPngChunks = chunks.isNotEmpty(),
            bitDepth = bitDepth,
            colorType = colorType,
            compressionMethod = compressionMethod,
            filterMethod = filterMethod,
            interlaceMethod = interlaceMethod,
            chunks = chunks,
            textualData = textualData
        )
    }

    private fun inspectAdvancedMetadata(
        context: Context,
        uri: Uri,
        format: ImageFormat,
        pngData: PngData?
    ): Quadruple<XmpData, IptcData, IccData, ProvenanceData> {
        // Read up to 256KB from the start of the file in a safe loop
        val sampleSize = 262144 // 256 KB
        val sampleBytes = ByteArray(sampleSize)
        var actualSampleRead = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                var total = 0
                while (total < sampleSize) {
                    val count = stream.read(sampleBytes, total, sampleSize - total)
                    if (count == -1) break
                    total += count
                }
                actualSampleRead = total
            }
        } catch (e: Exception) {
            // Ignore
        }

        val sampleSlice = sampleBytes.copyOf(actualSampleRead)
        val sampleText = String(sampleSlice, Charsets.ISO_8859_1)

        // 1. XMP Detection
        var hasXmp = false
        var rawXmp: String? = null
        val xmpProperties = mutableMapOf<String, String>()
        val namespaces = mutableListOf<String>()

        val xmpStart = sampleText.indexOf("<x:xmpmeta")
        if (xmpStart != -1) {
            val xmpEnd = sampleText.indexOf("</x:xmpmeta>", xmpStart)
            if (xmpEnd != -1) {
                hasXmp = true
                rawXmp = sampleText.substring(xmpStart, xmpEnd + 12)
            }
        } else if (pngData?.textualData?.containsKey("XML:com.adobe.xmp") == true) {
            hasXmp = true
            rawXmp = pngData.textualData["XML:com.adobe.xmp"]
        }

        if (hasXmp && rawXmp != null) {
            if (rawXmp.contains("xmlns:dc=")) namespaces.add("Dublin Core (dc)")
            if (rawXmp.contains("xmlns:xmp=")) namespaces.add("Adobe XMP Core (xmp)")
            if (rawXmp.contains("xmlns:photoshop=")) namespaces.add("Photoshop (photoshop)")
            if (rawXmp.contains("xmlns:crs=")) namespaces.add("Camera Raw Settings (crs)")
            if (rawXmp.contains("xmlns:c2pa=")) namespaces.add("Coalition for Content Provenance (c2pa)")

            val titleMatch = Regex("<dc:title>.*?<rdf:li>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(rawXmp)
            titleMatch?.groupValues?.get(1)?.let { xmpProperties["dc:title"] = it.trim() }

            val creatorMatch = Regex("<dc:creator>.*?<rdf:li>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(rawXmp)
            creatorMatch?.groupValues?.get(1)?.let { xmpProperties["dc:creator"] = it.trim() }

            val descriptionMatch = Regex("<dc:description>.*?<rdf:li>(.*?)</rdf:li>", RegexOption.DOT_MATCHES_ALL).find(rawXmp)
            descriptionMatch?.groupValues?.get(1)?.let { xmpProperties["dc:description"] = it.trim() }

            val createDateMatch = Regex("<xmp:CreateDate>(.*?)</xmp:CreateDate>").find(rawXmp)
            createDateMatch?.groupValues?.get(1)?.let { xmpProperties["xmp:CreateDate"] = it.trim() }
        }

        val xmpData = XmpData(
            hasXmp = hasXmp,
            rawXmp = rawXmp?.take(2000),
            properties = xmpProperties,
            namespaces = namespaces
        )

        // 2. IPTC Detection
        var hasIptc = false
        var creator: String? = null
        var copyright: String? = null
        var caption: String? = null
        val keywords = mutableListOf<String>()

        if (sampleText.contains("Photoshop 3.0") || sampleText.contains("8BIM")) {
            hasIptc = true
        }

        if (xmpProperties.containsKey("dc:creator")) {
            creator = xmpProperties["dc:creator"]
            hasIptc = true
        }
        if (xmpProperties.containsKey("dc:description")) {
            caption = xmpProperties["dc:description"]
            hasIptc = true
        }

        val iptcData = IptcData(
            hasIptc = hasIptc,
            creator = creator,
            copyrightNotice = copyright,
            caption = caption,
            keywords = keywords
        )

        // 3. ICC Color Profile
        var hasIcc = false
        var profileName: String? = null
        var colorSpace: String? = null
        var profileSize = 0

        if (sampleText.contains("ICC_PROFILE")) {
            hasIcc = true
            colorSpace = "RGB / Embedded ICC"
            profileName = "Embedded ICC Profile (JPEG marker)"
        } else if (pngData?.chunks?.any { it.type == "iCCP" || it.type == "sRGB" } == true) {
            hasIcc = true
            val iccpChunk = pngData.chunks.find { it.type == "iCCP" }
            if (iccpChunk != null) {
                profileName = "Embedded PNG iCCP Profile"
                profileSize = iccpChunk.length
                colorSpace = "RGB"
            } else {
                profileName = "sRGB Standard (sRGB chunk)"
                colorSpace = "sRGB"
            }
        }

        val iccData = IccData(
            hasProfile = hasIcc,
            profileName = profileName,
            colorSpace = colorSpace,
            profileSize = profileSize,
            profileClass = if (hasIcc) "Display Device Profile" else null
        )

        // 4. Content Credentials & AI Provenance
        val aiSignals = mutableListOf<String>()
        var isC2pa = false
        var isJumbf = false
        var generatorSoftware: String? = null

        val lowerSample = sampleText.lowercase()

        if (sampleText.contains("c2pa") || sampleText.contains("c2bi") || sampleText.contains("caBX") || sampleText.contains("urn:c2pa:")) {
            isC2pa = true
            aiSignals.add("C2PA Content Credentials Manifest detected")
        }

        if (sampleText.contains("jumb") || sampleText.contains("jumd")) {
            isJumbf = true
            aiSignals.add("JUMBF (Universal Metadata Box) detected")
        }

        if (lowerSample.contains("chatgpt") || lowerSample.contains("openai media service")) {
            aiSignals.add("AI Model: ChatGPT / OpenAI DALL-E")
            generatorSoftware = "ChatGPT (OpenAI Media Service API)"
        }
        if (lowerSample.contains("midjourney")) {
            aiSignals.add("AI Model: Midjourney")
            generatorSoftware = "Midjourney"
        }
        if (lowerSample.contains("stablediffusion") || lowerSample.contains("stable diffusion") || lowerSample.contains("sdxl")) {
            aiSignals.add("AI Model: Stable Diffusion")
            generatorSoftware = "Stable Diffusion"
        }
        if (lowerSample.contains("adobe firefly") || lowerSample.contains("firefly")) {
            aiSignals.add("AI Model: Adobe Firefly")
            generatorSoftware = "Adobe Firefly"
        }
        if (lowerSample.contains("trainedalgorithmicmedia")) {
            aiSignals.add("Claim: trainedAlgorithmicMedia (IPTC Generative Tag)")
        }
        if (lowerSample.contains("generative") || lowerSample.contains("synthetic")) {
            aiSignals.add("Generative AI metadata indicator present")
        }

        val provenanceData = ProvenanceData(
            hasProvenance = isC2pa || isJumbf || aiSignals.isNotEmpty(),
            isC2paDetected = isC2pa,
            isJumbfDetected = isJumbf,
            aiSignals = aiSignals,
            generatorSoftware = generatorSoftware
        )

        return Quadruple(xmpData, iptcData, iccData, provenanceData)
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

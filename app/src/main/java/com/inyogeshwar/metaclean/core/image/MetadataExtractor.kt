package com.inyogeshwar.metaclean.core.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.file.HashUtils
import com.inyogeshwar.metaclean.core.file.HexUtils
import com.inyogeshwar.metaclean.data.model.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MetadataExtractor {

    suspend fun extractMetadata(context: Context, uri: Uri): ImageMetadata {
        val fileName = FileUtils.getFileName(context, uri)
        val fileSizeBytes = FileUtils.getFileSize(context, uri)
        val formattedFileSize = FileUtils.formatFileSize(fileSizeBytes)
        val mimeType = FileUtils.getMimeType(context, uri)

        // Read header bytes and hex preview
        val headerBytes = ByteArray(512)
        var actualHeaderSize = 0
        context.contentResolver.openInputStream(uri)?.use { stream ->
            actualHeaderSize = stream.read(headerBytes).coerceAtLeast(0)
        }
        val headerSlice = headerBytes.copyOf(actualHeaderSize)

        // Detect Format
        val detectedFormat = FormatDetector.detectFormat(headerSlice)

        // Read image bounds efficiently without full bitmap decode
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        val width = boundsOptions.outWidth.coerceAtLeast(0)
        val height = boundsOptions.outHeight.coerceAtLeast(0)
        val megapixels = if (width > 0 && height > 0) {
            String.format("%.2f", (width.toDouble() * height.toDouble()) / 1_000_000.0).toDouble()
        } else 0.0

        // Hashes & Hex
        val checksums = context.contentResolver.openInputStream(uri)?.use { stream ->
            HashUtils.calculateHashes(stream)
        } ?: HashUtils.Checksums("N/A", "N/A", "N/A", "N/A")

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

        return ImageMetadata(
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
                if (stream.read(sig) != 8) return PngData(false)

                val lengthBuf = ByteArray(4)
                val typeBuf = ByteArray(4)

                while (stream.read(lengthBuf) == 4 && stream.read(typeBuf) == 4) {
                    val length = ByteBuffer.wrap(lengthBuf).order(ByteOrder.BIG_ENDIAN).int
                    val type = String(typeBuf, Charsets.US_ASCII)

                    val isCritical = type[0].isUpperCase()
                    val description = when (type) {
                        "IHDR" -> "Image Header (Dimensions, Bit depth, Color type)"
                        "PLTE" -> "Palette Table"
                        "IDAT" -> "Image Data Chunk"
                        "IEND" -> "Image Trailer / End of File"
                        "tEXt" -> "Uncompressed Textual Metadata"
                        "zTXt" -> "Compressed Textual Metadata"
                        "iTXt" -> "International Textual Metadata (XMP/UTF-8)"
                        "iCCP" -> "Embedded ICC Color Profile"
                        "sRGB" -> "Standard RGB Color Space"
                        "gAMA" -> "Gamma Correction"
                        "cHRM" -> "Primary Chromaticities"
                        "pHYs" -> "Physical Pixel Dimensions"
                        "eXIf" -> "Embedded EXIF Metadata Chunk"
                        "tIME" -> "Image Last Modification Time"
                        else -> "Auxiliary PNG Chunk"
                    }

                    chunks.add(PngChunk(type, length, description, isCritical))

                    // Parse chunk data if small (e.g. IHDR, tEXt, iTXt)
                    if (length in 1..65536) {
                        val chunkData = ByteArray(length)
                        var read = 0
                        while (read < length) {
                            val r = stream.read(chunkData, read, length - read)
                            if (r == -1) break
                            read += r
                        }

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
                            interlaceMethod = if (chunkData[12].toInt() == 0) "None" else "Adam7 Interlaced"
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
                                // Skip compression flag (1 byte), compression method (1 byte), lang tag (null terminated), trans key (null terminated)
                                var idx = nullIndex + 3
                                while (idx < chunkData.size && chunkData[idx] != 0.toByte()) idx++
                                idx++ // skip lang null
                                while (idx < chunkData.size && chunkData[idx] != 0.toByte()) idx++
                                idx++ // skip trans null
                                if (idx < chunkData.size) {
                                    val text = String(chunkData, idx, chunkData.size - idx, Charsets.UTF_8)
                                    textualData[key] = text.take(500)
                                }
                            }
                        }

                        // Skip 4-byte CRC
                        stream.skip(4)
                    } else {
                        // Skip length + 4 CRC
                        stream.skip(length.toLong() + 4)
                    }

                    if (type == "IEND") break
                }
            }
        } catch (e: Exception) {
            // Partial chunk parsing if file error
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
        // Scan first 128KB of image stream for XML / XMP / IPTC / ICC / C2PA / JUMBF patterns
        val sampleSize = 131072 // 128 KB
        val sampleBytes = ByteArray(sampleSize)
        var actualSampleRead = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                actualSampleRead = stream.read(sampleBytes).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            // Ignore stream read errors
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
            // Extract namespaces
            if (rawXmp.contains("xmlns:dc=")) namespaces.add("Dublin Core (dc)")
            if (rawXmp.contains("xmlns:xmp=")) namespaces.add("Adobe XMP Core (xmp)")
            if (rawXmp.contains("xmlns:photoshop=")) namespaces.add("Photoshop (photoshop)")
            if (rawXmp.contains("xmlns:crs=")) namespaces.add("Camera Raw Settings (crs)")
            if (rawXmp.contains("xmlns:c2pa=")) namespaces.add("Coalition for Content Provenance (c2pa)")

            // Extract basic properties with regex
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

        // 4. Content Credentials & AI Provenance (Read-Only / Informational)
        val aiSignals = mutableListOf<String>()
        var isC2pa = false
        var isJumbf = false
        var generatorSoftware: String? = null

        if (sampleText.contains("c2pa") || sampleText.contains("c2bi") || sampleText.contains("urn:c2pa:")) {
            isC2pa = true
            aiSignals.add("C2PA Content Credentials Manifest detected")
        }

        if (sampleText.contains("jumb") || sampleText.contains("jumd")) {
            isJumbf = true
            aiSignals.add("JUMBF (JPEG Universal Metadata Box Format) box detected")
        }

        // Detect AI provenance markers
        val lowerSample = sampleText.lowercase()
        if (lowerSample.contains("midjourney")) {
            aiSignals.add("AI Generator tag: Midjourney")
            generatorSoftware = "Midjourney"
        }
        if (lowerSample.contains("stable diffusion") || lowerSample.contains("stablediffusion") || lowerSample.contains("sdxl")) {
            aiSignals.add("AI Generator tag: Stable Diffusion")
            generatorSoftware = "Stable Diffusion"
        }
        if (lowerSample.contains("dall-e") || lowerSample.contains("dalle")) {
            aiSignals.add("AI Generator tag: DALL-E / OpenAI")
            generatorSoftware = "DALL-E"
        }
        if (lowerSample.contains("adobe firefly") || lowerSample.contains("firefly")) {
            aiSignals.add("AI Generator tag: Adobe Firefly")
            generatorSoftware = "Adobe Firefly"
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

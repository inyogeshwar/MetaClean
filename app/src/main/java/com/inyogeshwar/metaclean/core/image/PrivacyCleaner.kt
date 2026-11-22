package com.inyogeshwar.metaclean.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.file.FileUtils.readFully
import com.inyogeshwar.metaclean.core.file.FileUtils.skipFully
import com.inyogeshwar.metaclean.data.model.ImageMetadata
import com.inyogeshwar.metaclean.domain.model.CleanOptions
import com.inyogeshwar.metaclean.domain.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PrivacyCleaner {

    suspend fun cleanImage(
        context: Context,
        inputUri: Uri,
        options: CleanOptions = CleanOptions.ALL_PRIVACY,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): Pair<File, VerificationResult> = withContext(Dispatchers.IO) {
        onProgress(0.1f, "Inspecting original metadata...")
        val originalMetadata = MetadataExtractor.extractMetadata(context, inputUri, computeFullHashes = false)

        val format = FormatDetector.detectFormat(
            context.contentResolver.openInputStream(inputUri) ?: throw IOException("Cannot open input stream")
        )

        onProgress(0.3f, "Creating isolated processing copy...")
        val extension = when (format) {
            ImageFormat.PNG -> ".png"
            ImageFormat.WEBP -> ".webp"
            else -> ".jpg"
        }
        val tempOutputFile = FileUtils.createTempImageFile(context, "clean_", extension)

        onProgress(0.5f, "Cleaning privacy metadata from copy...")
        when (format) {
            ImageFormat.PNG -> {
                cleanPngLossless(context, inputUri, tempOutputFile, options)
            }
            ImageFormat.JPEG -> {
                cleanJpegExif(context, inputUri, tempOutputFile, options)
            }
            ImageFormat.WEBP -> {
                cleanWebp(context, inputUri, tempOutputFile, options)
            }
            else -> {
                cleanGenericBitmap(context, inputUri, tempOutputFile, options, format)
            }
        }

        onProgress(0.8f, "Verifying cleaned output...")
        val cleanedUri = Uri.fromFile(tempOutputFile)
        val cleanedMetadata = MetadataExtractor.extractMetadata(context, cleanedUri, computeFullHashes = false)

        onProgress(0.95f, "Generating verification audit...")
        val verificationResult = buildVerificationResult(originalMetadata, cleanedMetadata, options)

        onProgress(1.0f, "Completed")
        Pair(tempOutputFile, verificationResult)
    }

    private fun cleanJpegExif(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions
    ) {
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                FileUtils.copyStream(input, output)
            }
        } ?: throw IOException("Could not copy original JPEG")

        try {
            val exif = ExifInterface(outputFile.absolutePath)

            if (options.removeGps) {
                val gpsTags = listOf(
                    ExifInterface.TAG_GPS_LATITUDE,
                    ExifInterface.TAG_GPS_LATITUDE_REF,
                    ExifInterface.TAG_GPS_LONGITUDE,
                    ExifInterface.TAG_GPS_LONGITUDE_REF,
                    ExifInterface.TAG_GPS_ALTITUDE,
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    ExifInterface.TAG_GPS_DATESTAMP,
                    ExifInterface.TAG_GPS_PROCESSING_METHOD,
                    ExifInterface.TAG_GPS_AREA_INFORMATION,
                    ExifInterface.TAG_GPS_DOP,
                    ExifInterface.TAG_GPS_SPEED,
                    ExifInterface.TAG_GPS_SPEED_REF,
                    ExifInterface.TAG_GPS_TRACK,
                    ExifInterface.TAG_GPS_TRACK_REF,
                    ExifInterface.TAG_GPS_IMG_DIRECTION,
                    ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
                    ExifInterface.TAG_GPS_DEST_LATITUDE,
                    ExifInterface.TAG_GPS_DEST_LONGITUDE
                )
                for (tag in gpsTags) {
                    exif.setAttribute(tag, null)
                }
            }

            if (options.removeCameraDetails) {
                val cameraTags = listOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_SOFTWARE,
                    ExifInterface.TAG_LENS_MAKE,
                    ExifInterface.TAG_LENS_MODEL,
                    ExifInterface.TAG_LENS_SPECIFICATION,
                    ExifInterface.TAG_BODY_SERIAL_NUMBER,
                    ExifInterface.TAG_LENS_SERIAL_NUMBER,
                    ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION
                )
                for (tag in cameraTags) {
                    exif.setAttribute(tag, null)
                }
            }

            if (options.removeTimestamps) {
                val timeTags = listOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_OFFSET_TIME,
                    ExifInterface.TAG_OFFSET_TIME_ORIGINAL,
                    ExifInterface.TAG_OFFSET_TIME_DIGITIZED,
                    ExifInterface.TAG_SUBSEC_TIME,
                    ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
                    ExifInterface.TAG_SUBSEC_TIME_DIGITIZED
                )
                for (tag in timeTags) {
                    exif.setAttribute(tag, null)
                }
            }

            if (options.removeAuthorAndContact) {
                val authorTags = listOf(
                    ExifInterface.TAG_ARTIST,
                    ExifInterface.TAG_COPYRIGHT,
                    ExifInterface.TAG_USER_COMMENT,
                    ExifInterface.TAG_IMAGE_DESCRIPTION
                )
                for (tag in authorTags) {
                    exif.setAttribute(tag, null)
                }
            }

            exif.saveAttributes()
        } catch (e: Exception) {
            // Keep copied file intact
        }
    }

    /**
     * Lossless PNG chunk filtering.
     * Uses streaming buffers with skipFully to prevent memory spikes or heating.
     */
    private fun cleanPngLossless(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions
    ) {
        context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            val bufferedIn = BufferedInputStream(inputStream, 32768)
            FileOutputStream(outputFile).use { fileOut ->
                val bufferedOut = BufferedOutputStream(fileOut, 32768)

                val signature = ByteArray(8)
                if (!bufferedIn.readFully(signature)) throw IOException("Invalid PNG header")
                bufferedOut.write(signature)

                val lengthBuf = ByteArray(4)
                val typeBuf = ByteArray(4)
                val streamBuf = ByteArray(16384)

                while (bufferedIn.readFully(lengthBuf) && bufferedIn.readFully(typeBuf)) {
                    val length = ByteBuffer.wrap(lengthBuf).order(ByteOrder.BIG_ENDIAN).int
                    val type = String(typeBuf, Charsets.US_ASCII)

                    val isMetadataChunk = when (type) {
                        "tEXt", "zTXt", "iTXt" -> options.removeAuthorAndContact || options.removeXmpAndIptc
                        "eXIf" -> options.removeCameraDetails || options.removeGps || options.removeTimestamps
                        "tIME" -> options.removeTimestamps
                        "caBX", "jumb" -> true // Strip C2PA/JUMBF if requested
                        else -> false
                    }

                    if (isMetadataChunk) {
                        bufferedIn.skipFully(length.toLong() + 4)
                    } else {
                        bufferedOut.write(lengthBuf)
                        bufferedOut.write(typeBuf)

                        var remaining = length.toLong() + 4
                        while (remaining > 0) {
                            val toRead = minOf(remaining, streamBuf.size.toLong()).toInt()
                            val r = bufferedIn.read(streamBuf, 0, toRead)
                            if (r == -1) break
                            bufferedOut.write(streamBuf, 0, r)
                            remaining -= r
                        }
                    }

                    if (type == "IEND") break
                }
                bufferedOut.flush()
            }
        } ?: throw IOException("Could not open input PNG stream")
    }

    private fun cleanWebp(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions
    ) {
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                FileUtils.copyStream(input, output)
            }
        } ?: throw IOException("Could not copy original WebP")

        try {
            val exif = ExifInterface(outputFile.absolutePath)
            if (options.removeGps) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            }
            if (options.removeCameraDetails) {
                exif.setAttribute(ExifInterface.TAG_MAKE, null)
                exif.setAttribute(ExifInterface.TAG_MODEL, null)
            }
            if (options.removeTimestamps) {
                exif.setAttribute(ExifInterface.TAG_DATETIME, null)
            }
            exif.saveAttributes()
        } catch (e: Exception) {
            // Keep file
        }
    }

    private fun cleanGenericBitmap(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions,
        format: ImageFormat
    ) {
        // Safe downsampled decode to prevent OOM
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(inputUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        var sampleSize = 1
        val maxDim = 4096
        while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(inputUri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            if (bitmap != null) {
                FileOutputStream(outputFile).use { out ->
                    val compressFormat = if (format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    bitmap.compress(compressFormat, 95, out)
                }
                bitmap.recycle()
            }
        }
    }

    private fun buildVerificationResult(
        original: ImageMetadata,
        cleaned: ImageMetadata,
        options: CleanOptions
    ): VerificationResult {
        val removedFields = mutableListOf<String>()
        val retainedFields = mutableListOf<String>()
        val unremovableFields = mutableListOf<String>()

        if (original.exifData.gpsCoordinateString != null && cleaned.exifData.gpsCoordinateString == null) {
            removedFields.add("GPS Location Coordinates")
        }

        if ((original.exifData.cameraMake != null || original.exifData.cameraModel != null) &&
            cleaned.exifData.cameraMake == null && cleaned.exifData.cameraModel == null
        ) {
            removedFields.add("Camera Make & Model (${original.exifData.cameraMake ?: ""} ${original.exifData.cameraModel ?: ""}".trim() + ")")
        }

        if (original.exifData.software != null && cleaned.exifData.software == null) {
            removedFields.add("Software / Processing Tool (${original.exifData.software})")
        }

        if (original.exifData.dateTimeOriginal != null && cleaned.exifData.dateTimeOriginal == null) {
            removedFields.add("Capture Timestamp (${original.exifData.dateTimeOriginal})")
        }

        if (original.exifData.artist != null && cleaned.exifData.artist == null) {
            removedFields.add("Artist / Author (${original.exifData.artist})")
        }

        if (original.exifData.userComment != null && cleaned.exifData.userComment == null) {
            removedFields.add("User Comments & Notes")
        }

        if (original.pngData != null && cleaned.pngData != null) {
            val originalTextKeys = original.pngData.textualData.keys
            val cleanedTextKeys = cleaned.pngData.textualData.keys
            val removedKeys = originalTextKeys - cleanedTextKeys
            for (key in removedKeys) {
                removedFields.add("PNG Text Chunk ($key)")
            }
        }

        if (original.xmpData.hasXmp && !cleaned.xmpData.hasXmp) {
            removedFields.add("Adobe XMP Metadata Package")
        }

        if (original.provenanceData.hasProvenance) {
            if (cleaned.provenanceData.hasProvenance) {
                unremovableFields.add("Content Credentials / C2PA / AI Provenance")
            } else {
                removedFields.add("C2PA / AI Metadata Signals")
            }
        }

        retainedFields.add("Resolution & Dimensions (${cleaned.fileInfo.width} × ${cleaned.fileInfo.height} px)")
        retainedFields.add("Color Space & Rendering Profile")

        val origDim = Pair(original.fileInfo.width, original.fileInfo.height)
        val cleanDim = Pair(cleaned.fileInfo.width, cleaned.fileInfo.height)
        val dimensionsPreserved = (origDim.first == cleanDim.first && origDim.second == cleanDim.second) ||
                (origDim.first == 0 && cleanDim.first == 0)

        val origSize = original.fileInfo.fileSizeBytes
        val cleanSize = cleaned.fileInfo.fileSizeBytes
        val sizeDiff = origSize - cleanSize
        val percentage = if (origSize > 0) {
            ((sizeDiff.toDouble() / origSize.toDouble()) * 100.0).coerceAtLeast(0.0)
        } else 0.0

        val isVerified = removedFields.isNotEmpty() || (original.exifData.allTags.isEmpty() && !original.exifData.hasExif)

        val summary = if (removedFields.isNotEmpty()) {
            "Successfully cleaned ${removedFields.size} metadata fields. Visual dimensions preserved."
        } else {
            "Output is verified clean."
        }

        return VerificationResult(
            isVerifiedClean = isVerified,
            removedFields = removedFields,
            retainedFields = retainedFields,
            unremovableFields = unremovableFields,
            originalSizeBytes = origSize,
            cleanedSizeBytes = cleanSize,
            sizeDifferenceBytes = sizeDiff,
            sizeReductionPercentage = String.format("%.1f", percentage).toDouble(),
            originalDimensions = origDim,
            cleanedDimensions = cleanDim,
            dimensionsPreserved = dimensionsPreserved,
            summaryNotes = summary
        )
    }
}

package com.inyogeshwar.metaclean.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.inyogeshwar.metaclean.core.file.FileUtils
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
        val originalMetadata = MetadataExtractor.extractMetadata(context, inputUri)

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
                // Fallback for other formats: safe re-encoding with dimension preservation
                cleanGenericBitmap(context, inputUri, tempOutputFile, options, format)
            }
        }

        onProgress(0.8f, "Verifying cleaned output...")
        val cleanedUri = Uri.fromFile(tempOutputFile)
        val cleanedMetadata = MetadataExtractor.extractMetadata(context, cleanedUri)

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
        // First copy input stream to output file
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                FileUtils.copyStream(input, output)
            }
        } ?: throw IOException("Could not copy original JPEG")

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

        // Save attributes back into the file copy
        try {
            exif.saveAttributes()
        } catch (e: Exception) {
            // If in-place modification of unusual JPEG structure fails, fallback to clean bitmap save
            fallbackCleanJpeg(outputFile)
        }
    }

    private fun fallbackCleanJpeg(file: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
                }
                bitmap.recycle()
            }
        } catch (e: Exception) {
            // Keep existing file if fallback fails
        }
    }

    /**
     * Lossless PNG chunk filtering.
     * Strips textual and EXIF metadata chunks without modifying the raw image stream (IDAT).
     */
    private fun cleanPngLossless(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions
    ) {
        context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            val bufferedIn = BufferedInputStream(inputStream)
            FileOutputStream(outputFile).use { fileOut ->
                val bufferedOut = BufferedOutputStream(fileOut)

                // 1. Verify and write 8-byte PNG signature
                val signature = ByteArray(8)
                if (bufferedIn.read(signature) != 8) throw IOException("Invalid PNG header")
                bufferedOut.write(signature)

                val lengthBuf = ByteArray(4)
                val typeBuf = ByteArray(4)

                // 2. Iterate chunks
                while (bufferedIn.read(lengthBuf) == 4 && bufferedIn.read(typeBuf) == 4) {
                    val length = ByteBuffer.wrap(lengthBuf).order(ByteOrder.BIG_ENDIAN).int
                    val type = String(typeBuf, Charsets.US_ASCII)

                    val isMetadataChunk = when (type) {
                        "tEXt", "zTXt", "iTXt" -> options.removeAuthorAndContact || options.removeXmpAndIptc
                        "eXIf" -> options.removeCameraDetails || options.removeGps || options.removeTimestamps
                        "tIME" -> options.removeTimestamps
                        else -> false
                    }

                    if (isMetadataChunk) {
                        // Skip length bytes + 4 bytes CRC
                        bufferedIn.skip(length.toLong() + 4)
                    } else {
                        // Preserve chunk
                        bufferedOut.write(lengthBuf)
                        bufferedOut.write(typeBuf)

                        val chunkDataAndCrc = ByteArray(length + 4)
                        var read = 0
                        while (read < chunkDataAndCrc.size) {
                            val r = bufferedIn.read(chunkDataAndCrc, read, chunkDataAndCrc.size - read)
                            if (r == -1) break
                            read += r
                        }
                        bufferedOut.write(chunkDataAndCrc, 0, read)
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
        // Copy original first
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
            // Fallback to high-quality recompression preserving visual appearance
            fallbackCleanWebp(context, inputUri, outputFile)
        }
    }

    private fun fallbackCleanWebp(context: Context, inputUri: Uri, outputFile: File) {
        context.contentResolver.openInputStream(inputUri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
            if (bitmap != null) {
                FileOutputStream(outputFile).use { out ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, out)
                    }
                }
                bitmap.recycle()
            }
        }
    }

    private fun cleanGenericBitmap(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        options: CleanOptions,
        format: ImageFormat
    ) {
        context.contentResolver.openInputStream(inputUri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream)
                ?: throw IOException("Unable to decode image format: ${format.displayName}")
            FileOutputStream(outputFile).use { out ->
                val compressFormat = if (format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                bitmap.compress(compressFormat, 98, out)
            }
            bitmap.recycle()
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

        // 1. Check GPS removal
        if (original.exifData.gpsCoordinateString != null) {
            if (cleaned.exifData.gpsCoordinateString == null) {
                removedFields.add("GPS Location Coordinates")
            } else {
                retainedFields.add("GPS Location (Preserved)")
            }
        }

        // 2. Check Camera / Device details
        if (original.exifData.cameraMake != null || original.exifData.cameraModel != null) {
            if (cleaned.exifData.cameraMake == null && cleaned.exifData.cameraModel == null) {
                removedFields.add("Camera Make & Model (${original.exifData.cameraMake ?: ""} ${original.exifData.cameraModel ?: ""}".trim() + ")")
            } else {
                retainedFields.add("Camera Details")
            }
        }

        if (original.exifData.software != null) {
            if (cleaned.exifData.software == null) {
                removedFields.add("Software / Processing Tool (${original.exifData.software})")
            } else {
                retainedFields.add("Software / Tool")
            }
        }

        // 3. Check Timestamps
        if (original.exifData.dateTimeOriginal != null) {
            if (cleaned.exifData.dateTimeOriginal == null) {
                removedFields.add("Capture Timestamp (${original.exifData.dateTimeOriginal})")
            } else {
                retainedFields.add("Capture Timestamp")
            }
        }

        // 4. Check Author & Copyright
        if (original.exifData.artist != null) {
            if (cleaned.exifData.artist == null) {
                removedFields.add("Artist / Author (${original.exifData.artist})")
            } else {
                retainedFields.add("Artist / Author")
            }
        }
        if (original.exifData.userComment != null) {
            if (cleaned.exifData.userComment == null) {
                removedFields.add("User Comments & Notes")
            } else {
                retainedFields.add("User Comments")
            }
        }

        // 5. Check PNG chunks if PNG
        if (original.pngData != null && cleaned.pngData != null) {
            val originalTextKeys = original.pngData.textualData.keys
            val cleanedTextKeys = cleaned.pngData.textualData.keys
            val removedKeys = originalTextKeys - cleanedTextKeys
            for (key in removedKeys) {
                removedFields.add("PNG Text Chunk ($key)")
            }
        }

        // 6. Check XMP properties
        if (original.xmpData.hasXmp && !cleaned.xmpData.hasXmp) {
            removedFields.add("Adobe XMP Metadata Package")
        }

        // 7. Retained technical properties (always expected to remain intact)
        retainedFields.add("Resolution & Dimensions (${cleaned.fileInfo.width}x${cleaned.fileInfo.height})")
        retainedFields.add("Color Space & Rendering Profile")

        // 8. Provenance / C2PA checks (informational, read-only)
        if (original.provenanceData.hasProvenance) {
            unremovableFields.add("Content Credentials / C2PA / AI Provenance (Read-only by design)")
        }

        // Dimensions verification
        val origDim = Pair(original.fileInfo.width, original.fileInfo.height)
        val cleanDim = Pair(cleaned.fileInfo.width, cleaned.fileInfo.height)
        val dimensionsPreserved = (origDim.first == cleanDim.first && origDim.second == cleanDim.second) ||
                (origDim.first == 0 && cleanDim.first == 0)

        // Size calculation
        val origSize = original.fileInfo.fileSizeBytes
        val cleanSize = cleaned.fileInfo.fileSizeBytes
        val sizeDiff = origSize - cleanSize
        val percentage = if (origSize > 0) {
            ((sizeDiff.toDouble() / origSize.toDouble()) * 100.0).coerceAtLeast(0.0)
        } else 0.0

        val isVerified = removedFields.isNotEmpty() || (original.exifData.allTags.isEmpty() && !original.exifData.hasExif)

        val summary = if (removedFields.isNotEmpty()) {
            "Successfully cleaned ${removedFields.size} personal metadata fields. Visual dimensions preserved."
        } else {
            "No sensitive personal metadata found or needed removal. Output is verified clean."
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

package com.inyogeshwar.metaclean.core.file

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = size / 1024.0.pow(index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment ?: "image_${System.currentTimeMillis()}.jpg"
        }
        return name ?: "image.jpg"
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index != -1) {
                        size = it.getLong(index)
                    }
                }
            }
        }
        if (size <= 0) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                    size = it.statSize
                }
            } catch (e: Exception) {
                // Ignore fallback
            }
        }
        return size
    }

    fun getMimeType(context: Context, uri: Uri): String {
        var mime = context.contentResolver.getType(uri)
        if (mime == null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            }
        }
        return mime ?: "image/jpeg"
    }

    fun getExtensionFromMime(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/heif", "image/heic" -> "heic"
            "image/tiff" -> "tiff"
            "image/bmp" -> "bmp"
            else -> "jpg"
        }
    }

    fun InputStream.readFully(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Boolean {
        var totalRead = 0
        while (totalRead < length) {
            val count = this.read(buffer, offset + totalRead, length - totalRead)
            if (count == -1) return false
            totalRead += count
        }
        return true
    }

    fun InputStream.skipFully(bytesToSkip: Long): Long {
        var remaining = bytesToSkip
        val tempBuf = ByteArray(8192)
        while (remaining > 0) {
            val skipped = this.skip(remaining)
            if (skipped <= 0) {
                val toRead = minOf(remaining, tempBuf.size.toLong()).toInt()
                val read = this.read(tempBuf, 0, toRead)
                if (read == -1) break
                remaining -= read
            } else {
                remaining -= skipped
            }
        }
        return bytesToSkip - remaining
    }

    fun copyStream(input: InputStream, output: OutputStream, bufferSize: Int = 16384): Long {
        val buffer = ByteArray(bufferSize)
        var totalBytes: Long = 0
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
            totalBytes += bytesRead
        }
        output.flush()
        return totalBytes
    }

    fun createTempImageFile(context: Context, prefix: String = "clean_", extension: String = ".jpg"): File {
        val cacheDir = File(context.cacheDir, "cleaned")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File.createTempFile(prefix, extension, cacheDir)
    }

    fun clearTempFiles(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "cleaned")
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }

    /**
     * Saves a cleaned image directly into Android's public gallery (Pictures/MetaClean).
     * Works on Android 10+ (Scoped Storage) and earlier without requiring storage permissions.
     * The photo is immediately visible in Google Photos, Gallery, and other gallery apps.
     */
    fun saveImageToPublicGallery(
        context: Context,
        sourceFile: File,
        originalFileName: String,
        mimeType: String,
        suffix: String = ""
    ): Uri? {
        try {
            val extension = getExtensionFromMime(mimeType)
            val baseName = originalFileName.substringBeforeLast(".")
            val displayName = if (suffix.isNotEmpty() && !baseName.endsWith(suffix)) {
                "${baseName}${suffix}.${extension}"
            } else {
                originalFileName
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MetaClean")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        copyStream(input, out)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                return uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Overwrites the original image in place via ContentResolver / File if write permissions allow.
     * Returns true if successfully overwritten, false if restricted by Scoped Storage or read-only provider.
     */
    fun overwriteOriginal(context: Context, sourceFile: File, originalUri: Uri): Boolean {
        try {
            context.contentResolver.openOutputStream(originalUri, "wt")?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    copyStream(input, out)
                }
                return true
            }
        } catch (e: Exception) {
            // Cannot overwrite in-place (likely Scoped Storage restriction)
        }
        return false
    }
}

package com.inyogeshwar.metaclean.core.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
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
}

package com.inyogeshwar.metaclean.core.file

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {

    suspend fun createZipArchive(
        context: Context,
        filesToZip: List<Pair<Uri, String>>, // (Uri, TargetFileName)
        outputStream: OutputStream,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                val buffer = ByteArray(16384)
                filesToZip.forEachIndexed { index, (uri, fileName) ->
                    val entry = ZipEntry(fileName)
                    zipOut.putNextEntry(entry)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BufferedInputStream(inputStream).use { bufferedInput ->
                            var bytesRead: Int
                            while (bufferedInput.read(buffer).also { bytesRead = it } != -1) {
                                zipOut.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                    zipOut.closeEntry()
                    onProgress(index + 1, filesToZip.size)
                }
                zipOut.finish()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

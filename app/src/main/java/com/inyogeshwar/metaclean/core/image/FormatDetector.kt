package com.inyogeshwar.metaclean.core.image

import java.io.InputStream

enum class ImageFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val isSupportedForCleaning: Boolean
) {
    JPEG("jpg", "image/jpeg", "JPEG Image", true),
    PNG("png", "image/png", "PNG Image", true),
    WEBP("webp", "image/webp", "WebP Image", true),
    GIF("gif", "image/gif", "GIF Animation/Image", false),
    HEIF("heic", "image/heic", "High Efficiency Image (HEIF/HEIC)", false),
    TIFF("tiff", "image/tiff", "TIFF Image", false),
    BMP("bmp", "image/bmp", "Bitmap (BMP)", false),
    UNKNOWN("bin", "application/octet-stream", "Unknown Binary Format", false)
}

object FormatDetector {

    fun detectFormat(headerBytes: ByteArray): ImageFormat {
        if (headerBytes.size < 2) return ImageFormat.UNKNOWN

        // JPEG: FF D8 FF
        if (headerBytes.size >= 3 && headerBytes[0] == 0xFF.toByte() && headerBytes[1] == 0xD8.toByte() && headerBytes[2] == 0xFF.toByte()) {
            return ImageFormat.JPEG
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (headerBytes.size >= 8 &&
            headerBytes[0] == 0x89.toByte() && headerBytes[1] == 0x50.toByte() &&
            headerBytes[2] == 0x4E.toByte() && headerBytes[3] == 0x47.toByte() &&
            headerBytes[4] == 0x0D.toByte() && headerBytes[5] == 0x0A.toByte() &&
            headerBytes[6] == 0x1A.toByte() && headerBytes[7] == 0x0A.toByte()
        ) {
            return ImageFormat.PNG
        }

        // WebP: RIFF .... WEBP
        if (headerBytes.size >= 12 &&
            headerBytes[0] == 'R'.code.toByte() && headerBytes[1] == 'I'.code.toByte() &&
            headerBytes[2] == 'F'.code.toByte() && headerBytes[3] == 'F'.code.toByte() &&
            headerBytes[8] == 'W'.code.toByte() && headerBytes[9] == 'E'.code.toByte() &&
            headerBytes[10] == 'B'.code.toByte() && headerBytes[11] == 'P'.code.toByte()
        ) {
            return ImageFormat.WEBP
        }

        // GIF: GIF87a or GIF89a
        if (headerBytes.size >= 6 &&
            headerBytes[0] == 'G'.code.toByte() && headerBytes[1] == 'I'.code.toByte() &&
            headerBytes[2] == 'F'.code.toByte() && headerBytes[3] == '8'.code.toByte()
        ) {
            return ImageFormat.GIF
        }

        // TIFF: II*. or MM.*
        if (headerBytes.size >= 4) {
            if ((headerBytes[0] == 0x49.toByte() && headerBytes[1] == 0x49.toByte() && headerBytes[2] == 0x2A.toByte() && headerBytes[3] == 0x00.toByte()) ||
                (headerBytes[0] == 0x4D.toByte() && headerBytes[1] == 0x4D.toByte() && headerBytes[2] == 0x00.toByte() && headerBytes[3] == 0x2A.toByte())
            ) {
                return ImageFormat.TIFF
            }
        }

        // BMP: BM
        if (headerBytes.size >= 2 && headerBytes[0] == 0x42.toByte() && headerBytes[1] == 0x4D.toByte()) {
            return ImageFormat.BMP
        }

        // HEIF / HEIC: offset 4 'ftyp'
        if (headerBytes.size >= 12 &&
            headerBytes[4] == 'f'.code.toByte() && headerBytes[5] == 't'.code.toByte() &&
            headerBytes[6] == 'y'.code.toByte() && headerBytes[7] == 'p'.code.toByte()
        ) {
            return ImageFormat.HEIF
        }

        return ImageFormat.UNKNOWN
    }

    fun detectFormat(inputStream: InputStream): ImageFormat {
        val header = ByteArray(32)
        val read = inputStream.read(header)
        return if (read >= 2) {
            detectFormat(header.copyOf(read))
        } else {
            ImageFormat.UNKNOWN
        }
    }
}

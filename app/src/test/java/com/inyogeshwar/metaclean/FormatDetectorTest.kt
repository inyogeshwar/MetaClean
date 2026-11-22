package com.inyogeshwar.metaclean

import com.inyogeshwar.metaclean.core.image.FormatDetector
import com.inyogeshwar.metaclean.core.image.ImageFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class FormatDetectorTest {

    @Test
    fun testDetectJpeg() {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01)
        val format = FormatDetector.detectFormat(jpegHeader)
        assertEquals(ImageFormat.JPEG, format)
    }

    @Test
    fun testDetectPng() {
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00, 0x00, 0x00, 0x0D
        )
        val format = FormatDetector.detectFormat(pngHeader)
        assertEquals(ImageFormat.PNG, format)
    }

    @Test
    fun testDetectWebp() {
        val webpHeader = byteArrayOf(
            'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
            0x00, 0x00, 0x00, 0x00,
            'W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte()
        )
        val format = FormatDetector.detectFormat(webpHeader)
        assertEquals(ImageFormat.WEBP, format)
    }

    @Test
    fun testDetectGif() {
        val gifHeader = byteArrayOf('G'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), '8'.code.toByte(), '9'.code.toByte(), 'a'.code.toByte())
        val format = FormatDetector.detectFormat(gifHeader)
        assertEquals(ImageFormat.GIF, format)
    }

    @Test
    fun testDetectTiffLittleEndian() {
        val tiffHeader = byteArrayOf(0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00)
        val format = FormatDetector.detectFormat(tiffHeader)
        assertEquals(ImageFormat.TIFF, format)
    }

    @Test
    fun testDetectUnknown() {
        val randomBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val format = FormatDetector.detectFormat(randomBytes)
        assertEquals(ImageFormat.UNKNOWN, format)
    }

    @Test
    fun testDetectFromInputStream() {
        val pngHeader = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(),
            0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte()
        )
        val stream = ByteArrayInputStream(pngHeader)
        val format = FormatDetector.detectFormat(stream)
        assertEquals(ImageFormat.PNG, format)
    }
}

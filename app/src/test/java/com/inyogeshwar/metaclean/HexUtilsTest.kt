package com.inyogeshwar.metaclean

import com.inyogeshwar.metaclean.core.file.HexUtils
import org.junit.Assert.assertTrue
import org.junit.Test

class HexUtilsTest {

    @Test
    fun testFormatHeaderHex() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
        val hex = HexUtils.formatHeaderHex(bytes, 4)
        assertTrue(hex.contains("89 50 4E 47"))
    }

    @Test
    fun testFormatHexDump() {
        val testBytes = "Hello World 1234".toByteArray(Charsets.US_ASCII)
        val dump = HexUtils.formatHexDump(testBytes, 16)
        assertTrue(dump.contains("0000:"))
        assertTrue(dump.contains("Hello World 1234"))
    }
}

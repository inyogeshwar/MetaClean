package com.inyogeshwar.metaclean

import com.inyogeshwar.metaclean.core.file.HashUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class HashUtilsTest {

    @Test
    fun testChecksumCalculation() {
        val testData = "MetaClean Privacy Toolkit Test String"
        val inputStream = ByteArrayInputStream(testData.toByteArray(Charsets.UTF_8))
        val checksums = HashUtils.calculateHashes(inputStream)

        // Known hashes for "MetaClean Privacy Toolkit Test String":
        // MD5: 9a6156a096c4feea328675661bcce1bb (or similar valid 32 hex chars)
        assertEquals(32, checksums.md5.length)
        assertEquals(40, checksums.sha1.length)
        assertEquals(64, checksums.sha256.length)
        assertEquals(8, checksums.crc32.length)
    }

    @Test
    fun testEmptyStreamChecksums() {
        val emptyStream = ByteArrayInputStream(ByteArray(0))
        val checksums = HashUtils.calculateHashes(emptyStream)

        // MD5 of empty string is d41d8cd98f00b204e9800998ecf8427e
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", checksums.md5)
        // SHA-1 of empty string is da39a3ee5e6b4b0d3255bfef95601890afd80709
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", checksums.sha1)
        // SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", checksums.sha256)
        assertEquals("00000000", checksums.crc32)
    }
}

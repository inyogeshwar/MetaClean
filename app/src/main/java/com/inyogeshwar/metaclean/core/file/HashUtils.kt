package com.inyogeshwar.metaclean.core.file

import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

object HashUtils {

    data class Checksums(
        val md5: String,
        val sha1: String,
        val sha256: String,
        val crc32: String
    )

    fun calculateHashes(inputStream: InputStream): Checksums {
        val md5Digest = MessageDigest.getInstance("MD5")
        val sha1Digest = MessageDigest.getInstance("SHA-1")
        val sha256Digest = MessageDigest.getInstance("SHA-256")
        val crc32 = CRC32()

        val buffer = ByteArray(16384)
        var bytesRead: Int

        inputStream.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                md5Digest.update(buffer, 0, bytesRead)
                sha1Digest.update(buffer, 0, bytesRead)
                sha256Digest.update(buffer, 0, bytesRead)
                crc32.update(buffer, 0, bytesRead)
            }
        }

        return Checksums(
            md5 = md5Digest.digest().joinToString("") { "%02x".format(it) },
            sha1 = sha1Digest.digest().joinToString("") { "%02x".format(it) },
            sha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) },
            crc32 = "%08X".format(crc32.value)
        )
    }
}

package com.inyogeshwar.metaclean.core.file

object HexUtils {

    fun formatHexDump(bytes: ByteArray, maxBytes: Int = 256): String {
        val count = bytes.size.coerceAtMost(maxBytes)
        if (count == 0) return "No data available"

        val sb = StringBuilder()
        val bytesPerLine = 16

        for (i in 0 until count step bytesPerLine) {
            val lineOffset = "%04X: ".format(i)
            sb.append(lineOffset)

            val lineEnd = (i + bytesPerLine).coerceAtMost(count)
            // Hex portion
            for (j in i until i + bytesPerLine) {
                if (j < lineEnd) {
                    sb.append("%02X ".format(bytes[j]))
                } else {
                    sb.append("   ")
                }
            }
            sb.append(" | ")

            // ASCII portion
            for (j in i until lineEnd) {
                val b = bytes[j].toInt()
                if (b in 32..126) {
                    sb.append(b.toChar())
                } else {
                    sb.append('.')
                }
            }
            sb.append("\n")
        }

        if (bytes.size > maxBytes) {
            sb.append("... [${bytes.size - maxBytes} more bytes truncated]")
        }

        return sb.toString()
    }

    fun formatHeaderHex(bytes: ByteArray, length: Int = 16): String {
        val count = bytes.size.coerceAtMost(length)
        return bytes.take(count).joinToString(" ") { "%02X".format(it) }
    }
}

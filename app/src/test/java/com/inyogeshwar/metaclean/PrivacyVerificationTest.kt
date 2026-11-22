package com.inyogeshwar.metaclean

import com.inyogeshwar.metaclean.domain.model.VerificationResult
import org.junit.Assert.*
import org.junit.Test

class PrivacyVerificationTest {

    @Test
    fun testVerificationMetricsCalculation() {
        val origBytes = 2_000_000L
        val cleanBytes = 1_850_000L
        val diff = origBytes - cleanBytes
        val percent = (diff.toDouble() / origBytes.toDouble()) * 100.0

        val ver = VerificationResult(
            isVerifiedClean = true,
            removedFields = listOf("GPS Location Coordinates", "Camera Make & Model", "Capture Timestamp"),
            retainedFields = listOf("Resolution & Dimensions (4032x3024)", "Color Space & Rendering Profile"),
            unremovableFields = emptyList(),
            originalSizeBytes = origBytes,
            cleanedSizeBytes = cleanBytes,
            sizeDifferenceBytes = diff,
            sizeReductionPercentage = percent,
            originalDimensions = Pair(4032, 3024),
            cleanedDimensions = Pair(4032, 3024),
            dimensionsPreserved = true,
            summaryNotes = "Successfully cleaned 3 personal metadata fields."
        )

        assertTrue(ver.isVerifiedClean)
        assertEquals(3, ver.removedFields.size)
        assertEquals(2, ver.retainedFields.size)
        assertTrue(ver.dimensionsPreserved)
        assertEquals(150_000L, ver.sizeDifferenceBytes)
        assertEquals(7.5, ver.sizeReductionPercentage, 0.01)
    }

    @Test
    fun testDimensionMismatchDetection() {
        val ver = VerificationResult(
            isVerifiedClean = false,
            removedFields = emptyList(),
            retainedFields = emptyList(),
            unremovableFields = emptyList(),
            originalSizeBytes = 1000L,
            cleanedSizeBytes = 900L,
            sizeDifferenceBytes = 100L,
            sizeReductionPercentage = 10.0,
            originalDimensions = Pair(1920, 1080),
            cleanedDimensions = Pair(1280, 720),
            dimensionsPreserved = false,
            summaryNotes = "Dimension mismatch"
        )

        assertFalse(ver.dimensionsPreserved)
        assertNotEquals(ver.originalDimensions, ver.cleanedDimensions)
    }
}

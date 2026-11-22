package com.inyogeshwar.metaclean

import com.inyogeshwar.metaclean.domain.model.CleanOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanOptionsTest {

    @Test
    fun testAllPrivacyDefaults() {
        val options = CleanOptions.ALL_PRIVACY
        assertTrue(options.removeGps)
        assertTrue(options.removeCameraDetails)
        assertTrue(options.removeTimestamps)
        assertTrue(options.removeAuthorAndContact)
        assertTrue(options.removeThumbnails)
        assertTrue(options.removeXmpAndIptc)
        assertTrue(options.preserveDimensions)
    }

    @Test
    fun testLocationOnlyDefaults() {
        val options = CleanOptions.LOCATION_ONLY
        assertTrue(options.removeGps)
        assertFalse(options.removeCameraDetails)
        assertFalse(options.removeTimestamps)
        assertFalse(options.removeAuthorAndContact)
        assertFalse(options.removeThumbnails)
        assertFalse(options.removeXmpAndIptc)
        assertTrue(options.preserveDimensions)
    }

    @Test
    fun testCustomCleanOptions() {
        val custom = CleanOptions(
            removeGps = true,
            removeCameraDetails = false,
            removeTimestamps = true,
            removeAuthorAndContact = false,
            removeThumbnails = true,
            removeXmpAndIptc = false,
            preserveDimensions = true
        )
        assertTrue(custom.removeGps)
        assertFalse(custom.removeCameraDetails)
        assertTrue(custom.removeTimestamps)
        assertFalse(custom.removeAuthorAndContact)
        assertTrue(custom.removeThumbnails)
        assertFalse(custom.removeXmpAndIptc)
    }
}

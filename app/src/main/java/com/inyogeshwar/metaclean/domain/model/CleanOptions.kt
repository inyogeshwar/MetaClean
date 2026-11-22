package com.inyogeshwar.metaclean.domain.model

data class CleanOptions(
    val removeGps: Boolean = true,
    val removeCameraDetails: Boolean = true,
    val removeTimestamps: Boolean = true,
    val removeAuthorAndContact: Boolean = true,
    val removeThumbnails: Boolean = true,
    val removeXmpAndIptc: Boolean = true,
    val preserveDimensions: Boolean = true
) {
    companion object {
        val ALL_PRIVACY = CleanOptions(
            removeGps = true,
            removeCameraDetails = true,
            removeTimestamps = true,
            removeAuthorAndContact = true,
            removeThumbnails = true,
            removeXmpAndIptc = true,
            preserveDimensions = true
        )

        val LOCATION_ONLY = CleanOptions(
            removeGps = true,
            removeCameraDetails = false,
            removeTimestamps = false,
            removeAuthorAndContact = false,
            removeThumbnails = false,
            removeXmpAndIptc = false,
            preserveDimensions = true
        )
    }
}

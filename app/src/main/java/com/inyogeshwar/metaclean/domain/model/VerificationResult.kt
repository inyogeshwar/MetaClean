package com.inyogeshwar.metaclean.domain.model

data class VerificationResult(
    val isVerifiedClean: Boolean,
    val removedFields: List<String>,
    val retainedFields: List<String>,
    val unremovableFields: List<String> = emptyList(),
    val originalSizeBytes: Long,
    val cleanedSizeBytes: Long,
    val sizeDifferenceBytes: Long,
    val sizeReductionPercentage: Double,
    val originalDimensions: Pair<Int, Int>,
    val cleanedDimensions: Pair<Int, Int>,
    val dimensionsPreserved: Boolean,
    val summaryNotes: String
)

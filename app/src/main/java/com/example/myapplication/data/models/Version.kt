package com.example.myapplication.data.models

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch
        if (suffix.isEmpty() && other.suffix.isNotEmpty()) return 1
        if (suffix.isNotEmpty() && other.suffix.isEmpty()) return -1
        if (suffix.isEmpty() && other.suffix.isEmpty()) return 0
        return suffix.compareTo(other.suffix, ignoreCase = true)
    }
}

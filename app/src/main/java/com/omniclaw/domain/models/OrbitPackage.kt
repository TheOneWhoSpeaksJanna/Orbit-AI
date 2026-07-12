package com.omniclaw.domain.models

data class OmniClawPackage(
    val id: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
    val binaryNames: List<String>,
    val binRelativePath: String,
    val isArchive: Boolean = true
)

data class InstalledPackage(
    val packageId: String,
    val installPath: String,
    val timestamp: Long
)

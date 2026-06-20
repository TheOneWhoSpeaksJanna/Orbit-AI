package com.omniclaw.domain.model

data class OmniClawPackage(
    val id: String,
    val name: String,
    val version: String,
    val downloadUrl: String,
    val binaryNames: List<String>,
    val binRelativePath: String, // Folder inside the extracted package where binaries live
    val isArchive: Boolean = true
)

data class InstalledPackage(
    val packageId: String,
    val installPath: String,
    val timestamp: Long
)

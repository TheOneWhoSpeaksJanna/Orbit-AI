package com.omniclaw.data.local.updater

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.omniclaw.BuildConfig
import com.omniclaw.data.local.prefs.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val isNewer: Boolean,
    val assetId: Long = 0
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val info: UpdateInfo) : UpdateState()
    data object UpToDate : UpdateState()
    data class Downloading(val progress: Float) : UpdateState()
    data class Downloaded(val filePath: String) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

class UpdateManager(
    private val context: Context,
    private val prefsManager: PreferencesManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private suspend fun githubToken(): String? {
        return prefsManager.githubToken.first()
    }

    private suspend fun buildAuthenticatedRequest(url: String): Request {
        val token = githubToken()
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder.build()
    }

    suspend fun checkForUpdates() {
        _updateState.value = UpdateState.Checking
        try {
            val result = withContext(Dispatchers.IO) {
                val response = httpClient.newCall(
                    buildAuthenticatedRequest("$API_BASE_URL/releases/latest")
                ).execute()

                if (response.code == 404) {
                    val listResponse = httpClient.newCall(
                        buildAuthenticatedRequest("$API_BASE_URL/releases?per_page=1")
                    ).execute()

                    if (!listResponse.isSuccessful || listResponse.body == null) {
                        return@withContext null
                    }
                    val listBody = listResponse.body!!.string()
                    val releasesArray = JSONArray(listBody)
                    if (releasesArray.length() == 0) {
                        return@withContext null
                    }
                    val json = releasesArray.getJSONObject(0)
                    parseReleaseJson(json)
                } else if (!response.isSuccessful) {
                    throw Exception("GitHub API returned ${response.code}")
                } else {
                    val body = response.body?.string() ?: throw Exception("Empty response")
                    val json = JSONObject(body)
                    parseReleaseJson(json)
                }
            }

            if (result == null) {
                _updateState.value = UpdateState.UpToDate
            } else if (result.isNewer && result.downloadUrl.isNotBlank()) {
                _updateState.value = UpdateState.Available(result)
            } else {
                _updateState.value = UpdateState.UpToDate
            }
        } catch (e: Exception) {
            _updateState.value = UpdateState.Failed("${CHECK_FAILED_PREFIX}${e.message}")
        }
    }

    suspend fun downloadUpdate(info: UpdateInfo) {
        _updateState.value = UpdateState.Downloading(0f)
        try {
            val apkFile = withContext(Dispatchers.IO) {
                val downloadsDir = File(context.cacheDir, "updates")
                downloadsDir.mkdirs()
                val file = File(downloadsDir, "Orbit-AI-${info.latestVersion}.apk")

                val token = githubToken()
                val apiUrl = "$API_BASE_URL/releases/assets/${info.assetId}"
                val requestBuilder = Request.Builder()
                    .url(apiUrl)
                    .header("Accept", "application/octet-stream")
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                val request = requestBuilder.build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) throw Exception("${DOWNLOAD_FAILED_PREFIX}${response.code}")

                val body = response.body ?: throw Exception("No response body")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                _updateState.value = UpdateState.Downloading(progress)
                            }
                        }
                    }
                }
                file
            }

            _updateState.value = UpdateState.Downloaded(apkFile.absolutePath)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Failed("${DOWNLOAD_FAILED_PREFIX}${e.message}")
        }
    }

    fun installApk(filePath: String) {
        try {
            val file = File(filePath)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Failed("${INSTALL_FAILED_PREFIX}${e.message}")
        }
    }

    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    private fun parseReleaseJson(json: JSONObject): UpdateInfo {
        val tagName = json.optString("tag_name", "").removePrefix("v")
        val rawNotes = json.optString("body", "")
        val releaseNotes = if (rawNotes == "null" || rawNotes.isBlank()) "" else rawNotes
        val assets = json.optJSONArray("assets")
        var downloadUrl = ""
        var assetId = 0L

        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    assetId = asset.optLong("id", 0)
                    break
                }
            }
        }

        val currentVersion = BuildConfig.VERSION_NAME
        val isNewer = compareVersions(tagName, currentVersion) > 0

        return UpdateInfo(
            latestVersion = tagName,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            isNewer = isNewer,
            assetId = assetId
        )
    }

    companion object {
        private const val REPO_OWNER = "TheOneWhoSpeaksJanna"
        private const val REPO_NAME = "Orbit-AI"
        private val API_BASE_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME"

        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val BUFFER_SIZE = 8192

        private const val CHECK_FAILED_PREFIX = "Check failed: "
        private const val DOWNLOAD_FAILED_PREFIX = "Download failed: "
        private const val INSTALL_FAILED_PREFIX = "Install failed: "
    }
}

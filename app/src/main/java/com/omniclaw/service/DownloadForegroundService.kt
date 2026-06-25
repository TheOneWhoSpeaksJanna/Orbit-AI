package com.omniclaw.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omniclaw.OmniClawApplication
import com.omniclaw.data.local.updater.UpdateInfo
import com.omniclaw.data.local.updater.UpdateState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DOWNLOAD -> handleDownload(intent)
            ACTION_INSTALL_FROM_NOTIFICATION -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                if (filePath != null) {
                    val app = application as OmniClawApplication
                    app.container.updateManager.installApk(filePath)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_CANCEL -> {
                serviceScope.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleDownload(intent: Intent) {
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(); return
        }
        val version = intent.getStringExtra(EXTRA_VERSION) ?: run {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(); return
        }
        val notes = intent.getStringExtra(EXTRA_RELEASE_NOTES) ?: ""

        val notification = buildNotification("Starting download...", 0, true)
        startForeground(NOTIFICATION_ID, notification)

        val info = UpdateInfo(
            latestVersion = version,
            downloadUrl = downloadUrl,
            releaseNotes = notes,
            isNewer = true
        )

        val app = application as OmniClawApplication

        serviceScope.launch {
            val observer = launch {
                app.container.updateManager.updateState.collect { state ->
                    when (state) {
                        is UpdateState.Downloading -> {
                            val pct = (state.progress * 100).toInt().coerceIn(0, 99)
                            updateNotification(pct)
                        }
                        is UpdateState.Downloaded -> {
                            showCompletionNotification(state.filePath)
                        }
                        is UpdateState.Failed -> {
                            showFailedNotification(state.message)
                        }
                        else -> {}
                    }
                }
            }

            try {
                app.container.updateManager.downloadUpdate(info)
            } catch (_: Exception) {
                // handled inside downloadUpdate
            }

            delay(500)
            observer.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Update Download",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows update download progress"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int, indeterminate: Boolean): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading update")
            .setContentText(text)
            .setProgress(100, progress, indeterminate)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(progress: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        val text = "$progress%"
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, false))
    }

    private fun showCompletionNotification(filePath: String) {
        val installIntent = Intent(this, DownloadForegroundService::class.java).apply {
            action = ACTION_INSTALL_FROM_NOTIFICATION
            putExtra(EXTRA_FILE_PATH, filePath)
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update downloaded")
            .setContentText("Tap to install")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showFailedNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Download failed")
            .setContentText(message)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_DOWNLOAD = "com.omniclaw.service.DOWNLOAD"
        const val ACTION_INSTALL_FROM_NOTIFICATION = "com.omniclaw.service.INSTALL"
        const val ACTION_CANCEL = "com.omniclaw.service.CANCEL"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_VERSION = "version"
        const val EXTRA_RELEASE_NOTES = "release_notes"
        const val EXTRA_FILE_PATH = "file_path"

        private const val CHANNEL_ID = "orbit_download"
        private const val NOTIFICATION_ID = 1001
    }
}

package com.skeler.pulse.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.skeler.pulse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.seconds

sealed interface VideoRecordingState {
    data object Idle : VideoRecordingState
    data class Recording(
        val startMs: Long,
        val file: File,
    ) : VideoRecordingState
    data class Completed(val file: File) : VideoRecordingState
}

class VideoRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.skeler.pulse.action.START_VIDEO_RECORDING"
        const val ACTION_STOP = "com.skeler.pulse.action.STOP_VIDEO_RECORDING"
        const val ACTION_CANCEL = "com.skeler.pulse.action.CANCEL_VIDEO_RECORDING"
        const val NOTIFICATION_ID = 9003
        const val CHANNEL_ID = "video_recording_channel"
        const val MAX_DURATION_MS = 30_000L
        private const val TAG = "VideoRecordingSvc"

        private val _state = MutableStateFlow<VideoRecordingState>(VideoRecordingState.Idle)
        val state: StateFlow<VideoRecordingState> = _state.asStateFlow()

        fun isRecording(): Boolean = _state.value is VideoRecordingState.Recording

        fun resetState() {
            _state.value = VideoRecordingState.Idle
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.video_recording_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.video_recording_channel_description)
                    setShowBadge(false)
                }
                context.getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(channel)
            }
        }
    }

    private var currentFile: File? = null
    private var startMs: Long = 0L
    private var stopPendingIntent: PendingIntent? = null
    private var cancelPendingIntent: PendingIntent? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startService()
            ACTION_STOP -> stopService(deleteFile = false)
            ACTION_CANCEL -> stopService(deleteFile = true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        currentFile?.let { file ->
            if (_state.value is VideoRecordingState.Recording) {
                file.delete()
                _state.value = VideoRecordingState.Idle
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startService() {
        startMs = System.currentTimeMillis()

        stopPendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, VideoRecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        cancelPendingIntent = PendingIntent.getService(
            this, 3,
            Intent(this, VideoRecordingService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            val nm = getSystemService(NotificationManager::class.java)
            while (isActive) {
                delay(1.seconds)
                val current = _state.value
                if (current !is VideoRecordingState.Recording) break
                val elapsed = (System.currentTimeMillis() - current.startMs) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                val timeText = "%d:%02d".format(minutes, seconds)
                nm.notify(NOTIFICATION_ID, buildNotification(timeText))
            }
        }
    }

    fun setRecordingFile(file: File) {
        currentFile = file
        _state.value = VideoRecordingState.Recording(
            startMs = startMs,
            file = file,
        )
    }

    fun completeRecording() {
        val file = currentFile
        if (file != null && file.exists() && file.length() > 0L) {
            _state.value = VideoRecordingState.Completed(file)
        } else {
            file?.delete()
            _state.value = VideoRecordingState.Idle
        }
        currentFile = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopService(deleteFile: Boolean) {
        serviceScope.coroutineContext.cancelChildren()
        val file = currentFile
        if (deleteFile || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            _state.value = VideoRecordingState.Idle
        } else {
            _state.value = VideoRecordingState.Completed(file)
        }
        currentFile = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(timeText: String? = null): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.video_recording_notification_title))
            .apply { if (timeText != null) setContentText(timeText) }
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.video_recording_stop),
                stopPendingIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_cancel),
                cancelPendingIntent,
            )
            .build()
    }
}

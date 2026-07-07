package com.skeler.pulse.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.skeler.pulse.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration.Companion.milliseconds
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface VoiceRecordingState {
    data object Idle : VoiceRecordingState
    data class Recording(
        val startMs: Long,
        val amplitudes: List<Float>,
        val file: File,
    ) : VoiceRecordingState
    data class Completed(val file: File) : VoiceRecordingState
}

class VoiceRecordingService : Service() {

    companion object {
        const val ACTION_START = "com.skeler.pulse.action.START_VOICE_RECORDING"
        const val ACTION_STOP = "com.skeler.pulse.action.STOP_VOICE_RECORDING"
        const val ACTION_CANCEL = "com.skeler.pulse.action.CANCEL_VOICE_RECORDING"
        const val NOTIFICATION_ID = 9002
        const val CHANNEL_ID = "voice_recording_channel"

        private val _state = MutableStateFlow<VoiceRecordingState>(VoiceRecordingState.Idle)
        val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

        fun isRecording(): Boolean = _state.value is VoiceRecordingState.Recording

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.voice_recording_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.voice_recording_channel_description)
                    setShowBadge(false)
                }
                context.getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(channel)
            }
        }
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startMs: Long = 0L
    private var isServiceDestroyed = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording(false)
            ACTION_CANCEL -> stopRecording(true)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isServiceDestroyed = true
        if (mediaRecorder != null) {
            val rec = mediaRecorder!!
            @Suppress("DEPRECATION")
            try {
                try { rec.stop() } catch (_: IllegalStateException) {}
            } finally {
                rec.release()
            }
            mediaRecorder = null
        }
        currentFile?.let { file ->
            if (_state.value is VoiceRecordingState.Recording) {
                file.delete()
                _state.value = VoiceRecordingState.Idle
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startRecording() {
        if (mediaRecorder != null) return

        val file = createVoiceFile(this)
        currentFile = file
        startMs = System.currentTimeMillis()

        try {
            @Suppress("DEPRECATION")
            val rec = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = rec
            _state.value = VoiceRecordingState.Recording(
                startMs = startMs,
                amplitudes = emptyList(),
                file = file,
            )

            startForeground(NOTIFICATION_ID, buildNotification())

            serviceScope.launch {
                val stopIntent = Intent(this@VoiceRecordingService, VoiceRecordingService::class.java).apply {
                    action = ACTION_STOP
                }
                val cancelIntent = Intent(this@VoiceRecordingService, VoiceRecordingService::class.java).apply {
                    action = ACTION_CANCEL
                }
                val stopPendingIntent = PendingIntent.getService(
                    this@VoiceRecordingService, 0, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val cancelPendingIntent = PendingIntent.getService(
                    this@VoiceRecordingService, 1, cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                while (isActive) {
                    delay(50.milliseconds)
                    if (isServiceDestroyed || mediaRecorder == null) break
                    val current = _state.value
                    if (current !is VoiceRecordingState.Recording) break
                    val amp = mediaRecorder?.maxAmplitude ?: 0
                    val normalized = (amp.toFloat() / 32767f).coerceIn(0f, 1f)
                    val amps = (current.amplitudes + normalized).takeLast(120)
                    _state.value = current.copy(amplitudes = amps)
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(amps, stopPendingIntent, cancelPendingIntent))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VoiceRecordingService", "Failed to start recording", e)
            file.delete()
            currentFile = null
            _state.value = VoiceRecordingState.Idle
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRecording(deleteFile: Boolean) {
        val file = currentFile
        val rec = mediaRecorder
        if (rec != null) {
            @Suppress("DEPRECATION")
            try {
                try { rec.stop() } catch (_: IllegalStateException) {}
            } finally {
                rec.release()
            }
            mediaRecorder = null
        }

        if (deleteFile || file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            _state.value = VoiceRecordingState.Idle
        } else {
            _state.value = VoiceRecordingState.Completed(file)
        }

        currentFile = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        amplitudes: List<Float> = emptyList(),
        stopPendingIntent: PendingIntent? = null,
        cancelPendingIntent: PendingIntent? = null,
    ): Notification {
        val elapsedMs = if (startMs > 0) System.currentTimeMillis() - startMs else 0L
        val totalSec = elapsedMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val timeText = "%d:%02d".format(min, sec)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.voice_recording_notification_title))
            .setContentText(timeText)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .apply {
                if (stopPendingIntent != null) {
                    addAction(
                        android.R.drawable.ic_media_pause,
                        getString(R.string.voice_recording_stop),
                        stopPendingIntent,
                    )
                }
                if (cancelPendingIntent != null) {
                    addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_cancel),
                        cancelPendingIntent,
                    )
                }
            }
            .build()
    }

    private fun createVoiceFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val voiceDir = File(context.cacheDir, "voice_messages")
        voiceDir.mkdirs()
        return File(voiceDir, "voice_$timeStamp.amr")
    }
}

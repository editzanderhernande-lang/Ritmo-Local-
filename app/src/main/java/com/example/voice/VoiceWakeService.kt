package com.example.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground microphone service that keeps CPU awake cleanly (PARTIAL_WAKE_LOCK) so offline
 * hands-free "Ridmo / Ritmo" voice commands continue working even with the screen turned off.
 */
class VoiceWakeService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VOICE_WAKE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val hasRecordPerm = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasRecordPerm) {
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            acquireWakeLockIfNeeded()
            _isRunning.value = true
            onServiceRunningChanged?.invoke(true)
        } catch (_: SecurityException) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun acquireWakeLockIfNeeded() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RitmoLocal:VoiceWakeLock")
                    ?.apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld == false) {
                // Acquire for up to 12 hours max as safety cap, re-acquired on service restart if needed
                wakeLock?.acquire(12L * 3600L * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Control de voz con pantalla apagada",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Permite escuchar 'Ridmo' sin internet mientras conduces o la pantalla está apagada."
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            101,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceWakeService::class.java).apply {
            action = ACTION_STOP_VOICE_WAKE
        }
        val stopPending = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_ritmo)
            .setColor(ContextCompat.getColor(this, R.color.ritmo_notif_accent))
            .setContentTitle("Ritmo Voz · Pantalla apagada activa")
            .setContentText("Di \"Ridmo siguiente canción\", \"anterior\", \"subir volumen\"…")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPending)
            .addAction(0, "Detener", stopPending)
            .build()
    }

    override fun onDestroy() {
        _isRunning.value = false
        onServiceRunningChanged?.invoke(false)
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "ritmo_voice_wake_channel"
        private const val NOTIFICATION_ID = 4208
        const val ACTION_STOP_VOICE_WAKE = "com.example.ritmolocal.STOP_VOICE_WAKE"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        @Volatile
        internal var onServiceRunningChanged: ((Boolean) -> Unit)? = null

        fun start(context: Context) {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) return
            val intent = Intent(context, VoiceWakeService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, VoiceWakeService::class.java))
            } catch (_: Exception) {
            }
        }
    }
}

package dev.ashera.slumbr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.ashera.slumbr.MainActivity
import dev.ashera.slumbr.R
import dev.ashera.slumbr.audio.AudioEngine
import dev.ashera.slumbr.audio.NoiseType

/**
 * Foreground service that keeps noise playback alive when the app is backgrounded
 * or the screen is off. Uses FOREGROUND_SERVICE_MEDIA_PLAYBACK type for battery
 * optimization exemption.
 */
class SoundService : Service() {
    companion object {
        private const val CHANNEL_ID = "slumbr_playback"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "dev.ashera.slumbr.STOP"

        fun startIntent(
            context: Context,
            noiseType: NoiseType,
            volume: Float,
        ): Intent =
            Intent(context, SoundService::class.java).apply {
                putExtra("noise_type", noiseType.name)
                putExtra("volume", volume)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = ACTION_STOP
            }
    }

    inner class LocalBinder : Binder() {
        val service: SoundService get() = this@SoundService
    }

    private val binder = LocalBinder()
    val audioEngine = AudioEngine()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            audioEngine.stop()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val noiseTypeName = intent?.getStringExtra("noise_type") ?: NoiseType.BROWN.name
        val noiseType = NoiseType.valueOf(noiseTypeName)
        val volume = intent?.getFloatExtra("volume", 0.8f) ?: 0.8f

        val notification = buildNotification(noiseType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        audioEngine.start(noiseType, volume)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        audioEngine.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.notification_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(noiseType: NoiseType): Notification {
        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val stopIntent =
            PendingIntent.getService(
                this,
                1,
                stopIntent(this),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text_playing, noiseType.displayName))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop),
                stopIntent,
            ).setOngoing(true)
            .setSilent(true)
            .build()
    }
}

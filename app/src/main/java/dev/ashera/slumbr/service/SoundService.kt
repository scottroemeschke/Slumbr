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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
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
        const val ACTION_GRACEFUL_STOP = "dev.ashera.slumbr.GRACEFUL_STOP"

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

        fun gracefulStopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = ACTION_GRACEFUL_STOP
            }
    }

    inner class LocalBinder : Binder() {
        val service: SoundService get() = this@SoundService
    }

    private val binder = LocalBinder()
    val audioEngine = AudioEngine()
    private var mediaSession: MediaSessionCompat? = null

    /** Called when the service is stopped via notification (hard stop). */
    var onServiceStopped: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
        audioEngine.onPlaybackComplete = {
            updateMediaSessionState(PlaybackStateCompat.STATE_STOPPED)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int =
        when (intent?.action) {
            ACTION_STOP -> {
                audioEngine.release()
                onServiceStopped?.invoke()
                updateMediaSessionState(PlaybackStateCompat.STATE_STOPPED)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                START_NOT_STICKY
            }
            ACTION_GRACEFUL_STOP -> {
                // Service self-stops via onPlaybackComplete when fade-out finishes
                audioEngine.stop()
                START_NOT_STICKY
            }
            else -> {
                val noiseTypeName =
                    intent?.getStringExtra("noise_type") ?: NoiseType.BROWN.name
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

                updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING)
                updateMediaSessionMetadata(noiseType)
                audioEngine.start(noiseType, volume)
                START_NOT_STICKY
            }
        }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        audioEngine.release()
        super.onDestroy()
    }

    fun updateNotification(noiseType: NoiseType) {
        val notification = buildNotification(noiseType)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
        updateMediaSessionMetadata(noiseType)
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "SlumbrSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onStop() {
                    // Hardware stop button / "OK Google, stop"
                    startService(stopIntent(this@SoundService))
                }
            })
            isActive = true
        }
    }

    private fun updateMediaSessionState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_STOP)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata(noiseType: NoiseType) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.notification_title))
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, noiseType.displayName)
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private fun createNotificationChannel() {
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

    private fun buildNotification(noiseType: NoiseType): Notification {
        val openIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val fadeOutIntent =
            PendingIntent.getService(
                this,
                1,
                gracefulStopIntent(this),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val stopIntent =
            PendingIntent.getService(
                this,
                2,
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
                getString(R.string.action_fade_out),
                fadeOutIntent,
            )
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.action_stop),
                stopIntent,
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}

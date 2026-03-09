package dev.ashera.slumbr.android.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dev.ashera.slumbr.core.audio.NoiseType
import dev.ashera.slumbr.core.playback.PlaybackCommand
import dev.ashera.slumbr.core.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class SoundService :
    Service(),
    ForegroundHost {
    companion object {
        private const val ACTION_STOP = "dev.ashera.slumbr.STOP"
        private const val ACTION_GRACEFUL_STOP = "dev.ashera.slumbr.GRACEFUL_STOP"
        private const val EXTRA_NOISE_TYPE = "noise_type"
        private const val EXTRA_VOLUME = "volume"
        private const val DEFAULT_VOLUME = 0.8f

        fun startIntent(
            context: Context,
            noiseType: NoiseType,
            volume: Float,
        ): Intent =
            Intent(context, SoundService::class.java).apply {
                putExtra(EXTRA_NOISE_TYPE, noiseType.name)
                putExtra(EXTRA_VOLUME, volume)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = ACTION_STOP
            }

        fun gracefulStopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = ACTION_GRACEFUL_STOP
            }

        private fun commandFrom(intent: Intent?): PlaybackCommand? =
            when (intent?.action) {
                ACTION_STOP -> PlaybackCommand.HardStop
                ACTION_GRACEFUL_STOP -> PlaybackCommand.GracefulStop
                else -> {
                    val noiseTypeName = intent?.getStringExtra(EXTRA_NOISE_TYPE) ?: return null
                    val noiseType =
                        NoiseType.entries.find { it.name == noiseTypeName } ?: return null
                    val volume = intent.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME)
                    PlaybackCommand.Start(noiseType, volume)
                }
            }
    }

    @Inject lateinit var playbackController: PlaybackController

    @Inject lateinit var notifier: PlaybackNotifier

    @Inject lateinit var mediaSessionController: MediaSessionController

    @Inject lateinit var playbackObserver: ServicePlaybackObserver

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
        mediaSessionController.initialize { playbackController.handleCommand(PlaybackCommand.HardStop) }
        playbackObserver.observe(serviceScope, this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        playbackController.handleCommand(commandFrom(intent))
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSessionController.release()
        super.onDestroy()
    }

    override fun promoteForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                AndroidPlaybackNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(AndroidPlaybackNotifier.NOTIFICATION_ID, notification)
        }
    }

    override fun demoteForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}

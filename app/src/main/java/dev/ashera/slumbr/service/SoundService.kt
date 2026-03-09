package dev.ashera.slumbr.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.playback.PlaybackCommand
import dev.ashera.slumbr.playback.PlaybackController
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
        fun startIntent(
            context: Context,
            noiseType: NoiseType,
            volume: Float,
        ): Intent =
            Intent(context, SoundService::class.java).apply {
                putExtra(PlaybackCommand.EXTRA_NOISE_TYPE, noiseType.name)
                putExtra(PlaybackCommand.EXTRA_VOLUME, volume)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = PlaybackCommand.ACTION_STOP
            }

        fun gracefulStopIntent(context: Context): Intent =
            Intent(context, SoundService::class.java).apply {
                action = PlaybackCommand.ACTION_GRACEFUL_STOP
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
        playbackController.handleCommand(PlaybackCommand.from(intent))
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

package dev.ashera.slumbr.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.playback.PlaybackCommand
import dev.ashera.slumbr.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SoundService : Service() {
    companion object {
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

    @Inject lateinit var playbackController: PlaybackController

    @Inject lateinit var notificationManager: PlaybackNotificationManager

    @Inject lateinit var mediaSessionManager: PlaybackMediaSessionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager.createChannel()
        mediaSessionManager.init(playbackController)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (val command = PlaybackCommand.from(intent)) {
            is PlaybackCommand.Start -> {
                val notification =
                    notificationManager.buildNotification(
                        command.noiseType,
                        mediaSessionManager.sessionToken,
                    )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        PlaybackNotificationManager.NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                    )
                } else {
                    startForeground(PlaybackNotificationManager.NOTIFICATION_ID, notification)
                }
                mediaSessionManager.updateState(PlaybackStateCompat.STATE_PLAYING)
                mediaSessionManager.updateMetadata(command.noiseType)
                observePlaybackState()
            }
            is PlaybackCommand.HardStop -> playbackController.hardStop()
            is PlaybackCommand.GracefulStop -> playbackController.gracefulStop()
            else -> {}
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSessionManager.release()
        super.onDestroy()
    }

    private fun observePlaybackState() {
        if (observing) return
        observing = true

        serviceScope.launch {
            playbackController.playbackState
                .map { it.currentNoise }
                .distinctUntilChanged()
                .collect { noiseType ->
                    if (noiseType != null) {
                        notificationManager.updateNotification(
                            noiseType,
                            mediaSessionManager.sessionToken,
                        )
                        mediaSessionManager.updateMetadata(noiseType)
                    }
                }
        }

        serviceScope.launch {
            playbackController.playbackState
                .map { it.isPlaying }
                .distinctUntilChanged()
                .collect { isPlaying ->
                    if (!isPlaying) {
                        mediaSessionManager.updateState(PlaybackStateCompat.STATE_STOPPED)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
        }
    }
}

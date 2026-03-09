package dev.ashera.slumbr.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ashera.slumbr.MainActivity
import dev.ashera.slumbr.R
import dev.ashera.slumbr.audio.NoiseType
import javax.inject.Inject

class PlaybackNotificationManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        companion object {
            const val CHANNEL_ID = "slumbr_playback"
            const val NOTIFICATION_ID = 1
        }

        fun createChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    setSound(null, null)
                    enableVibration(false)
                }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        fun buildNotification(
            noiseType: NoiseType,
            mediaSessionToken: MediaSessionCompat.Token?,
        ): Notification {
            val openIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val fadeOutIntent =
                PendingIntent.getService(
                    context,
                    1,
                    SoundService.gracefulStopIntent(context),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val stopIntent =
                PendingIntent.getService(
                    context,
                    2,
                    SoundService.stopIntent(context),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(
                    context.getString(R.string.notification_text_playing, noiseType.displayName),
                ).setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentIntent(openIntent)
                .addAction(
                    R.drawable.ic_notification_fade_out,
                    context.getString(R.string.action_fade_out),
                    fadeOutIntent,
                ).addAction(
                    R.drawable.ic_notification_stop,
                    context.getString(R.string.action_stop),
                    stopIntent,
                ).setStyle(
                    MediaStyle()
                        .setMediaSession(mediaSessionToken)
                        .setShowActionsInCompactView(0, 1),
                ).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setSilent(true)
                .build()
        }

        fun updateNotification(
            noiseType: NoiseType,
            mediaSessionToken: MediaSessionCompat.Token?,
        ) {
            val notification = buildNotification(noiseType, mediaSessionToken)
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

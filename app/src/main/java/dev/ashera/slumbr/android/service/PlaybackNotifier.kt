package dev.ashera.slumbr.android.service

import android.app.Notification
import dev.ashera.slumbr.core.audio.NoiseType

interface PlaybackNotifier {
    fun createChannel()

    fun buildNotification(noiseType: NoiseType): Notification

    fun updateNotification(noiseType: NoiseType)
}

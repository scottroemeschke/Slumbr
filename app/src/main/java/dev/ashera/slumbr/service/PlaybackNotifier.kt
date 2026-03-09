package dev.ashera.slumbr.service

import android.app.Notification
import dev.ashera.slumbr.audio.NoiseType

interface PlaybackNotifier {
    fun createChannel()

    fun buildNotification(noiseType: NoiseType): Notification

    fun updateNotification(noiseType: NoiseType)
}

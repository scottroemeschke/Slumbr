package dev.ashera.slumbr.service

import android.app.Notification
import dev.ashera.slumbr.audio.NoiseType

class FakePlaybackNotifier : PlaybackNotifier {
    var channelCreated = false
        private set
    var lastBuiltNoiseType: NoiseType? = null
        private set
    var lastUpdatedNoiseType: NoiseType? = null
        private set

    override fun createChannel() {
        channelCreated = true
    }

    override fun buildNotification(noiseType: NoiseType): Notification {
        lastBuiltNoiseType = noiseType
        // Return a stub — tests don't inspect the Notification object itself
        @Suppress("DEPRECATION")
        return Notification()
    }

    override fun updateNotification(noiseType: NoiseType) {
        lastUpdatedNoiseType = noiseType
    }
}

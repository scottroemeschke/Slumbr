package dev.ashera.slumbr.service

import android.support.v4.media.session.MediaSessionCompat
import dev.ashera.slumbr.audio.NoiseType

class FakeMediaSessionController : MediaSessionController {
    override val sessionToken: MediaSessionCompat.Token? = null

    var onStopCallback: (() -> Unit)? = null
        private set
    var lastPlayingNoise: NoiseType? = null
        private set
    var stoppedCalled = false
        private set
    var released = false
        private set

    override fun initialize(onStop: () -> Unit) {
        onStopCallback = onStop
    }

    override fun updatePlaying(noiseType: NoiseType) {
        lastPlayingNoise = noiseType
    }

    override fun updateStopped() {
        stoppedCalled = true
    }

    override fun release() {
        released = true
    }
}

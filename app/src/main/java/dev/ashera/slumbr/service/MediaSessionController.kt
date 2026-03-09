package dev.ashera.slumbr.service

import android.support.v4.media.session.MediaSessionCompat
import dev.ashera.slumbr.audio.NoiseType

interface MediaSessionController {
    val sessionToken: MediaSessionCompat.Token?

    fun initialize(onStop: () -> Unit)

    fun updatePlaying(noiseType: NoiseType)

    fun updateStopped()

    fun release()
}

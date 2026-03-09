package dev.ashera.slumbr.android.service

import android.support.v4.media.session.MediaSessionCompat
import dev.ashera.slumbr.core.audio.NoiseType

interface MediaSessionController {
    val sessionToken: MediaSessionCompat.Token?

    fun initialize(onStop: () -> Unit)

    fun updatePlaying(noiseType: NoiseType)

    fun updateStopped()

    fun release()
}

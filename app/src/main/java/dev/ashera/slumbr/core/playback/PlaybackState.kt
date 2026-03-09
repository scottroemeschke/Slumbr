package dev.ashera.slumbr.core.playback

import dev.ashera.slumbr.core.audio.NoiseType

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentNoise: NoiseType? = null,
    val fadeProgress: Float = 0f,
)

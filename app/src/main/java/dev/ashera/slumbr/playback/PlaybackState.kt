package dev.ashera.slumbr.playback

import dev.ashera.slumbr.audio.NoiseType

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentNoise: NoiseType? = null,
    val fadeProgress: Float = 0f,
)

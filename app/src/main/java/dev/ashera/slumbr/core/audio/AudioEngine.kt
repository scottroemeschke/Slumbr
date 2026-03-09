package dev.ashera.slumbr.core.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioEngine {
    companion object {
        const val SAMPLE_RATE = 22050
    }

    val fadeProgress: StateFlow<Float>
    var onPlaybackComplete: (() -> Unit)?
    val isPlaying: Boolean

    fun start(
        noiseType: NoiseType,
        volume: Float = 0.8f,
    )

    fun switchNoise(noiseType: NoiseType)

    fun setVolume(volume: Float)

    fun stop()

    fun release()
}

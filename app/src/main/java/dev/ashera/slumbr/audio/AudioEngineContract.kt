package dev.ashera.slumbr.audio

import kotlinx.coroutines.flow.StateFlow

interface AudioEngineContract {
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

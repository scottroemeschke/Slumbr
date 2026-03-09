package dev.ashera.slumbr.playback

import dev.ashera.slumbr.audio.AudioEngineContract
import dev.ashera.slumbr.audio.NoiseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAudioEngine : AudioEngineContract {
    private val _fadeProgress = MutableStateFlow(0f)
    override val fadeProgress: StateFlow<Float> = _fadeProgress.asStateFlow()
    override var onPlaybackComplete: (() -> Unit)? = null

    var startedNoiseType: NoiseType? = null
        private set
    var startedVolume: Float? = null
        private set
    var switchedNoiseType: NoiseType? = null
        private set
    var lastVolume: Float? = null
        private set
    var stopCalled = false
        private set
    var releaseCalled = false
        private set
    private var _isPlaying = false

    override val isPlaying: Boolean get() = _isPlaying

    override fun start(
        noiseType: NoiseType,
        volume: Float,
    ) {
        _isPlaying = true
        startedNoiseType = noiseType
        startedVolume = volume
        stopCalled = false
        releaseCalled = false
    }

    override fun switchNoise(noiseType: NoiseType) {
        switchedNoiseType = noiseType
    }

    override fun setVolume(volume: Float) {
        lastVolume = volume
    }

    override fun stop() {
        stopCalled = true
    }

    override fun release() {
        releaseCalled = true
        _isPlaying = false
        _fadeProgress.value = 0f
    }

    fun simulateFadeOutComplete() {
        _isPlaying = false
        _fadeProgress.value = 0f
        onPlaybackComplete?.invoke()
    }
}

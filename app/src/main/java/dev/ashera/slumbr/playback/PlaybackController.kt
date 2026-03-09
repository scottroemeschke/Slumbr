package dev.ashera.slumbr.playback

import dev.ashera.slumbr.audio.AudioEngineContract
import dev.ashera.slumbr.audio.NoiseType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackController
    @Inject
    constructor(
        private val audioEngine: AudioEngineContract,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val _playbackState = MutableStateFlow(PlaybackState())
        val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

        init {
            audioEngine.onPlaybackComplete = {
                _playbackState.update { PlaybackState() }
            }
            scope.launch {
                audioEngine.fadeProgress.collect { progress ->
                    _playbackState.update { it.copy(fadeProgress = progress) }
                }
            }
        }

        fun start(
            noiseType: NoiseType,
            volume: Float,
        ) {
            // Idempotent: ViewModel calls this for immediate UI feedback,
            // then Service calls it again to ensure audio starts even without ViewModel.
            if (_playbackState.value.isPlaying) return
            audioEngine.start(noiseType, volume)
            _playbackState.update {
                PlaybackState(isPlaying = true, currentNoise = noiseType, fadeProgress = 0f)
            }
        }

        fun switchNoise(noiseType: NoiseType) {
            audioEngine.switchNoise(noiseType)
            _playbackState.update { it.copy(currentNoise = noiseType) }
        }

        fun setVolume(volume: Float) {
            audioEngine.setVolume(volume)
        }

        fun gracefulStop() {
            audioEngine.stop()
            // State will be updated via onPlaybackComplete when fade-out finishes
        }

        fun hardStop() {
            audioEngine.release()
            _playbackState.update { PlaybackState() }
        }

        fun release() {
            audioEngine.release()
            _playbackState.update { PlaybackState() }
        }
    }

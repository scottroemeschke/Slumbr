package dev.ashera.slumbr.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.ashera.slumbr.core.audio.NoiseType
import dev.ashera.slumbr.core.playback.PlaybackController
import dev.ashera.slumbr.core.system.DndStateProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SoundUiState(
    val selectedNoise: NoiseType? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 0.8f,
    val instantTransition: Boolean = false,
)

@HiltViewModel
class SoundViewModel
    @Inject
    constructor(
        private val playbackController: PlaybackController,
        private val dndStateProvider: DndStateProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SoundUiState())
        val uiState: StateFlow<SoundUiState> = _uiState.asStateFlow()

        private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

        init {
            viewModelScope.launch {
                playbackController.playbackState.collect { playback ->
                    _uiState.update {
                        it.copy(
                            isPlaying = playback.isPlaying,
                            selectedNoise = if (playback.isPlaying) playback.currentNoise else null,
                        )
                    }
                }
            }
        }

        fun selectNoise(noiseType: NoiseType) {
            val current = _uiState.value
            when {
                current.isPlaying && current.selectedNoise == noiseType -> {
                    // Toggle off — graceful fade-out
                    _uiState.update { it.copy(instantTransition = false) }
                    playbackController.gracefulStop()
                }
                current.isPlaying && current.selectedNoise != null -> {
                    // Switch noise type
                    _uiState.update { it.copy(instantTransition = true) }
                    playbackController.switchNoise(noiseType)
                    checkDndTotalSilence()
                }
                else -> {
                    // Start from stopped
                    _uiState.update { it.copy(instantTransition = false) }
                    playbackController.start(noiseType, current.volume)
                    checkDndTotalSilence()
                }
            }
        }

        fun setVolume(volume: Float) {
            _uiState.update { it.copy(volume = volume) }
            playbackController.setVolume(volume)
        }

        fun showSnackbar(message: String) {
            _snackbarMessages.tryEmit(message)
        }

        private fun checkDndTotalSilence() {
            if (dndStateProvider.isTotalSilence()) {
                _snackbarMessages.tryEmit(DND_WARNING)
            }
        }

        companion object {
            const val DND_WARNING = "Do Not Disturb (Total Silence) is on \u2014 audio may be muted"
        }
    }

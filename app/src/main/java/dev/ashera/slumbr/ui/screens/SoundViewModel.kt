package dev.ashera.slumbr.ui.screens

import androidx.lifecycle.ViewModel
import dev.ashera.slumbr.audio.NoiseType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SoundUiState(
    val selectedNoise: NoiseType? = null,
    val isPlaying: Boolean = false,
    val volume: Float = 0.8f,
)

class SoundViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SoundUiState())
    val uiState: StateFlow<SoundUiState> = _uiState.asStateFlow()

    fun selectNoise(noiseType: NoiseType) {
        _uiState.update {
            if (it.selectedNoise == noiseType && it.isPlaying) {
                it.copy(isPlaying = false, selectedNoise = null)
            } else {
                it.copy(selectedNoise = noiseType, isPlaying = true)
            }
        }
    }

    fun setVolume(volume: Float) {
        _uiState.update { it.copy(volume = volume) }
    }

    fun stop() {
        _uiState.update { it.copy(isPlaying = false, selectedNoise = null) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }
}

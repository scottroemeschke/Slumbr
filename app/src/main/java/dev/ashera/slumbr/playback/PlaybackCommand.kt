package dev.ashera.slumbr.playback

import android.content.Intent
import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.service.SoundService

sealed class PlaybackCommand {
    data class Start(
        val noiseType: NoiseType,
        val volume: Float,
    ) : PlaybackCommand()

    data class SwitchNoise(
        val noiseType: NoiseType,
    ) : PlaybackCommand()

    data class SetVolume(
        val volume: Float,
    ) : PlaybackCommand()

    data object GracefulStop : PlaybackCommand()

    data object HardStop : PlaybackCommand()

    companion object {
        private const val EXTRA_NOISE_TYPE = "noise_type"
        private const val EXTRA_VOLUME = "volume"
        private const val DEFAULT_VOLUME = 0.8f

        fun from(intent: Intent?): PlaybackCommand? =
            when (intent?.action) {
                SoundService.ACTION_STOP -> HardStop
                SoundService.ACTION_GRACEFUL_STOP -> GracefulStop
                else -> {
                    val noiseTypeName = intent?.getStringExtra(EXTRA_NOISE_TYPE) ?: return null
                    val noiseType = NoiseType.valueOf(noiseTypeName)
                    val volume = intent.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME)
                    Start(noiseType, volume)
                }
            }
    }
}

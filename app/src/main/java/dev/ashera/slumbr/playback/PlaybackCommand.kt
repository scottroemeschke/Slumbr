package dev.ashera.slumbr.playback

import android.content.Intent
import dev.ashera.slumbr.audio.NoiseType

sealed class PlaybackCommand {
    data class Start(
        val noiseType: NoiseType,
        val volume: Float,
    ) : PlaybackCommand()

    data object GracefulStop : PlaybackCommand()

    data object HardStop : PlaybackCommand()

    companion object {
        const val ACTION_STOP = "dev.ashera.slumbr.STOP"
        const val ACTION_GRACEFUL_STOP = "dev.ashera.slumbr.GRACEFUL_STOP"
        const val EXTRA_NOISE_TYPE = "noise_type"
        const val EXTRA_VOLUME = "volume"
        private const val DEFAULT_VOLUME = 0.8f

        fun from(intent: Intent?): PlaybackCommand? =
            when (intent?.action) {
                ACTION_STOP -> HardStop
                ACTION_GRACEFUL_STOP -> GracefulStop
                else -> {
                    val noiseTypeName = intent?.getStringExtra(EXTRA_NOISE_TYPE) ?: return null
                    val noiseType =
                        NoiseType.entries.find { it.name == noiseTypeName } ?: return null
                    val volume = intent.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME)
                    Start(noiseType, volume)
                }
            }
    }
}

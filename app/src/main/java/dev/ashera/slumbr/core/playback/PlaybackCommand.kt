package dev.ashera.slumbr.core.playback

import dev.ashera.slumbr.core.audio.NoiseType

sealed class PlaybackCommand {
    data class Start(
        val noiseType: NoiseType,
        val volume: Float,
    ) : PlaybackCommand()

    data object GracefulStop : PlaybackCommand()

    data object HardStop : PlaybackCommand()
}

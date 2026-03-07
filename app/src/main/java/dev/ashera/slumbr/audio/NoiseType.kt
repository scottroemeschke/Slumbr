package dev.ashera.slumbr.audio

enum class NoiseType(
    val displayName: String,
    /** Gain factor to normalize perceived loudness across noise colors. */
    val perceptualGain: Float,
) {
    WHITE("White", 0.5f),
    PINK("Pink", 0.8f),
    BROWN("Brown", 1.6f),
}

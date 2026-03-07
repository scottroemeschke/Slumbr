package dev.ashera.slumbr.audio

enum class NoiseType(
    val displayName: String,
    /** Gain factor to normalize perceived loudness across noise colors. */
    val perceptualGain: Float,
) {
    WHITE("White", 0.35f),
    PINK("Pink", 0.75f),
    BROWN("Brown", 2.5f),
}

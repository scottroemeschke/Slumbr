package dev.ashera.slumbr.audio

@Suppress("MagicNumber") // Gain factors are empirically tuned perceptual constants
enum class NoiseType(
    val displayName: String,
    /** Gain factor to normalize perceived loudness across noise colors. */
    val perceptualGain: Float,
) {
    /** Attenuated — flat spectrum with strong HF energy sounds loudest on any speaker. */
    WHITE("White", 0.35f),

    /** Moderate attenuation — balanced spectrum, moderate perceived loudness. */
    PINK("Pink", 0.75f),

    /** Boosted — most energy is in LF that phone speakers can't reproduce. */
    BROWN("Brown", 2.5f),
}

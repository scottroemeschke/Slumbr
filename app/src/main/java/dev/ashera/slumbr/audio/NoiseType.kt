package dev.ashera.slumbr.audio

@Suppress("MagicNumber") // Gain factors are empirically tuned perceptual constants
enum class NoiseType(
    val displayName: String,
    /** Gain factor to normalize perceived loudness across noise colors. */
    val perceptualGain: Float,
) {
    /** Attenuated — low-pass shaped, gentle rolloff above ~4 kHz. */
    WHITE("White Noise", 0.44f),

    /** Moderate attenuation — Voss-McCartney with gentle HF smoothing above ~6 kHz. */
    PINK("Pink Noise", 0.85f),

    /** Boosted — leaky integrator with high-pass at ~30 Hz and soft clipping. */
    BROWN("Brown Noise", 2.76f),
    ;

    /** Create the [SoundGenerator] for this noise type. */
    fun createGenerator(): SoundGenerator = NoiseGenerator(this)
}

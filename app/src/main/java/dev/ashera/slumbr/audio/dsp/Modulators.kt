package dev.ashera.slumbr.audio.dsp

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Low-frequency oscillator for slow modulation (breathing, drift, gust envelopes).
 * Produces values in [0, 1] by default.
 *
 * @param frequencyHz oscillation frequency (typically 0.01–2 Hz for sleep sounds)
 * @param sampleRate audio sample rate
 */
@Suppress("MagicNumber")
class Lfo(
    private var frequencyHz: Float,
    private val sampleRate: Int,
) {
    private var phase = Random.nextFloat() // randomize start phase

    /** Returns next LFO value in [0, 1]. */
    fun next(): Float {
        val value = ((sin(2.0 * PI * phase).toFloat() + 1f) / 2f).coerceIn(0f, 1f)
        phase += frequencyHz / sampleRate
        if (phase >= 1f) phase -= 1f
        return value
    }

    fun setFrequency(hz: Float) {
        frequencyHz = hz
    }
}

/**
 * Slow random drift generator — produces smoothly varying values in [0, 1].
 * Uses filtered white noise for organic, non-periodic modulation.
 *
 * @param driftRateHz approximate rate of variation (controls low-pass cutoff)
 * @param sampleRate audio sample rate
 */
@Suppress("MagicNumber")
class DriftModulator(
    driftRateHz: Float,
    sampleRate: Int,
) {
    private val filter = LowPassFilter(driftRateHz, sampleRate)

    /** Returns next drift value in [0, 1]. */
    fun next(): Float {
        val noise = Random.nextFloat() * 2f - 1f
        val filtered = filter.process(noise)
        // Map filtered noise [-1, 1] to [0, 1]
        return ((filtered + 1f) / 2f).coerceIn(0f, 1f)
    }
}

/**
 * Simple AR (attack-release) envelope for transient events like droplets.
 *
 * @param attackSamples number of samples for attack ramp
 * @param releaseSamples number of samples for release ramp
 */
class ArEnvelope(
    private val attackSamples: Int,
    private val releaseSamples: Int,
) {
    init {
        require(attackSamples > 0) { "attackSamples must be positive" }
        require(releaseSamples > 0) { "releaseSamples must be positive" }
    }

    private var position = 0
    private val totalSamples = attackSamples + releaseSamples
    private var active = false

    fun trigger() {
        position = 0
        active = true
    }

    val isActive: Boolean get() = active

    /** Returns envelope value in [0, 1], or 0 if inactive. */
    fun next(): Float {
        if (!active) return 0f
        val value =
            if (position <= attackSamples) {
                position.toFloat() / attackSamples
            } else {
                val releasePos = position - attackSamples
                1f - releasePos.toFloat() / releaseSamples
            }
        position++
        if (position >= totalSamples) {
            active = false
        }
        return value.coerceIn(0f, 1f)
    }
}

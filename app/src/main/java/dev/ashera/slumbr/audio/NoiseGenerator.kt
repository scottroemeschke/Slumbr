package dev.ashera.slumbr.audio

import kotlin.math.min
import kotlin.random.Random

/**
 * Generates PCM noise samples for different noise colors.
 * All generators produce mono float samples in [-1, 1].
 */
class NoiseGenerator(
    private val type: NoiseType,
) {
    // Brown noise state
    private var brownLast = 0.0

    // Pink noise state (Voss-McCartney algorithm, 8 octaves)
    private val pinkOctaves = DoubleArray(8)
    private var pinkCounter = 0
    private var pinkRunningSum = 0.0

    init {
        // Initialize pink noise octaves
        for (i in pinkOctaves.indices) {
            val value = Random.nextDouble(-1.0, 1.0)
            pinkOctaves[i] = value
            pinkRunningSum += value
        }
    }

    fun nextSample(): Float =
        when (type) {
            NoiseType.WHITE -> white()
            NoiseType.PINK -> pink()
            NoiseType.BROWN -> brown()
        }

    private fun white(): Float = Random.nextFloat() * 2f - 1f

    private fun pink(): Float {
        // Voss-McCartney: update one octave per sample based on trailing zeros of counter
        val k = Integer.numberOfTrailingZeros(pinkCounter)
        val octave = min(k, pinkOctaves.size - 1)
        pinkRunningSum -= pinkOctaves[octave]
        val newValue = Random.nextDouble(-1.0, 1.0)
        pinkOctaves[octave] = newValue
        pinkRunningSum += newValue
        pinkCounter++

        // Normalize: sum of 8 uniform [-1,1] has max magnitude 8
        return (pinkRunningSum / pinkOctaves.size).toFloat()
    }

    private fun brown(): Float {
        // Brownian/red noise: integrated white noise with leaky integrator
        brownLast += Random.nextDouble(-1.0, 1.0) * BROWN_STEP_SCALE
        brownLast *= BROWN_LEAK_FACTOR
        brownLast = brownLast.coerceIn(-1.0, 1.0)
        return brownLast.toFloat()
    }

    companion object {
        private const val BROWN_STEP_SCALE = 0.02
        private const val BROWN_LEAK_FACTOR = 0.998
    }
}

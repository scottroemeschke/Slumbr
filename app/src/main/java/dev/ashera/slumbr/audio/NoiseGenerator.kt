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
    private var brownLast = 0f

    // Pink noise state (Voss-McCartney algorithm, 8 octaves)
    private val pinkOctaves = FloatArray(8)
    private var pinkCounter = 0
    private var pinkRunningSum = 0f

    init {
        for (i in pinkOctaves.indices) {
            val value = Random.nextFloat() * 2f - 1f
            pinkOctaves[i] = value
            pinkRunningSum += value
        }
    }

    fun nextSample(): Float {
        val raw =
            when (type) {
                NoiseType.WHITE -> white()
                NoiseType.PINK -> pink()
                NoiseType.BROWN -> brown()
            }
        return (raw * type.perceptualGain).coerceIn(-1f, 1f)
    }

    /**
     * Fill [buffer] with gain-adjusted samples. Avoids per-sample virtual dispatch
     * by selecting the generation strategy once and looping internally.
     */
    fun fillBuffer(buffer: FloatArray) {
        val gain = type.perceptualGain
        when (type) {
            NoiseType.WHITE -> for (i in buffer.indices) {
                buffer[i] = (white() * gain).coerceIn(-1f, 1f)
            }
            NoiseType.PINK -> for (i in buffer.indices) {
                buffer[i] = (pink() * gain).coerceIn(-1f, 1f)
            }
            NoiseType.BROWN -> for (i in buffer.indices) {
                buffer[i] = (brown() * gain).coerceIn(-1f, 1f)
            }
        }
    }

    private fun white(): Float = Random.nextFloat() * 2f - 1f

    private fun pink(): Float {
        // Voss-McCartney: update one octave per sample based on trailing zeros of counter
        val k = Integer.numberOfTrailingZeros(pinkCounter)
        val octave = min(k, pinkOctaves.size - 1)
        pinkRunningSum -= pinkOctaves[octave]
        val newValue = Random.nextFloat() * 2f - 1f
        pinkOctaves[octave] = newValue
        pinkRunningSum += newValue
        pinkCounter++

        // Normalize: sum of 8 uniform [-1,1] has max magnitude 8
        return pinkRunningSum / pinkOctaves.size
    }

    private fun brown(): Float {
        // Brownian/red noise: integrated white noise with leaky integrator
        brownLast += (Random.nextFloat() * 2f - 1f) * BROWN_STEP_SCALE
        brownLast *= BROWN_LEAK_FACTOR
        brownLast = brownLast.coerceIn(-1f, 1f)
        return brownLast
    }

    companion object {
        private const val BROWN_STEP_SCALE = 0.02f
        private const val BROWN_LEAK_FACTOR = 0.998f
    }
}

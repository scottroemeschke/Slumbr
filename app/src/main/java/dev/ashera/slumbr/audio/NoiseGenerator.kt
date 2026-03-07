package dev.ashera.slumbr.audio

import kotlin.math.min
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Generates PCM noise samples for different noise colors.
 * All generators produce mono float samples in [-1, 1].
 *
 * Each noise type includes spectral shaping for pleasant sleep/masking sound:
 * - White: single-pole low-pass (~4 kHz) to roll off harsh highs
 * - Pink: Voss-McCartney + single-pole low-pass (~6 kHz) to smooth top-end
 * - Brown: leaky integrator + tanh soft clipping + high-pass (~30 Hz) for speaker safety
 */
@Suppress("MagicNumber") // DSP constants are empirically tuned
class NoiseGenerator(
    private val type: NoiseType,
) {
    // Brown noise state
    private var brownLast = 0f

    // Pink noise state (Voss-McCartney algorithm, 8 octaves)
    private val pinkOctaves = FloatArray(8)
    private var pinkCounter = 0
    private var pinkRunningSum = 0f

    // Filter state
    private var lpState = 0f
    private var hpState = 0f
    private var hpPrevInput = 0f

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

    /** White noise through single-pole low-pass at ~4 kHz. */
    private fun white(): Float {
        val x = Random.nextFloat() * 2f - 1f
        lpState += LP_ALPHA_WHITE * (x - lpState)
        return lpState
    }

    /** Voss-McCartney pink noise through single-pole low-pass at ~6 kHz. */
    private fun pink(): Float {
        val k = Integer.numberOfTrailingZeros(pinkCounter)
        val octave = min(k, pinkOctaves.size - 1)
        pinkRunningSum -= pinkOctaves[octave]
        val newValue = Random.nextFloat() * 2f - 1f
        pinkOctaves[octave] = newValue
        pinkRunningSum += newValue
        pinkCounter++

        val raw = pinkRunningSum / pinkOctaves.size
        lpState += LP_ALPHA_PINK * (raw - lpState)
        return lpState
    }

    /** Brown noise with soft clipping + high-pass at ~30 Hz. */
    private fun brown(): Float {
        brownLast += (Random.nextFloat() * 2f - 1f) * BROWN_STEP_SCALE
        brownLast *= BROWN_LEAK_FACTOR
        // Soft clip: tanh saturation instead of hard clamp
        val clipped = tanh(brownLast.toDouble() * SOFT_CLIP_DRIVE).toFloat()
        // Single-pole high-pass: removes sub-bass rumble
        val hpOutput = HP_ALPHA_BROWN * (hpState + clipped - hpPrevInput)
        hpPrevInput = clipped
        hpState = hpOutput
        return hpOutput
    }

    companion object {
        private const val BROWN_STEP_SCALE = 0.02f
        private const val BROWN_LEAK_FACTOR = 0.998f

        // Single-pole low-pass: α = 2πfc/fs / (1 + 2πfc/fs)
        private const val LP_ALPHA_WHITE = 0.53f // ~4 kHz cutoff at 22050 Hz
        private const val LP_ALPHA_PINK = 0.63f // ~6 kHz cutoff at 22050 Hz

        // Single-pole high-pass: α = 1 / (1 + 2πfc/fs)
        private const val HP_ALPHA_BROWN = 0.991f // ~30 Hz cutoff at 22050 Hz

        // tanh soft-clip drive — 1.5 gives gentle saturation
        private const val SOFT_CLIP_DRIVE = 1.5
    }
}

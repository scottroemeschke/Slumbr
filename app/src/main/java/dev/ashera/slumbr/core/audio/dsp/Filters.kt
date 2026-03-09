package dev.ashera.slumbr.core.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Single-pole IIR low-pass filter.
 * Cheap and effective for gentle spectral shaping.
 */
@Suppress("MagicNumber")
class LowPassFilter(
    cutoffHz: Float,
    sampleRate: Int,
) {
    private val alpha: Float
    private var state = 0f

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(cutoffHz > 0f) { "cutoffHz must be positive" }
        require(cutoffHz < sampleRate / 2f) { "cutoffHz must be below Nyquist ($sampleRate/2)" }
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        val dt = 1f / sampleRate
        alpha = dt / (rc + dt)
    }

    fun process(input: Float): Float {
        state += alpha * (input - state)
        return state
    }

    fun reset() {
        state = 0f
    }
}

/**
 * Single-pole IIR high-pass filter.
 */
@Suppress("MagicNumber")
class HighPassFilter(
    cutoffHz: Float,
    sampleRate: Int,
) {
    private val alpha: Float
    private var prevInput = 0f
    private var state = 0f

    init {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(cutoffHz > 0f) { "cutoffHz must be positive" }
        require(cutoffHz < sampleRate / 2f) { "cutoffHz must be below Nyquist ($sampleRate/2)" }
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        val dt = 1f / sampleRate
        alpha = rc / (rc + dt)
    }

    fun process(input: Float): Float {
        state = alpha * (state + input - prevInput)
        prevInput = input
        return state
    }

    fun reset() {
        prevInput = 0f
        state = 0f
    }
}

/**
 * Biquad filter — supports low-pass, high-pass, band-pass, and notch modes.
 * Use for resonant filtering, duct resonances, housing bands, etc.
 *
 * Coefficients follow the Audio EQ Cookbook (Robert Bristow-Johnson).
 */
@Suppress("MagicNumber")
class BiquadFilter private constructor(
    private var b0: Float,
    private var b1: Float,
    private var b2: Float,
    private var a1: Float,
    private var a2: Float,
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun process(input: Float): Float {
        val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = output
        return output
    }

    fun reset() {
        x1 = 0f
        x2 = 0f
        y1 = 0f
        y2 = 0f
    }

    /** Reconfigure as band-pass without allocating a new object. */
    fun reconfigureBandPass(
        centerHz: Float,
        q: Float,
        sampleRate: Int,
    ) {
        require(sampleRate > 0) { "sampleRate must be positive" }
        require(centerHz > 0f) { "centerHz must be positive" }
        require(centerHz < sampleRate / 2f) { "centerHz must be below Nyquist ($sampleRate/2)" }
        require(q > 0f) { "q must be positive" }
        val w0 = 2f * PI.toFloat() * centerHz / sampleRate
        val sinW0 = sin(w0)
        val cosW0 = cos(w0)
        val alpha = sinW0 / (2f * q)
        val a0 = 1f + alpha
        b0 = (sinW0 / 2f) / a0
        b1 = 0f
        b2 = -(sinW0 / 2f) / a0
        a1 = (-2f * cosW0) / a0
        a2 = (1f - alpha) / a0
    }

    companion object {
        fun lowPass(
            cutoffHz: Float,
            q: Float,
            sampleRate: Int,
        ): BiquadFilter {
            val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
            val sinW0 = sin(w0)
            val cosW0 = cos(w0)
            val alpha = sinW0 / (2f * q)
            val a0 = 1f + alpha
            return BiquadFilter(
                b0 = ((1f - cosW0) / 2f) / a0,
                b1 = (1f - cosW0) / a0,
                b2 = ((1f - cosW0) / 2f) / a0,
                a1 = (-2f * cosW0) / a0,
                a2 = (1f - alpha) / a0,
            )
        }

        fun highPass(
            cutoffHz: Float,
            q: Float,
            sampleRate: Int,
        ): BiquadFilter {
            val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
            val sinW0 = sin(w0)
            val cosW0 = cos(w0)
            val alpha = sinW0 / (2f * q)
            val a0 = 1f + alpha
            return BiquadFilter(
                b0 = ((1f + cosW0) / 2f) / a0,
                b1 = -(1f + cosW0) / a0,
                b2 = ((1f + cosW0) / 2f) / a0,
                a1 = (-2f * cosW0) / a0,
                a2 = (1f - alpha) / a0,
            )
        }

        fun bandPass(
            centerHz: Float,
            q: Float,
            sampleRate: Int,
        ): BiquadFilter {
            val w0 = 2f * PI.toFloat() * centerHz / sampleRate
            val sinW0 = sin(w0)
            val cosW0 = cos(w0)
            val alpha = sinW0 / (2f * q)
            val a0 = 1f + alpha
            return BiquadFilter(
                b0 = (sinW0 / 2f) / a0,
                b1 = 0f,
                b2 = -(sinW0 / 2f) / a0,
                a1 = (-2f * cosW0) / a0,
                a2 = (1f - alpha) / a0,
            )
        }

        fun lowShelf(
            cutoffHz: Float,
            gainDb: Float,
            sampleRate: Int,
        ): BiquadFilter {
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
            val sinW0 = sin(w0)
            val cosW0 = cos(w0)
            val alpha = sinW0 / 2f * sqrt(2f)
            val sqrtA2Alpha = 2f * sqrt(a) * alpha
            val a0 = (a + 1f) + (a - 1f) * cosW0 + sqrtA2Alpha
            return BiquadFilter(
                b0 = (a * ((a + 1f) - (a - 1f) * cosW0 + sqrtA2Alpha)) / a0,
                b1 = (2f * a * ((a - 1f) - (a + 1f) * cosW0)) / a0,
                b2 = (a * ((a + 1f) - (a - 1f) * cosW0 - sqrtA2Alpha)) / a0,
                a1 = (-2f * ((a - 1f) + (a + 1f) * cosW0)) / a0,
                a2 = ((a + 1f) + (a - 1f) * cosW0 - sqrtA2Alpha) / a0,
            )
        }

        fun highShelf(
            cutoffHz: Float,
            gainDb: Float,
            sampleRate: Int,
        ): BiquadFilter {
            val a = 10.0.pow(gainDb / 40.0).toFloat()
            val w0 = 2f * PI.toFloat() * cutoffHz / sampleRate
            val sinW0 = sin(w0)
            val cosW0 = cos(w0)
            val alpha = sinW0 / 2f * sqrt(2f)
            val sqrtA2Alpha = 2f * sqrt(a) * alpha
            val a0 = (a + 1f) - (a - 1f) * cosW0 + sqrtA2Alpha
            return BiquadFilter(
                b0 = (a * ((a + 1f) + (a - 1f) * cosW0 + sqrtA2Alpha)) / a0,
                b1 = (-2f * a * ((a - 1f) + (a + 1f) * cosW0)) / a0,
                b2 = (a * ((a + 1f) + (a - 1f) * cosW0 - sqrtA2Alpha)) / a0,
                a1 = (2f * ((a - 1f) - (a + 1f) * cosW0)) / a0,
                a2 = ((a + 1f) - (a - 1f) * cosW0 - sqrtA2Alpha) / a0,
            )
        }
    }
}

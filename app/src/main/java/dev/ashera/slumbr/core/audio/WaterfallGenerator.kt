package dev.ashera.slumbr.core.audio

import dev.ashera.slumbr.core.audio.dsp.BiquadFilter
import dev.ashera.slumbr.core.audio.dsp.DriftModulator
import dev.ashera.slumbr.core.audio.dsp.HighPassFilter
import dev.ashera.slumbr.core.audio.dsp.LowPassFilter
import kotlin.math.min
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Procedural waterfall sound generator using three layered noise bands:
 *
 * 1. **Body** — pink noise (Voss-McCartney) provides the broadband wash
 * 2. **Rumble** — brown noise low-passed at ~200 Hz for the deep bass of falling water
 * 3. **Splash** — white noise band-passed ~800–5000 Hz for mist/spray texture
 *
 * Each band has independent slow drift modulation for organic movement.
 * A high-shelf cut tames upper mids to prevent listening fatigue.
 *
 * Target: distant/soft waterfall, NOT bright crashing falls.
 */
@Suppress("MagicNumber") // DSP constants are empirically tuned
class WaterfallGenerator : SoundGenerator {
    // --- Pink noise body (Voss-McCartney, 8 octaves) ---
    private val pinkOctaves = FloatArray(PINK_OCTAVE_COUNT)
    private var pinkCounter = 0
    private var pinkRunningSum = 0f
    private val bodyDrift = DriftModulator(BODY_DRIFT_HZ, AudioEngine.SAMPLE_RATE)

    // --- Brown noise rumble ---
    private var brownLast = 0f
    private val rumbleLp = LowPassFilter(RUMBLE_CUTOFF, AudioEngine.SAMPLE_RATE)
    private val rumbleHp = HighPassFilter(RUMBLE_HP_CUTOFF, AudioEngine.SAMPLE_RATE)
    private val rumbleDrift = DriftModulator(RUMBLE_DRIFT_HZ, AudioEngine.SAMPLE_RATE)

    // --- Splash band ---
    private val splashBp = BiquadFilter.bandPass(SPLASH_CENTER, SPLASH_Q, AudioEngine.SAMPLE_RATE)
    private val splashDrift = DriftModulator(SPLASH_DRIFT_HZ, AudioEngine.SAMPLE_RATE)
    private val splashFastDrift = DriftModulator(SPLASH_FAST_DRIFT_HZ, AudioEngine.SAMPLE_RATE)

    // --- Overall shaping ---
    private val highShelfCut =
        BiquadFilter.highShelf(
            cutoffHz = HIGH_SHELF_FREQ,
            gainDb = HIGH_SHELF_DB,
            sampleRate = AudioEngine.SAMPLE_RATE,
        )

    init {
        for (i in pinkOctaves.indices) {
            val value = Random.nextFloat() * 2f - 1f
            pinkOctaves[i] = value
            pinkRunningSum += value
        }
    }

    override fun fillBuffer(buffer: FloatArray) {
        for (i in buffer.indices) {
            // Layer 1: Pink noise body (broadband wash)
            val body = pink() * BODY_GAIN
            val bodyMod = DRIFT_MIN + bodyDrift.next() * DRIFT_RANGE

            // Layer 2: Brown noise rumble (low-frequency mass)
            val rumble = rumble() * RUMBLE_GAIN
            val rumbleMod = DRIFT_MIN + rumbleDrift.next() * DRIFT_RANGE

            // Layer 3: Band-passed splash (mist/spray) with two-speed modulation
            val splash = splashBp.process(Random.nextFloat() * 2f - 1f) * SPLASH_GAIN
            val splashSlow = DRIFT_MIN + splashDrift.next() * DRIFT_RANGE
            val splashFast = FAST_DRIFT_MIN + splashFastDrift.next() * FAST_DRIFT_RANGE
            val splashMod = splashSlow * splashFast

            // Mix layers
            var sample = body * bodyMod + rumble * rumbleMod + splash * splashMod

            // High-shelf cut to tame upper mids / prevent fatigue
            sample = highShelfCut.process(sample)

            buffer[i] = sample.coerceIn(-1f, 1f)
        }
    }

    /** Voss-McCartney pink noise, unnormalized in roughly [-1, 1]. */
    private fun pink(): Float {
        val k = Integer.numberOfTrailingZeros(pinkCounter)
        val octave = min(k, pinkOctaves.size - 1)
        pinkRunningSum -= pinkOctaves[octave]
        val newValue = Random.nextFloat() * 2f - 1f
        pinkOctaves[octave] = newValue
        pinkRunningSum += newValue
        pinkCounter++
        return pinkRunningSum / pinkOctaves.size
    }

    /** Brown noise filtered to low rumble range. */
    private fun rumble(): Float {
        brownLast += (Random.nextFloat() * 2f - 1f) * BROWN_STEP
        brownLast *= BROWN_LEAK
        val clipped = tanh(brownLast.toDouble() * BROWN_DRIVE).toFloat()
        return rumbleHp.process(rumbleLp.process(clipped))
    }

    companion object {
        private const val PINK_OCTAVE_COUNT = 8

        // Brown noise parameters
        private const val BROWN_STEP = 0.02f
        private const val BROWN_LEAK = 0.998f
        private const val BROWN_DRIVE = 1.5

        // Rumble: low-passed brown noise for deep bass
        private const val RUMBLE_CUTOFF = 200f
        private const val RUMBLE_HP_CUTOFF = 25f // subsonic safety
        private const val RUMBLE_GAIN = 0.6f
        private const val RUMBLE_DRIFT_HZ = 0.15f

        // Body: pink noise broadband wash
        private const val BODY_GAIN = 0.7f
        private const val BODY_DRIFT_HZ = 0.25f

        // Splash: band-passed white noise for mist texture
        private const val SPLASH_CENTER = 2500f
        private const val SPLASH_Q = 0.5f // wide band
        private const val SPLASH_GAIN = 0.25f
        private const val SPLASH_DRIFT_HZ = 0.5f
        private const val SPLASH_FAST_DRIFT_HZ = 1.5f // faster turbulence layer

        // High-shelf cut to prevent upper-mid fatigue
        private const val HIGH_SHELF_FREQ = 3000f
        private const val HIGH_SHELF_DB = -4f

        // Slow drift modulation range: [DRIFT_MIN, 1.0] → ~35% depth
        private const val DRIFT_MIN = 0.65f
        private const val DRIFT_RANGE = 1f - DRIFT_MIN

        // Fast drift modulation range (splash only): subtler
        private const val FAST_DRIFT_MIN = 0.75f
        private const val FAST_DRIFT_RANGE = 1f - FAST_DRIFT_MIN
    }
}

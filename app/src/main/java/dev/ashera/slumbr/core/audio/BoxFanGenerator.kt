package dev.ashera.slumbr.core.audio

import dev.ashera.slumbr.core.audio.dsp.BiquadFilter
import dev.ashera.slumbr.core.audio.dsp.DriftModulator
import dev.ashera.slumbr.core.audio.dsp.HighPassFilter
import dev.ashera.slumbr.core.audio.dsp.Lfo
import dev.ashera.slumbr.core.audio.dsp.LowPassFilter
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * Procedural box fan sound generator.
 *
 * Models the two real components of fan noise:
 * - Broadband noise from air turbulence (brown + pink noise blend)
 * - Weak tonal component at blade-pass frequency (~120 Hz for 5 blades at ~1440 RPM)
 *
 * DSP chain:
 * 1. Brown + pink noise blend — brown for warm fan rumble, pink for air turbulence
 * 2. Low shelf boost at ~200 Hz — motor/housing body warmth
 * 3. Band-limited via low-pass (~3 kHz) — fans lack high-frequency content
 * 4. Weak tonal hum at ~120 Hz — blade-pass frequency
 * 5. Resonant band-pass peaks at housing frequencies (~350 Hz, ~700 Hz)
 * 6. High-pass at ~40 Hz — remove sub-bass rumble
 * 7. Slow AM (LFO ~0.07 Hz) + random drift — subtle breathing feel
 */
@Suppress("MagicNumber") // DSP constants are empirically tuned
class BoxFanGenerator : SoundGenerator {
    // Pink noise state (Voss-McCartney, 8 octaves)
    private val pinkOctaves = FloatArray(PINK_OCTAVE_COUNT)
    private var pinkCounter = 0
    private var pinkRunningSum = 0f

    // Brown noise state
    private var brownLast = 0f

    // Motor hum oscillator phase
    private var humPhase = 0.0

    // Filters
    private val lowShelf = BiquadFilter.lowShelf(LOW_SHELF_FREQ, LOW_SHELF_GAIN_DB, AudioEngine.SAMPLE_RATE)
    private val lpFilter = LowPassFilter(LP_CUTOFF, AudioEngine.SAMPLE_RATE)
    private val hpFilter = HighPassFilter(HP_CUTOFF, AudioEngine.SAMPLE_RATE)
    private val housingBand1 =
        BiquadFilter.bandPass(HOUSING_FREQ_1, HOUSING_Q, AudioEngine.SAMPLE_RATE)
    private val housingBand2 =
        BiquadFilter.bandPass(HOUSING_FREQ_2, HOUSING_Q, AudioEngine.SAMPLE_RATE)

    // Modulation
    private val amLfo = Lfo(AM_LFO_RATE, AudioEngine.SAMPLE_RATE)
    private val drift = DriftModulator(DRIFT_RATE, AudioEngine.SAMPLE_RATE)

    init {
        for (i in pinkOctaves.indices) {
            val value = Random.nextFloat() * 2f - 1f
            pinkOctaves[i] = value
            pinkRunningSum += value
        }
    }

    override fun fillBuffer(buffer: FloatArray) {
        for (i in buffer.indices) {
            buffer[i] = nextSample()
        }
    }

    private fun nextSample(): Float {
        // 1. Blended noise base: brown for warm fan rumble, pink for air turbulence
        val pink = pinkNoise()
        val brown = brownNoise()
        val noise = pink * PINK_MIX + brown * BROWN_MIX

        // 2. Low shelf boost for motor/housing body warmth
        val warmed = lowShelf.process(noise)

        // 3. Band-limit — fans lack high-frequency content
        val shaped = lpFilter.process(warmed)

        // 4. Weak motor hum at blade-pass frequency (~120 Hz)
        val hum = sin(humPhase).toFloat() * HUM_LEVEL
        humPhase += HUM_PHASE_INC
        if (humPhase >= TWO_PI) humPhase -= TWO_PI

        // 5. Housing resonances — parallel band-pass, mixed in subtly
        val resonance =
            housingBand1.process(shaped) * HOUSING_MIX +
                housingBand2.process(shaped) * HOUSING_MIX

        // 6. Combine and high-pass to remove sub-bass
        val combined = shaped + hum + resonance
        val filtered = hpFilter.process(combined)

        // 7. Amplitude modulation: subtle breathing + drift
        val amDepth = AM_DEPTH_BASE + drift.next() * AM_DEPTH_DRIFT
        val am = 1f - amDepth + amDepth * amLfo.next()

        val output = filtered * am * OUTPUT_GAIN
        return output.coerceIn(-1f, 1f)
    }

    private fun pinkNoise(): Float {
        val k = Integer.numberOfTrailingZeros(pinkCounter)
        val octave = min(k, pinkOctaves.size - 1)
        pinkRunningSum -= pinkOctaves[octave]
        val newValue = Random.nextFloat() * 2f - 1f
        pinkOctaves[octave] = newValue
        pinkRunningSum += newValue
        pinkCounter++
        return pinkRunningSum / pinkOctaves.size
    }

    private fun brownNoise(): Float {
        brownLast += (Random.nextFloat() * 2f - 1f) * BROWN_STEP_SCALE
        brownLast *= BROWN_LEAK_FACTOR
        return tanh(brownLast.toDouble() * SOFT_CLIP_DRIVE).toFloat()
    }

    companion object {
        private const val PINK_OCTAVE_COUNT = 8

        // Noise blend — brown for warm fan rumble, pink for broadband air turbulence
        private const val PINK_MIX = 0.35f
        private const val BROWN_MIX = 0.45f
        private const val BROWN_STEP_SCALE = 0.02f
        private const val BROWN_LEAK_FACTOR = 0.998f
        private const val SOFT_CLIP_DRIVE = 1.5

        // Spectral shaping
        private const val LOW_SHELF_FREQ = 200f
        private const val LOW_SHELF_GAIN_DB = 4f
        private const val LP_CUTOFF = 3000f
        private const val HP_CUTOFF = 40f

        // Motor hum — blade-pass frequency (5 blades × ~1440 RPM / 60 ≈ 120 Hz)
        private const val HUM_FREQ = 120f
        private const val HUM_LEVEL = 0.03f
        private val HUM_PHASE_INC = 2.0 * PI * HUM_FREQ / AudioEngine.SAMPLE_RATE
        private val TWO_PI = 2.0 * PI

        // Housing resonances
        private const val HOUSING_FREQ_1 = 350f
        private const val HOUSING_FREQ_2 = 700f
        private const val HOUSING_Q = 3f
        private const val HOUSING_MIX = 0.08f

        // Amplitude modulation — breathing feel with noticeable movement
        private const val AM_LFO_RATE = 0.07f
        private const val AM_DEPTH_BASE = 0.22f
        private const val AM_DEPTH_DRIFT = 0.12f
        private const val DRIFT_RATE = 0.15f

        // Output level
        private const val OUTPUT_GAIN = 0.9f
    }
}

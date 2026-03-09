package dev.ashera.slumbr.core.audio.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

@Suppress("MagicNumber")
class ModulatorsTest {
    private val sampleRate = 22050

    @Test
    fun `LFO produces values in 0 to 1 range`() {
        val lfo = Lfo(1f, sampleRate)
        // 2 full cycles
        repeat(sampleRate * 2) {
            val value = lfo.next()
            assertTrue("LFO value $value out of range", value in 0f..1f)
        }
    }

    @Test
    fun `LFO oscillates with expected period`() {
        val lfo = Lfo(1f, sampleRate)
        val firstValue = lfo.next()
        // Advance exactly one period (sampleRate samples for 1 Hz)
        var min = firstValue
        var max = firstValue
        repeat(sampleRate - 1) {
            val v = lfo.next()
            if (v < min) min = v
            if (v > max) max = v
        }
        val afterOnePeriod = lfo.next()
        // After one full cycle, value should match the first sample
        assertTrue(
            "LFO should return to same value after one period (first=$firstValue, after=$afterOnePeriod)",
            abs(firstValue - afterOnePeriod) < 0.01f,
        )
        assertTrue("LFO should reach near 0 (min=$min)", min < 0.1f)
        assertTrue("LFO should reach near 1 (max=$max)", max > 0.9f)
    }

    @Test
    fun `DriftModulator produces values in 0 to 1 range`() {
        val drift = DriftModulator(0.5f, sampleRate)
        repeat(sampleRate * 5) {
            val value = drift.next()
            assertTrue("Drift value $value out of range", value in 0f..1f)
        }
    }

    @Test
    fun `DriftModulator varies over time`() {
        val drift = DriftModulator(1f, sampleRate)
        // Use 5 seconds of samples to give the random walk enough time to vary
        val sampleCount = sampleRate * 5
        val samples = FloatArray(sampleCount) { drift.next() }
        val min = samples.min()
        val max = samples.max()
        assertTrue(
            "Drift should have some variation (range=${max - min})",
            max - min > 0.01f,
        )
    }

    @Test
    fun `ArEnvelope produces correct shape`() {
        val env = ArEnvelope(attackSamples = 100, releaseSamples = 200)

        // Before trigger — inactive
        assertTrue("Should be inactive before trigger", !env.isActive)
        assertTrue("Should return 0 when inactive", env.next() == 0f)

        // Trigger
        env.trigger()
        assertTrue("Should be active after trigger", env.isActive)

        // First sample is 0/100 = 0 (start of attack)
        val first = env.next()
        assertTrue("First sample should be 0 (was $first)", first == 0f)

        // Attack phase — should ramp up (positions 1-99)
        var prev = first
        for (i in 1 until 100) {
            val v = env.next()
            assertTrue("Attack should ramp up (i=$i, prev=$prev, v=$v)", v >= prev)
            prev = v
        }

        // At position 100 (start of release): releasePos=0, value = 1 - 0/200 = 1.0
        val peak = env.next()
        assertTrue("Peak should be 1.0 (peak=$peak)", peak == 1f)

        // Release phase — should ramp down (positions 101-299)
        prev = peak
        for (i in 1 until 200) {
            val v = env.next()
            assertTrue("Release should ramp down (i=$i, prev=$prev, v=$v)", v <= prev)
            prev = v
        }

        // After envelope completes (300 samples consumed: positions 0-299)
        assertTrue("Should be inactive after completion", !env.isActive)
        assertTrue("Should return 0 after completion", env.next() == 0f)
    }

    @Test
    fun `ArEnvelope can be retriggered`() {
        val env = ArEnvelope(attackSamples = 10, releaseSamples = 10)
        env.trigger()
        repeat(20) { env.next() } // complete first envelope (10 attack + 10 release)
        assertTrue("Should be inactive", !env.isActive)

        env.trigger()
        assertTrue("Should be active after retrigger", env.isActive)
        env.next() // position 0 = 0 (start of attack ramp)
        val v = env.next() // position 1 = 1/10 = 0.1
        assertTrue("Should produce non-zero on second sample after retrigger (v=$v)", v > 0f)
    }

    @Test
    fun `ArEnvelope with zero attack starts at peak`() {
        val env = ArEnvelope(attackSamples = 0, releaseSamples = 10)
        env.trigger()
        // First sample should be at peak (1.0)
        val first = env.next()
        assertTrue("Zero-attack should start at peak (first=$first)", first == 1f)
        // Should ramp down
        val second = env.next()
        assertTrue("Should ramp down after peak (second=$second)", second < first)
        // Exhaust remaining samples
        repeat(8) { env.next() }
        assertTrue("Should be inactive after completion", !env.isActive)
    }

    @Test
    fun `ArEnvelope with zero release drops immediately`() {
        val env = ArEnvelope(attackSamples = 10, releaseSamples = 0)
        env.trigger()
        // Ramp up through attack
        var prev = 0f
        repeat(10) {
            val v = env.next()
            assertTrue("Attack should ramp up (v=$v, prev=$prev)", v >= prev)
            prev = v
        }
        // Should be inactive immediately after attack completes
        assertTrue("Should be inactive after attack-only envelope", !env.isActive)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ArEnvelope rejects negative attackSamples`() {
        ArEnvelope(attackSamples = -1, releaseSamples = 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ArEnvelope rejects negative releaseSamples`() {
        ArEnvelope(attackSamples = 10, releaseSamples = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ArEnvelope rejects zero total length`() {
        ArEnvelope(attackSamples = 0, releaseSamples = 0)
    }
}

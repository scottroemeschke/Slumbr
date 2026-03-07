package dev.ashera.slumbr.audio.dsp

import org.junit.Assert.assertTrue
import org.junit.Test

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
        // Sample one full second — should complete one cycle
        var min = 1f
        var max = 0f
        repeat(sampleRate) {
            val v = lfo.next()
            if (v < min) min = v
            if (v > max) max = v
        }
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
        val samples = FloatArray(sampleRate) { drift.next() }
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

        // Attack phase — should ramp up
        var prev = first
        for (i in 1 until 100) {
            val v = env.next()
            assertTrue("Attack should ramp up (i=$i, prev=$prev, v=$v)", v >= prev)
            prev = v
        }

        // Peak — sample at position 100 = 100/100 = 1.0
        val peak = env.next()
        assertTrue("Peak should be 1.0 (peak=$peak)", peak == 1f)

        // Release phase — should ramp down
        prev = peak
        for (i in 1..200) {
            val v = env.next()
            assertTrue("Release should ramp down (i=$i, prev=$prev, v=$v)", v <= prev)
            prev = v
        }

        // After envelope completes
        assertTrue("Should be inactive after completion", !env.isActive)
        assertTrue("Should return 0 after completion", env.next() == 0f)
    }

    @Test
    fun `ArEnvelope can be retriggered`() {
        val env = ArEnvelope(attackSamples = 10, releaseSamples = 10)
        env.trigger()
        repeat(21) { env.next() } // complete first envelope (10 attack + 10 release + 1)
        assertTrue("Should be inactive", !env.isActive)

        env.trigger()
        assertTrue("Should be active after retrigger", env.isActive)
        env.next() // position 0 = 0 (start of attack ramp)
        val v = env.next() // position 1 = 1/10 = 0.1
        assertTrue("Should produce non-zero on second sample after retrigger (v=$v)", v > 0f)
    }
}

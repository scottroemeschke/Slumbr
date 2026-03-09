package dev.ashera.slumbr.core.audio.dsp

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@Suppress("MagicNumber")
class FiltersTest {
    private val sampleRate = 22050

    @Test
    fun `low-pass filter attenuates high frequencies`() {
        val filter = LowPassFilter(100f, sampleRate)
        val lowFreqPower = measureSinePower(filter, 50f)

        val filter2 = LowPassFilter(100f, sampleRate)
        val highFreqPower = measureSinePower(filter2, 5000f)

        assertTrue(
            "Low-pass should attenuate high freq (low=$lowFreqPower, high=$highFreqPower)",
            highFreqPower < lowFreqPower * 0.1f,
        )
    }

    @Test
    fun `high-pass filter attenuates low frequencies`() {
        val filter = HighPassFilter(1000f, sampleRate)
        val lowFreqPower = measureSinePower(filter, 50f)

        val filter2 = HighPassFilter(1000f, sampleRate)
        val highFreqPower = measureSinePower(filter2, 5000f)

        assertTrue(
            "High-pass should attenuate low freq (low=$lowFreqPower, high=$highFreqPower)",
            lowFreqPower < highFreqPower * 0.1f,
        )
    }

    @Test
    fun `biquad band-pass passes center frequency`() {
        val center = 1000f
        val bpFilter = BiquadFilter.bandPass(center, 5f, sampleRate)

        val centerPower = measureSinePower(bpFilter, center)
        bpFilter.reset()
        val offCenterPower = measureSinePower(bpFilter, 100f)

        assertTrue(
            "Band-pass should favor center freq (center=$centerPower, off=$offCenterPower)",
            centerPower > offCenterPower * 2f,
        )
    }

    @Test
    fun `biquad reconfigure changes filter behavior`() {
        val bpFilter = BiquadFilter.bandPass(500f, 5f, sampleRate)

        val power500 = measureSinePower(bpFilter, 500f)
        bpFilter.reset()
        bpFilter.reconfigureBandPass(3000f, 5f, sampleRate)
        val power500After = measureSinePower(bpFilter, 500f)

        assertTrue(
            "After reconfigure to 3kHz, 500Hz should be attenuated",
            power500After < power500 * 0.5f,
        )
    }

    @Test
    fun `biquad low-pass filter produces finite output`() {
        val filter = BiquadFilter.lowPass(1000f, 0.707f, sampleRate)
        repeat(10000) {
            val input = sin(2.0 * PI * 440.0 * it / sampleRate).toFloat()
            val output = filter.process(input)
            assertTrue("Output should be finite: $output", output.isFinite())
        }
    }

    @Test
    fun `low shelf boosts low frequencies`() {
        val filter = BiquadFilter.lowShelf(200f, 12f, sampleRate)
        val lowPower = measureSinePower(filter, 50f)

        val filter2 = BiquadFilter.lowShelf(200f, 12f, sampleRate)
        val highPower = measureSinePower(filter2, 5000f)

        assertTrue(
            "Low shelf +12dB should boost lows relative to highs (low=$lowPower, high=$highPower)",
            lowPower > highPower,
        )
    }

    @Test
    fun `high shelf boosts high frequencies`() {
        val filter = BiquadFilter.highShelf(2000f, 12f, sampleRate)
        val highPower = measureSinePower(filter, 8000f)

        val filter2 = BiquadFilter.highShelf(2000f, 12f, sampleRate)
        val lowPower = measureSinePower(filter2, 100f)

        assertTrue(
            "High shelf +12dB should boost highs relative to lows (high=$highPower, low=$lowPower)",
            highPower > lowPower,
        )
    }

    /** Measure RMS power of a sine wave through a filter. */
    private fun measureSinePower(
        filter: Any,
        freqHz: Float,
    ): Double {
        val numSamples = sampleRate // 1 second
        var sumSquared = 0.0
        // Skip first 1000 samples to let filter settle
        val skipSamples = 1000
        for (i in 0 until numSamples + skipSamples) {
            val input = sin(2.0 * PI * freqHz * i / sampleRate).toFloat()
            val output =
                when (filter) {
                    is LowPassFilter -> filter.process(input)
                    is HighPassFilter -> filter.process(input)
                    is BiquadFilter -> filter.process(input)
                    else -> error("Unknown filter type")
                }
            if (i >= skipSamples) {
                sumSquared += output * output
            }
        }
        return sumSquared / numSamples
    }
}

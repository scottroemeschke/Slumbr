package dev.ashera.slumbr.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class WaterfallGeneratorTest {
    @Test
    fun `waterfall produces samples in valid range`() {
        val generator = WaterfallGenerator()
        val buffer = FloatArray(22050) // 1 second
        generator.fillBuffer(buffer)
        for (sample in buffer) {
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `waterfall has non-zero variance`() {
        val generator = WaterfallGenerator()
        val buffer = FloatArray(22050)
        generator.fillBuffer(buffer)
        val mean = buffer.average()
        val variance = buffer.map { (it - mean) * (it - mean) }.average()
        assertTrue("Waterfall variance too low: $variance", variance > 0.001)
    }

    @Test
    fun `waterfall via NoiseType createGenerator produces valid samples`() {
        val generator = NoiseType.WATERFALL.createGenerator()
        val buffer = FloatArray(1000)
        generator.fillBuffer(buffer)
        for (sample in buffer) {
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `waterfall is smoother than raw white noise`() {
        // White noise through NoiseGenerator includes LP filter + perceptual gain,
        // so compare against raw white noise to verify waterfall has spectral shaping
        val waterfall = WaterfallGenerator()
        val waterfallBuffer = FloatArray(10000)
        waterfall.fillBuffer(waterfallBuffer)

        // Generate raw unfiltered white noise for comparison
        val rawWhite = FloatArray(10000) { (Math.random() * 2.0 - 1.0).toFloat() }

        val waterfallDelta = avgDelta(waterfallBuffer)
        val whiteDelta = avgDelta(rawWhite)
        assertTrue(
            "Waterfall should be smoother than raw white noise " +
                "(waterfall=$waterfallDelta, white=$whiteDelta)",
            waterfallDelta < whiteDelta,
        )
    }

    @Test
    fun `multiple seconds of waterfall stay in valid range`() {
        val generator = WaterfallGenerator()
        val buffer = FloatArray(22050) // 1 second per fill
        repeat(5) {
            generator.fillBuffer(buffer)
            for (sample in buffer) {
                assertTrue("Sample $sample out of range at second $it", sample in -1f..1f)
            }
        }
    }

    private fun avgDelta(samples: FloatArray): Double {
        var total = 0.0
        for (i in 1 until samples.size) {
            total += kotlin.math.abs(samples[i] - samples[i - 1])
        }
        return total / (samples.size - 1)
    }
}

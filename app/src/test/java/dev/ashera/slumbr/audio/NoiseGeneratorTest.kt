package dev.ashera.slumbr.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseGeneratorTest {
    @Test
    fun `white noise produces samples in valid range`() {
        val generator = NoiseGenerator(NoiseType.WHITE)
        repeat(10000) {
            val sample = generator.nextSample()
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `pink noise produces samples in valid range`() {
        val generator = NoiseGenerator(NoiseType.PINK)
        repeat(10000) {
            val sample = generator.nextSample()
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `brown noise produces samples in valid range`() {
        val generator = NoiseGenerator(NoiseType.BROWN)
        repeat(10000) {
            val sample = generator.nextSample()
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `white noise has non-zero variance`() {
        val generator = NoiseGenerator(NoiseType.WHITE)
        val samples = FloatArray(1000) { generator.nextSample() }
        val mean = samples.average()
        val variance = samples.map { (it - mean) * (it - mean) }.average()
        assertTrue("White noise variance too low: $variance", variance > 0.01)
    }

    @Test
    fun `perceptual gain normalizes RMS levels across noise types`() {
        val sampleCount = 50000

        fun rms(type: NoiseType): Double {
            val gen = NoiseGenerator(type)
            val samples = FloatArray(sampleCount) { gen.nextSample() }
            return kotlin.math.sqrt(samples.map { (it * it).toDouble() }.average())
        }

        val whiteRms = rms(NoiseType.WHITE)
        val pinkRms = rms(NoiseType.PINK)
        val brownRms = rms(NoiseType.BROWN)
        val maxRms = maxOf(whiteRms, pinkRms, brownRms)
        val minRms = minOf(whiteRms, pinkRms, brownRms)
        // All noise types should be within 2x RMS of each other after gain correction
        assertTrue(
            "RMS spread too wide (white=$whiteRms, pink=$pinkRms, brown=$brownRms)",
            maxRms / minRms < 2.0,
        )
    }

    @Test
    fun `brown noise is smoother than white noise`() {
        val white = NoiseGenerator(NoiseType.WHITE)
        val brown = NoiseGenerator(NoiseType.BROWN)

        fun avgDelta(gen: NoiseGenerator): Double {
            var prev = gen.nextSample()
            var totalDelta = 0.0
            repeat(10000) {
                val cur = gen.nextSample()
                totalDelta += kotlin.math.abs(cur - prev)
                prev = cur
            }
            return totalDelta / 10000
        }

        val whiteDelta = avgDelta(white)
        val brownDelta = avgDelta(brown)
        assertTrue(
            "Brown noise should be smoother (whiteDelta=$whiteDelta, brownDelta=$brownDelta)",
            brownDelta < whiteDelta,
        )
    }
}

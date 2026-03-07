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
    fun `brown noise is smoother than white noise`() {
        val white = NoiseGenerator(NoiseType.WHITE)
        val brown = NoiseGenerator(NoiseType.BROWN)

        val whiteDelta = avgSampleDelta(white)
        val brownDelta = avgSampleDelta(brown)
        assertTrue(
            "Brown noise should be smoother (whiteDelta=$whiteDelta, brownDelta=$brownDelta)",
            brownDelta < whiteDelta,
        )
    }

    @Test
    fun `pink noise is smoother than white noise`() {
        val white = NoiseGenerator(NoiseType.WHITE)
        val pink = NoiseGenerator(NoiseType.PINK)

        val whiteDelta = avgSampleDelta(white)
        val pinkDelta = avgSampleDelta(pink)
        assertTrue(
            "Pink noise should be smoother than white (white=$whiteDelta, pink=$pinkDelta)",
            pinkDelta < whiteDelta,
        )
    }

    @Test
    fun `fillBuffer produces same range as nextSample`() {
        for (type in NoiseType.entries) {
            val generator = NoiseGenerator(type)
            val buffer = FloatArray(1000)
            generator.fillBuffer(buffer)
            for (sample in buffer) {
                assertTrue("$type: sample $sample out of range", sample in -1f..1f)
            }
        }
    }

    private fun avgSampleDelta(gen: NoiseGenerator): Double {
        var prev = gen.nextSample()
        var totalDelta = 0.0
        repeat(10000) {
            val cur = gen.nextSample()
            totalDelta += kotlin.math.abs(cur - prev)
            prev = cur
        }
        return totalDelta / 10000
    }
}

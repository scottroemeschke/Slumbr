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

        val whiteDelta = avgSampleDelta(white, NoiseType.WHITE.perceptualGain)
        val brownDelta = avgSampleDelta(brown, NoiseType.BROWN.perceptualGain)
        assertTrue(
            "Brown noise should be smoother (whiteDelta=$whiteDelta, brownDelta=$brownDelta)",
            brownDelta < whiteDelta,
        )
    }

    @Test
    fun `pink noise is smoother than white noise`() {
        val white = NoiseGenerator(NoiseType.WHITE)
        val pink = NoiseGenerator(NoiseType.PINK)

        val whiteDelta = avgSampleDelta(white, NoiseType.WHITE.perceptualGain)
        val pinkDelta = avgSampleDelta(pink, NoiseType.PINK.perceptualGain)
        assertTrue(
            "Pink noise should be smoother than white (white=$whiteDelta, pink=$pinkDelta)",
            pinkDelta < whiteDelta,
        )
    }

    @Test
    fun `fillBuffer produces same range as nextSample`() {
        val sentinel = 2f
        for (type in NoiseType.entries) {
            val generator = NoiseGenerator(type)
            val buffer = FloatArray(1000) { sentinel }
            generator.fillBuffer(buffer)
            for (sample in buffer) {
                assertTrue("$type: sample still sentinel", sample != sentinel)
                assertTrue("$type: sample $sample out of range", sample in -1f..1f)
            }
        }
    }

    @Test
    fun `createGenerator returns SoundGenerator that produces valid samples`() {
        for (type in NoiseType.entries) {
            val generator = type.createGenerator()
            val buffer = FloatArray(1000)
            generator.fillBuffer(buffer)
            for (sample in buffer) {
                assertTrue("$type: sample $sample out of range", sample in -1f..1f)
            }
        }
    }

    /** Compute average sample-to-sample delta, normalizing out perceptualGain. */
    private fun avgSampleDelta(
        gen: NoiseGenerator,
        gain: Float,
    ): Double {
        var prev = gen.nextSample() / gain
        var totalDelta = 0.0
        repeat(10000) {
            val cur = gen.nextSample() / gain
            totalDelta += kotlin.math.abs(cur - prev)
            prev = cur
        }
        return totalDelta / 10000
    }
}

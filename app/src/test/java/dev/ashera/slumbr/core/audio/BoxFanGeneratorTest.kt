package dev.ashera.slumbr.core.audio

import org.junit.Assert.assertTrue
import org.junit.Test

class BoxFanGeneratorTest {
    @Test
    fun `box fan produces samples in valid range`() {
        val generator = BoxFanGenerator()
        val buffer = FloatArray(22050) // 1 second
        generator.fillBuffer(buffer)
        for (sample in buffer) {
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    @Test
    fun `box fan has non-zero variance`() {
        val generator = BoxFanGenerator()
        val buffer = FloatArray(22050)
        generator.fillBuffer(buffer)
        val mean = buffer.average()
        val variance = buffer.map { (it - mean) * (it - mean) }.average()
        assertTrue("Box fan variance too low: $variance", variance > 0.001)
    }

    @Test
    fun `box fan is smoother than white noise`() {
        val fan = BoxFanGenerator()
        val white = NoiseGenerator(NoiseType.WHITE)

        val fanBuffer = FloatArray(10000)
        val whiteBuffer = FloatArray(10000)
        fan.fillBuffer(fanBuffer)
        white.fillBuffer(whiteBuffer)

        val fanDelta = avgDelta(fanBuffer)
        val whiteDelta = avgDelta(whiteBuffer)
        assertTrue(
            "Box fan should be smoother than white (fan=$fanDelta, white=$whiteDelta)",
            fanDelta < whiteDelta,
        )
    }

    @Test
    fun `createGenerator returns BoxFanGenerator via NoiseType`() {
        val generator = NoiseType.BOX_FAN.createGenerator()
        assertTrue(generator is BoxFanGenerator)
        val buffer = FloatArray(1000)
        generator.fillBuffer(buffer)
        for (sample in buffer) {
            assertTrue("Sample $sample out of range", sample in -1f..1f)
        }
    }

    private fun avgDelta(buffer: FloatArray): Double {
        var total = 0.0
        for (i in 1 until buffer.size) {
            total += kotlin.math.abs(buffer[i] - buffer[i - 1])
        }
        return total / (buffer.size - 1)
    }
}

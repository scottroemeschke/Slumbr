package dev.ashera.slumbr.core.audio

/**
 * Interface for all procedural sound generators.
 * Implementations produce mono float samples in [-1, 1].
 */
interface SoundGenerator {
    /** Fill [buffer] with generated samples. */
    fun fillBuffer(buffer: FloatArray)
}

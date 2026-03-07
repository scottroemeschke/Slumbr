package dev.ashera.slumbr.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Low-level audio engine that generates and plays noise via AudioTrack.
 * Runs on a background coroutine. Supports fade in/out for smooth transitions.
 */
class AudioEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val FADE_DURATION_MS = 2000L
        private const val FADE_SAMPLES = (SAMPLE_RATE * FADE_DURATION_MS / 1000).toInt()
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var fadingOut = false

    val isPlaying: Boolean
        get() = playbackJob?.isActive == true && !fadingOut

    fun start(
        noiseType: NoiseType,
        volume: Float = 0.8f,
    ) {
        stop()

        val bufferSize =
            AudioTrack
                .getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                ).coerceAtLeast(SAMPLE_RATE) // At least 1 second buffer

        val track =
            AudioTrack
                .Builder()
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                ).setAudioFormat(
                    AudioFormat
                        .Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setBufferSizeInBytes(bufferSize * Float.SIZE_BYTES)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

        track.setVolume(volume)
        track.play()
        audioTrack = track
        fadingOut = false

        val generator = NoiseGenerator(noiseType)
        val chunkSize = SAMPLE_RATE / 10 // 100ms chunks
        val buffer = FloatArray(chunkSize)

        playbackJob =
            scope.launch {
                var totalSamples = 0L

                while (isActive && !fadingOut) {
                    for (i in buffer.indices) {
                        val sample = generator.nextSample()
                        // Fade in: linear ramp over FADE_SAMPLES
                        val fadeIn =
                            if (totalSamples < FADE_SAMPLES) {
                                totalSamples.toFloat() / FADE_SAMPLES
                            } else {
                                1f
                            }
                        buffer[i] = sample * fadeIn
                        totalSamples++
                    }
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }

                // Fade out
                if (fadingOut) {
                    var fadeRemaining = FADE_SAMPLES
                    while (fadeRemaining > 0 && isActive) {
                        val samplesToWrite = min(chunkSize, fadeRemaining)
                        for (i in 0 until samplesToWrite) {
                            val sample = generator.nextSample()
                            val fadeOut = fadeRemaining.toFloat() / FADE_SAMPLES
                            buffer[i] = sample * fadeOut
                            fadeRemaining--
                        }
                        track.write(buffer, 0, samplesToWrite, AudioTrack.WRITE_BLOCKING)
                    }
                }

                track.stop()
                track.release()
            }
    }

    fun stop() {
        if (playbackJob?.isActive == true) {
            fadingOut = true
            playbackJob?.invokeOnCompletion {
                audioTrack = null
                playbackJob = null
            }
        } else {
            audioTrack?.let {
                it.stop()
                it.release()
            }
            audioTrack = null
            playbackJob = null
        }
    }

    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }

    fun release() {
        fadingOut = false
        playbackJob?.cancel()
        audioTrack?.let {
            it.stop()
            it.release()
        }
        audioTrack = null
        playbackJob = null
    }
}

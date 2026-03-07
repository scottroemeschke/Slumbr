package dev.ashera.slumbr.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * Low-level audio engine that generates and plays noise via AudioTrack.
 * Runs on a background coroutine. Supports fade in/out for smooth transitions
 * and seamless noise-type switching.
 */
class AudioEngine {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val FADE_IN_MS = 2000L
        private const val FADE_OUT_MS = 16000L
        private val FADE_IN_SAMPLES = (SAMPLE_RATE * FADE_IN_MS / 1000).toInt()
        private val FADE_OUT_SAMPLES = (SAMPLE_RATE * FADE_OUT_MS / 1000).toInt()
        private val GAIN_UP_STEP = 1f / FADE_IN_SAMPLES
        private val GAIN_DOWN_STEP = 1f / FADE_OUT_SAMPLES
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var targetGain = 0f

    @Volatile
    private var generator: NoiseGenerator? = null

    private val _fadeProgress = MutableStateFlow(0f)
    val fadeProgress: StateFlow<Float> = _fadeProgress.asStateFlow()

    var onPlaybackComplete: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = playbackJob?.isActive == true

    /**
     * Swap the noise generator in-place without any fade or restart.
     * The new noise type plays at the current volume level seamlessly.
     */
    fun switchNoise(noiseType: NoiseType) {
        generator = NoiseGenerator(noiseType)
    }

    fun start(
        noiseType: NoiseType,
        volume: Float = 0.8f,
    ) {
        playbackJob?.cancel()
        audioTrack?.stop()

        val minBufferBytes =
            AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
            )
        // At least 1 second of PCM float for stable playback without underruns
        val oneSecondBytes = SAMPLE_RATE * Float.SIZE_BYTES
        val bufferSizeBytes = maxOf(minBufferBytes, oneSecondBytes)

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
                ).setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

        track.setVolume(volume)
        track.play()
        audioTrack = track

        generator = NoiseGenerator(noiseType)
        targetGain = 1f
        _fadeProgress.value = 0f

        val chunkSize = SAMPLE_RATE / 10 // 100ms chunks
        val buffer = FloatArray(chunkSize)

        playbackJob =
            scope.launch {
                var currentGain = 0f

                var gen = generator
                while (isActive && gen != null) {
                    for (i in buffer.indices) {
                        currentGain =
                            when {
                                currentGain < targetGain ->
                                    min(currentGain + GAIN_UP_STEP, targetGain)
                                currentGain > targetGain ->
                                    max(currentGain - GAIN_DOWN_STEP, targetGain)
                                else -> currentGain
                            }
                        buffer[i] = gen.nextSample() * smoothstep(currentGain)
                    }

                    _fadeProgress.value = currentGain
                    track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)

                    if (currentGain == 0f && targetGain == 0f) break
                    gen = generator
                }

                track.stop()
                track.release()

                if (isActive) {
                    // Natural completion (fade-out finished, not cancelled)
                    if (audioTrack === track) audioTrack = null
                    _fadeProgress.value = 0f
                    onPlaybackComplete?.invoke()
                }
            }
    }

    /**
     * Graceful stop — sets target gain to zero and lets the coroutine
     * fade out over [FADE_OUT_MS] milliseconds.
     */
    fun stop() {
        targetGain = 0f
    }

    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume.coerceIn(0f, 1f))
    }

    /** S-curve (3x²-2x³): eases in and out for natural-sounding fades. */
    @Suppress("MagicNumber")
    private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)

    fun release() {
        targetGain = 0f
        playbackJob?.cancel()
        audioTrack?.let {
            it.stop()
            it.release()
        }
        audioTrack = null
        playbackJob = null
        _fadeProgress.value = 0f
    }
}

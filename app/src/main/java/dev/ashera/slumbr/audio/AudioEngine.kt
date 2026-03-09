package dev.ashera.slumbr.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Low-level audio engine that generates and plays noise via AudioTrack.
 * Runs on a background coroutine. Supports fade in/out for smooth transitions
 * and seamless noise-type switching.
 */
@Singleton
class AudioEngine
    @Inject
    constructor() : AudioEngineContract {
        companion object {
            const val SAMPLE_RATE = 22050
            private const val FADE_IN_MS = 2000L
            private const val FADE_OUT_MS = 16000L
            private val FADE_IN_SAMPLES = (SAMPLE_RATE * FADE_IN_MS / 1000).toInt()
            private val FADE_OUT_SAMPLES = (SAMPLE_RATE * FADE_OUT_MS / 1000).toInt()
            private val GAIN_UP_STEP = 1f / FADE_IN_SAMPLES
            private val GAIN_DOWN_STEP = 1f / FADE_OUT_SAMPLES
        }

        private var audioTrack: AudioTrack? = null
        private var playbackJob: Job? = null
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Volatile
        private var targetGain = 0f

        @Volatile
        private var generator: SoundGenerator? = null

        private val _fadeProgress = MutableStateFlow(0f)
        override val fadeProgress: StateFlow<Float> = _fadeProgress.asStateFlow()

        override var onPlaybackComplete: (() -> Unit)? = null

        override val isPlaying: Boolean
            get() = playbackJob?.isActive == true

        /**
         * Swap the noise generator in-place without any fade or restart.
         * The new noise type plays at the current volume level seamlessly.
         */
        override fun switchNoise(noiseType: NoiseType) {
            generator = noiseType.createGenerator()
        }

        override fun start(
            noiseType: NoiseType,
            volume: Float,
        ) {
            playbackJob?.cancel()
            audioTrack?.stop()

            val track = buildAudioTrack()
            track.setVolume(volume)
            track.play()
            audioTrack = track

            generator = noiseType.createGenerator()
            targetGain = 1f
            _fadeProgress.value = 0f

            val chunkSize = SAMPLE_RATE // ~1 second chunks
            val floatBuffer = FloatArray(chunkSize)
            val pcmBuffer = ShortArray(chunkSize)

            playbackJob =
                scope.launch {
                    var currentGain = 0f

                    var gen = generator
                    while (isActive && gen != null) {
                        gen.fillBuffer(floatBuffer)

                        for (i in floatBuffer.indices) {
                            currentGain =
                                when {
                                    currentGain < targetGain ->
                                        min(currentGain + GAIN_UP_STEP, targetGain)
                                    currentGain > targetGain ->
                                        max(currentGain - GAIN_DOWN_STEP, targetGain)
                                    else -> currentGain
                                }
                            val shapedGain =
                                if (targetGain == 0f) {
                                    // Cubic: fast initial drop, gentle tail toward silence
                                    currentGain * currentGain * currentGain
                                } else {
                                    // Smoothstep S-curve for fade-in
                                    smoothstep(currentGain)
                                }
                            pcmBuffer[i] =
                                (floatBuffer[i] * shapedGain * Short.MAX_VALUE)
                                    .toInt()
                                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                    .toShort()
                        }

                        _fadeProgress.value = currentGain
                        track.write(pcmBuffer, 0, pcmBuffer.size, AudioTrack.WRITE_BLOCKING)

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

        private fun buildAudioTrack(): AudioTrack {
            val minBufferBytes =
                AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
            // ~3 seconds of 16-bit PCM for stable playback with large chunks
            val threeSecondBytes = SAMPLE_RATE * Short.SIZE_BYTES * 3
            val bufferSizeBytes = maxOf(minBufferBytes, threeSecondBytes)

            return AudioTrack
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
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                ).setBufferSizeInBytes(bufferSizeBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }

        /**
         * Graceful stop — sets target gain to zero and lets the coroutine
         * fade out over [FADE_OUT_MS] milliseconds.
         */
        override fun stop() {
            targetGain = 0f
        }

        override fun setVolume(volume: Float) {
            audioTrack?.setVolume(volume.coerceIn(0f, 1f))
        }

        /** S-curve (3x^2-2x^3): eases in and out for natural-sounding fades. */
        @Suppress("MagicNumber")
        private fun smoothstep(x: Float): Float = x * x * (3f - 2f * x)

        override fun release() {
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

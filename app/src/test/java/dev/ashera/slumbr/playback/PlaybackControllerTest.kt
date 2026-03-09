package dev.ashera.slumbr.playback

import dev.ashera.slumbr.audio.NoiseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlaybackControllerTest {
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var controller: PlaybackController

    @Before
    fun setUp() {
        fakeEngine = FakeAudioEngine()
        controller = PlaybackController(fakeEngine)
    }

    @Test
    fun `start sets playing state with correct noise type`() {
        controller.start(NoiseType.BROWN, 0.8f)

        val state = controller.playbackState.value
        assertTrue(state.isPlaying)
        assertEquals(NoiseType.BROWN, state.currentNoise)
        assertEquals(NoiseType.BROWN, fakeEngine.startedNoiseType)
        assertEquals(0.8f, fakeEngine.startedVolume)
    }

    @Test
    fun `switchNoise updates current noise without restart`() {
        controller.start(NoiseType.BROWN, 0.8f)
        controller.switchNoise(NoiseType.PINK)

        val state = controller.playbackState.value
        assertTrue(state.isPlaying)
        assertEquals(NoiseType.PINK, state.currentNoise)
        assertEquals(NoiseType.PINK, fakeEngine.switchedNoiseType)
        // Engine was NOT restarted with PINK — only switchNoise called
        assertEquals(NoiseType.BROWN, fakeEngine.startedNoiseType)
    }

    @Test
    fun `gracefulStop calls engine stop`() {
        controller.start(NoiseType.WHITE, 0.8f)
        controller.gracefulStop()

        assertTrue(fakeEngine.stopCalled)
        // State remains playing until fade-out completes
        assertTrue(controller.playbackState.value.isPlaying)
    }

    @Test
    fun `gracefulStop transitions to stopped after fade-out completes`() {
        controller.start(NoiseType.WHITE, 0.8f)
        controller.gracefulStop()
        fakeEngine.simulateFadeOutComplete()

        val state = controller.playbackState.value
        assertFalse(state.isPlaying)
        assertNull(state.currentNoise)
    }

    @Test
    fun `hardStop immediately stops playback`() {
        controller.start(NoiseType.BROWN, 0.8f)
        controller.hardStop()

        val state = controller.playbackState.value
        assertFalse(state.isPlaying)
        assertNull(state.currentNoise)
        assertTrue(fakeEngine.releaseCalled)
    }

    @Test
    fun `setVolume delegates to engine`() {
        controller.start(NoiseType.BROWN, 0.8f)
        controller.setVolume(0.5f)

        assertEquals(0.5f, fakeEngine.lastVolume)
    }

    @Test
    fun `initial state is not playing`() {
        val state = controller.playbackState.value
        assertFalse(state.isPlaying)
        assertNull(state.currentNoise)
        assertEquals(0f, state.fadeProgress)
    }

    @Test
    fun `handleCommand Start delegates to start`() {
        controller.handleCommand(PlaybackCommand.Start(NoiseType.PINK, 0.7f))

        val state = controller.playbackState.value
        assertTrue(state.isPlaying)
        assertEquals(NoiseType.PINK, state.currentNoise)
        assertEquals(NoiseType.PINK, fakeEngine.startedNoiseType)
        assertEquals(0.7f, fakeEngine.startedVolume)
    }

    @Test
    fun `handleCommand GracefulStop delegates to gracefulStop`() {
        controller.start(NoiseType.WHITE, 0.8f)
        controller.handleCommand(PlaybackCommand.GracefulStop)

        assertTrue(fakeEngine.stopCalled)
        assertTrue(controller.playbackState.value.isPlaying)
    }

    @Test
    fun `handleCommand HardStop delegates to hardStop`() {
        controller.start(NoiseType.BROWN, 0.8f)
        controller.handleCommand(PlaybackCommand.HardStop)

        assertFalse(controller.playbackState.value.isPlaying)
        assertTrue(fakeEngine.releaseCalled)
    }

    @Test
    fun `handleCommand null is no-op`() {
        controller.handleCommand(null)

        assertFalse(controller.playbackState.value.isPlaying)
    }
}

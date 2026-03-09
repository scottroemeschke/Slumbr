package dev.ashera.slumbr.service

import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.playback.FakeAudioEngine
import dev.ashera.slumbr.playback.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServicePlaybackObserverTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var controller: PlaybackController
    private lateinit var fakeNotifier: FakePlaybackNotifier
    private lateinit var fakeMediaSession: FakeMediaSessionController
    private lateinit var fakeHost: FakeForegroundHost
    private lateinit var observer: ServicePlaybackObserver

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeAudioEngine()
        controller = PlaybackController(fakeEngine)
        fakeNotifier = FakePlaybackNotifier()
        fakeMediaSession = FakeMediaSessionController()
        fakeHost = FakeForegroundHost()
        observer = ServicePlaybackObserver(controller, fakeNotifier, fakeMediaSession)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `start playback promotes foreground and updates media session`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            controller.start(NoiseType.BROWN, 0.8f)

            assertEquals(1, fakeHost.promotedCount)
            assertEquals(NoiseType.BROWN, fakeNotifier.lastBuiltNoiseType)
            assertEquals(NoiseType.BROWN, fakeMediaSession.lastPlayingNoise)
        }

    @Test
    fun `switch noise updates notification without re-promoting`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            controller.start(NoiseType.BROWN, 0.8f)
            controller.switchNoise(NoiseType.PINK)

            assertEquals(1, fakeHost.promotedCount)
            assertEquals(NoiseType.PINK, fakeNotifier.lastUpdatedNoiseType)
            assertEquals(NoiseType.PINK, fakeMediaSession.lastPlayingNoise)
        }

    @Test
    fun `stop playback demotes foreground and updates media session`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            controller.start(NoiseType.WHITE, 0.8f)
            controller.hardStop()

            assertEquals(1, fakeHost.demotedCount)
            assertTrue(fakeMediaSession.stoppedCalled)
        }

    @Test
    fun `no demote when not foreground`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            // Never started, so stopping is a no-op for the host
            assertEquals(0, fakeHost.demotedCount)
        }

    @Test
    fun `fade-out completion demotes foreground`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            controller.start(NoiseType.BROWN, 0.8f)
            controller.gracefulStop()
            fakeEngine.simulateFadeOutComplete()

            assertEquals(1, fakeHost.demotedCount)
            assertTrue(fakeMediaSession.stoppedCalled)
        }

    @Test
    fun `restart after stop promotes foreground again`() =
        runTest(testDispatcher) {
            observer.observe(backgroundScope, fakeHost)

            controller.start(NoiseType.BROWN, 0.8f)
            controller.hardStop()
            controller.start(NoiseType.PINK, 0.8f)

            assertEquals(2, fakeHost.promotedCount)
            assertNull(fakeNotifier.lastUpdatedNoiseType)
        }
}

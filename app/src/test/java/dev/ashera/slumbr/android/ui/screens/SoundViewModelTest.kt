package dev.ashera.slumbr.android.ui.screens

import dev.ashera.slumbr.core.audio.NoiseType
import dev.ashera.slumbr.core.playback.FakeAudioEngine
import dev.ashera.slumbr.core.playback.FakeDndStateProvider
import dev.ashera.slumbr.core.playback.PlaybackController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SoundViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeEngine: FakeAudioEngine
    private lateinit var controller: PlaybackController
    private lateinit var fakeDnd: FakeDndStateProvider
    private lateinit var viewModel: SoundViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeEngine = FakeAudioEngine()
        controller = PlaybackController(fakeEngine)
        fakeDnd = FakeDndStateProvider()
        viewModel = SoundViewModel(controller, fakeDnd)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `select noise when stopped starts playback`() {
        viewModel.selectNoise(NoiseType.BROWN)

        assertEquals(NoiseType.BROWN, fakeEngine.startedNoiseType)
        assertTrue(viewModel.uiState.value.isPlaying)
        assertEquals(NoiseType.BROWN, viewModel.uiState.value.selectedNoise)
    }

    @Test
    fun `select same noise when playing triggers graceful stop`() {
        viewModel.selectNoise(NoiseType.BROWN)
        viewModel.selectNoise(NoiseType.BROWN)

        assertTrue(fakeEngine.stopCalled)
    }

    @Test
    fun `select different noise when playing switches noise`() {
        viewModel.selectNoise(NoiseType.BROWN)
        viewModel.selectNoise(NoiseType.PINK)

        assertEquals(NoiseType.PINK, fakeEngine.switchedNoiseType)
        assertTrue(viewModel.uiState.value.isPlaying)
        assertEquals(NoiseType.PINK, viewModel.uiState.value.selectedNoise)
    }

    @Test
    fun `switch noise sets instant transition`() {
        viewModel.selectNoise(NoiseType.BROWN)
        viewModel.selectNoise(NoiseType.PINK)

        assertTrue(viewModel.uiState.value.instantTransition)
    }

    @Test
    fun `start from stopped does not set instant transition`() {
        viewModel.selectNoise(NoiseType.BROWN)

        assertFalse(viewModel.uiState.value.instantTransition)
    }

    @Test
    fun `dnd total silence emits snackbar`() =
        runTest {
            fakeDnd.totalSilence = true
            val messages = mutableListOf<String>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.snackbarMessages.collect { messages.add(it) }
                }

            viewModel.selectNoise(NoiseType.BROWN)

            assertEquals(1, messages.size)
            assertEquals(SoundViewModel.DND_WARNING, messages.first())
            job.cancel()
        }

    @Test
    fun `dnd not total silence does not emit snackbar`() =
        runTest {
            fakeDnd.totalSilence = false
            val messages = mutableListOf<String>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.snackbarMessages.collect { messages.add(it) }
                }

            viewModel.selectNoise(NoiseType.BROWN)

            assertTrue(messages.isEmpty())
            job.cancel()
        }

    @Test
    fun `setVolume updates ui state and delegates to controller`() {
        viewModel.setVolume(0.5f)

        assertEquals(0.5f, viewModel.uiState.value.volume)
        assertEquals(0.5f, fakeEngine.lastVolume)
    }

    @Test
    fun `playback complete resets ui state`() {
        viewModel.selectNoise(NoiseType.BROWN)
        fakeEngine.simulateFadeOutComplete()

        assertFalse(viewModel.uiState.value.isPlaying)
        assertNull(viewModel.uiState.value.selectedNoise)
    }

    @Test
    fun `start uses current volume from ui state`() {
        viewModel.setVolume(0.3f)
        viewModel.selectNoise(NoiseType.WHITE)

        assertEquals(0.3f, fakeEngine.startedVolume)
    }
}

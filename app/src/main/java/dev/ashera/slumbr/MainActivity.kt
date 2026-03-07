package dev.ashera.slumbr

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ashera.slumbr.audio.NoiseType
import dev.ashera.slumbr.service.SoundService
import dev.ashera.slumbr.ui.screens.HomeScreen
import dev.ashera.slumbr.ui.screens.SoundViewModel
import dev.ashera.slumbr.ui.theme.SlumbrTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SoundViewModel by viewModels()
    private var soundService: SoundService? = null
    private var bound = false

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                val binder = service as SoundService.LocalBinder
                soundService = binder.service
                bound = true

                binder.service.onServiceStopped = {
                    viewModel.stop()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                soundService?.onServiceStopped = null
                soundService = null
                bound = false
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { /* Permission result — service works regardless */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            SlumbrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    HomeScreen(
                        uiState = uiState,
                        snackbarMessages = viewModel.snackbarMessages,
                        onNoiseSelected = { noiseType ->
                            handleNoiseSelected(noiseType)
                        },
                        onVolumeChanged = { volume ->
                            viewModel.setVolume(volume)
                            soundService?.audioEngine?.setVolume(volume)
                        },
                    )
                }
            }
        }
    }

    private fun handleNoiseSelected(noiseType: NoiseType) {
        val previousState = viewModel.uiState.value
        viewModel.selectNoise(noiseType)
        val newState = viewModel.uiState.value

        when {
            !newState.isPlaying -> {
                // Toggle off — graceful fade-out
                soundService?.audioEngine?.stop()
                    ?: gracefulStopSoundService()
            }
            previousState.isPlaying && previousState.selectedNoise != noiseType -> {
                // Switch noise type in-place
                soundService?.let {
                    it.audioEngine.switchNoise(noiseType)
                    it.updateNotification(noiseType)
                } ?: startSoundService(noiseType, newState.volume)
                checkDndTotalSilence()
            }
            else -> {
                // Start from stopped
                startSoundService(noiseType, newState.volume)
                checkDndTotalSilence()
            }
        }
    }

    private fun checkDndTotalSilence() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.currentInterruptionFilter ==
            NotificationManager.INTERRUPTION_FILTER_NONE
        ) {
            viewModel.showSnackbar(getString(R.string.dnd_total_silence_warning))
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, SoundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        soundService?.onServiceStopped = null
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun startSoundService(
        noiseType: NoiseType,
        volume: Float,
    ) {
        val intent = SoundService.startIntent(this, noiseType, volume)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun gracefulStopSoundService() {
        val intent = SoundService.gracefulStopIntent(this)
        startService(intent)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

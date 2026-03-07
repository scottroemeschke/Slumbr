package dev.ashera.slumbr

import android.Manifest
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
            }

            override fun onServiceDisconnected(name: ComponentName?) {
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
                        onNoiseSelected = { noiseType ->
                            viewModel.selectNoise(noiseType)
                            val state = viewModel.uiState.value
                            if (state.isPlaying && state.selectedNoise != null) {
                                startSoundService(state.selectedNoise!!, state.volume)
                            } else {
                                stopSoundService()
                            }
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

    override fun onStart() {
        super.onStart()
        Intent(this, SoundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    private fun startSoundService(
        noiseType: dev.ashera.slumbr.audio.NoiseType,
        volume: Float,
    ) {
        val intent = SoundService.startIntent(this, noiseType, volume)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopSoundService() {
        val intent = SoundService.stopIntent(this)
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

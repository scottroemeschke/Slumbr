package dev.ashera.slumbr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import dev.ashera.slumbr.service.SoundService
import dev.ashera.slumbr.ui.screens.HomeScreen
import dev.ashera.slumbr.ui.screens.SoundViewModel
import dev.ashera.slumbr.ui.theme.SlumbrTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SoundViewModel by viewModels()

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
                            val wasPlaying = uiState.isPlaying
                            viewModel.selectNoise(noiseType)
                            if (!wasPlaying) {
                                ContextCompat.startForegroundService(
                                    this@MainActivity,
                                    SoundService.startIntent(
                                        this@MainActivity,
                                        noiseType,
                                        uiState.volume,
                                    ),
                                )
                            }
                        },
                        onVolumeChanged = { volume ->
                            viewModel.setVolume(volume)
                        },
                    )
                }
            }
        }
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

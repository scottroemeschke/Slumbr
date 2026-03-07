package dev.ashera.slumbr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ashera.slumbr.audio.NoiseType
import kotlinx.coroutines.flow.SharedFlow

private const val FADE_IN_DURATION_MS = 2000
private const val FADE_OUT_DURATION_MS = 16000

@Composable
fun HomeScreen(
    uiState: SoundUiState,
    snackbarMessages: SharedFlow<String>,
    onNoiseSelected: (NoiseType) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "\uD83C\uDF19",
                    fontSize = 64.sp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Slumbr",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(NoiseType.entries.toList()) { noiseType ->
                    NoiseCard(
                        noiseType = noiseType,
                        isActive = uiState.isPlaying && uiState.selectedNoise == noiseType,
                        instantTransition = uiState.instantTransition,
                        onClick = { onNoiseSelected(noiseType) },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Slider(
                value = uiState.volume,
                onValueChange = onVolumeChanged,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun NoiseCard(
    noiseType: NoiseType,
    isActive: Boolean,
    instantTransition: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animSpec =
        if (instantTransition) {
            snap<Color>()
        } else {
            tween<Color>(
                durationMillis = if (isActive) FADE_IN_DURATION_MS else FADE_OUT_DURATION_MS,
                easing = EaseInOut,
            )
        }

    val containerColor by animateColorAsState(
        targetValue =
            if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = animSpec,
        label = "cardColor",
    )

    val contentColor by animateColorAsState(
        targetValue =
            if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = animSpec,
        label = "textColor",
    )

    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(108.dp),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${noiseType.displayName} Noise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
            )
        }
    }
}

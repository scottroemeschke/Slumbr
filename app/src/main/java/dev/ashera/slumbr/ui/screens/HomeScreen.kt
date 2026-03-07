package dev.ashera.slumbr.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ashera.slumbr.audio.NoiseType

@Composable
fun HomeScreen(
    uiState: SoundUiState,
    onNoiseSelected: (NoiseType) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Slumbr",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Light,
            letterSpacing = 4.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text =
                if (uiState.isPlaying && uiState.selectedNoise != null) {
                    "Playing ${uiState.selectedNoise.displayName} Noise"
                } else {
                    "Tap a sound to begin"
                },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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
                    onClick = { onNoiseSelected(noiseType) },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Volume slider
        Text(
            text = "Volume",
            style = MaterialTheme.typography.labelMedium,
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

@Composable
private fun NoiseCard(
    noiseType: NoiseType,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue =
            if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = tween(durationMillis = 300),
        label = "cardColor",
    )

    val contentColor by animateColorAsState(
        targetValue =
            if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = tween(durationMillis = 300),
        label = "textColor",
    )

    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .height(88.dp),
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

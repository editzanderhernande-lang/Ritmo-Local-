package com.example.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.LocalTrack
import com.example.model.VisualizerStyle
import com.example.model.formatDurationMs
import com.example.player.PlaybackUiState

@Composable
fun ImmersiveVisualOverlay(
    track: LocalTrack,
    playbackState: PlaybackUiState,
    audioSessionId: Int,
    initialVisualizerStyle: VisualizerStyle,
    effectsEnabled: Boolean,
    onToggleEffects: (Boolean) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var selectedStyle by remember(initialVisualizerStyle) { mutableStateOf(initialVisualizerStyle) }

    val pulseTransition = rememberInfiniteTransition(label = "immersive_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF07050F), primary.copy(alpha = 0.28f), secondary.copy(alpha = 0.18f), Color(0xFF050409))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("immersive_visual_overlay")
    ) {
        if (!track.albumArtUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(track.albumArtUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(28.dp) else Modifier)
                    .graphicsLayer { alpha = if (effectsEnabled) 0.42f else 0.18f }
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize().testTag("immersive_close_btn")) {
                    Icon(Icons.Rounded.Close, contentDescription = "Salir del modo inmersivo", tint = Color.White)
                }
                Text("Modo Visual Inmersivo", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(
                    onClick = { onToggleEffects(!effectsEnabled) },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("immersive_battery_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.BatterySaver,
                        contentDescription = if (effectsEnabled) "Desactivar efectos para ahorrar batería" else "Activar efectos visuales",
                        tint = if (effectsEnabled) primary else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            val s = if (effectsEnabled && playbackState.isPlaying) pulseScale else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .clip(RoundedCornerShape(28.dp))
                        .border(2.dp, primary.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                ) {
                    AlbumArtwork(
                        albumArtUri = track.albumArtUri,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 28.dp
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${track.artist} · ${formatDurationMs(playbackState.positionMs)} / ${formatDurationMs(playbackState.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (effectsEnabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioVisualizerView(
                        audioSessionId = audioSessionId,
                        isPlaying = playbackState.isPlaying,
                        animationsEnabled = effectsEnabled,
                        showVisualizer = effectsEnabled,
                        accentColor = primary,
                        secondaryColor = secondary,
                        style = com.example.model.PlayerVisualStyle.NEON_PULSE,
                        visualizerStyle = selectedStyle,
                        intensity = 1.15f,
                        modifier = Modifier.fillMaxWidth().height(92.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VisualizerStyle.entries.forEach { st ->
                            FilterChip(
                                selected = selectedStyle == st,
                                onClick = {
                                    selectedStyle = st
                                    onSelectVisualizerStyle(st)
                                },
                                label = { Text(st.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.1f)) {
                    Text(
                        text = "Efectos desactivados para ahorrar batería",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = primary, contentColor = Color.White),
                    modifier = Modifier.size(72.dp).testTag("immersive_play_pause_btn")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
        }
    }
}

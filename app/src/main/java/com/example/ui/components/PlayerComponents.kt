package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.LyricsRepository
import com.example.data.TrackLyrics
import com.example.model.LocalTrack
import com.example.model.TimeOfDaySlot
import com.example.model.UiStyleMode
import com.example.model.formatDurationMs
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerUiState
import com.example.player.formatSleepTimerCountdown
import com.example.ui.i18n.LocalExtraStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AfternoonAccent
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.MorningAccent
import com.example.ui.theme.NightAccent

@Composable
fun AlbumArtwork(
    albumArtUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (albumArtUri.isNullOrBlank()) {
            Image(
                painter = painterResource(id = R.drawable.img_album_placeholder),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val imageRequest = remember(context, albumArtUri) {
                ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(true)
                    .error(R.drawable.img_album_placeholder)
                    .placeholder(R.drawable.img_album_placeholder)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

fun slotAccentColor(slot: TimeOfDaySlot): Color = when (slot) {
    TimeOfDaySlot.MORNING -> MorningAccent
    TimeOfDaySlot.AFTERNOON -> AfternoonAccent
    TimeOfDaySlot.NIGHT -> NightAccent
}

@Composable
fun UserProfileAvatar(
    avatarUri: String?,
    avatarBase64: String,
    fallbackPhotoUrl: String?,
    displayName: String,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val decodedBitmap = remember(avatarBase64) {
        if (avatarBase64.isBlank()) null else try {
            val raw = Base64.decode(avatarBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(raw, 0, raw.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "R"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(primary.copy(alpha = 0.85f), secondary.copy(alpha = 0.85f))
                )
            )
            .border(1.2.dp, primary.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = "Foto de perfil de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            !avatarUri.isNullOrBlank() || !fallbackPhotoUrl.isNullOrBlank() -> {
                val modelUrl = avatarUri?.takeIf { it.isNotBlank() } ?: fallbackPhotoUrl
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(modelUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto de perfil de $displayName",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Text(
                    text = initial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackUiState,
    activeSlot: TimeOfDaySlot,
    isFavorite: Boolean,
    sleepTimerState: SleepTimerUiState = SleepTimerUiState(),
    animationsEnabled: Boolean = true,
    handsFreeVoiceActive: Boolean = false,
    onOpenSleepTimer: () -> Unit = {},
    onOpenVoiceControl: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    if (animationsEnabled) {
        AnimatedVisibility(
            visible = track != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = modifier
        ) {
            if (track != null) {
                MiniPlayerBarContent(track, playbackState, isFavorite, sleepTimerState, handsFreeVoiceActive, onOpenSleepTimer, onOpenVoiceControl, onToggleFavorite, onTogglePlayPause, onSkipNext, onExpand)
            }
        }
    } else if (track != null) {
        Box(modifier = modifier) {
            MiniPlayerBarContent(track, playbackState, isFavorite, sleepTimerState, handsFreeVoiceActive, onOpenSleepTimer, onOpenVoiceControl, onToggleFavorite, onTogglePlayPause, onSkipNext, onExpand)
        }
    }
}

@Composable
private fun MiniPlayerBarContent(
    track: LocalTrack,
    playbackState: PlaybackUiState,
    isFavorite: Boolean,
    sleepTimerState: SleepTimerUiState,
    handsFreeVoiceActive: Boolean,
    onOpenSleepTimer: () -> Unit,
    onOpenVoiceControl: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onExpand: () -> Unit
) {
    val uiStyle = LocalUiStyleMode.current
    val extreme = com.example.ui.theme.LocalExtremeVisualStyle.current
    val isMinimal = uiStyle == UiStyleMode.MINIMALIST
    val isNeon = uiStyle == UiStyleMode.NEON_MODERN
    val primary = MaterialTheme.colorScheme.primary
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val cardCorner = when (extreme.miniPlayerBarStyle) {
        com.example.model.MiniPlayerBarStyle.FLOATING_PILL -> extreme.cardCornerDp.dp
        com.example.model.MiniPlayerBarStyle.DOCKED_GLASS -> (extreme.cardCornerDp * 0.55f).dp
        com.example.model.MiniPlayerBarStyle.MINIMAL_LINE -> 6.dp
    }
    val horizontalPad = if (extreme.miniPlayerBarStyle == com.example.model.MiniPlayerBarStyle.DOCKED_GLASS) 0.dp else 12.dp
    val cardShape = RoundedCornerShape(cardCorner)

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = extreme.surfaceTransparencyAlpha),
        tonalElevation = if (isMinimal) 1.dp else 6.dp,
        shadowElevation = extreme.shadowElevationDp.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPad, vertical = 6.dp)
            .border(
                width = if (extreme.miniPlayerBarStyle == com.example.model.MiniPlayerBarStyle.MINIMAL_LINE) 0.5.dp else extreme.cardBorderWidthDp.dp,
                color = if (isNeon) primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = if (isMinimal) 0.35f else 0.6f),
                shape = cardShape
            )
            .clickable(onClick = onExpand)
            .testTag("mini_player_bar")
    ) {
        Column {
            if (sleepTimerState.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primary.copy(alpha = 0.14f))
                        .clickable(onClick = onOpenSleepTimer)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("mini_player_sleep_timer_banner"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (sleepTimerState.stopAfterCurrentTrack) {
                                "Apagado al finalizar esta pista"
                            } else {
                                "Apagado en ${formatSleepTimerCountdown(sleepTimerState.remainingSeconds)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("Ajustar", style = MaterialTheme.typography.labelSmall, color = primary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    albumArtUri = track.albumArtUri,
                    contentDescription = "Portada de ${track.title}",
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.artist} · ${track.formatLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(
                    onClick = onOpenVoiceControl,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_voice")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voz Ritmo",
                        tint = if (handsFreeVoiceActive) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_favorite")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                        tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                        tint = primary
                    )
                }
                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_player_next")
                ) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente canción")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.5.dp),
                color = primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    track: LocalTrack,
    playbackState: PlaybackUiState,
    activeSlot: TimeOfDaySlot,
    isFavorite: Boolean,
    sleepTimerState: SleepTimerUiState,
    isSyncing: Boolean,
    isSignedIn: Boolean,
    currentLyrics: TrackLyrics? = null,
    handsFreeVoiceActive: Boolean = false,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenCastSheet: () -> Unit,
    onOpenVoiceControl: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onShareTrack: () -> Unit,
    onChangeCover: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSaveToCloud: () -> Unit
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val duration = playbackState.durationMs.coerceAtLeast(1L)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val sliderValue = if (isScrubbing) scrubPosition else (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val uiStyle = LocalUiStyleMode.current
    val isMinimal = uiStyle == UiStyleMode.MINIMALIST
    val isNeon = uiStyle == UiStyleMode.NEON_MODERN

    if (isNeon) {
        var showMoreMenu by remember { mutableStateOf(false) }
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = com.example.ui.theme.NeonBg,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                primary.copy(alpha = 0.28f),
                                com.example.ui.theme.NeonBg,
                                Color(0xFF06040C)
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.ExpandMore, contentDescription = "Cerrar reproductor", tint = Color.White)
                    }
                    Text(
                        text = "Reproduciendo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onOpenEqualizer, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_open_eq_button")) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = "Ecualizador y Dolby Atmos", tint = primary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(276.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.5.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .clickable(onClick = onChangeCover)
                ) {
                    AlbumArtwork(
                        albumArtUri = track.albumArtUri,
                        contentDescription = "Portada del álbum ${track.album}",
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 24.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("player_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                                tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Opciones de pista",
                                    tint = Color.White
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(extra.lyricsLabel) },
                                    leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                                    onClick = { showMoreMenu = false; onOpenLyrics() }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(strings.changeCover) },
                                    leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                                    onClick = { showMoreMenu = false; onChangeCover() }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Temporizador de sueño") },
                                    leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                                    onClick = { showMoreMenu = false; onOpenSleepTimer() }
                                )
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(if (isSignedIn) "Guardar en la nube" else "Guardar progreso") },
                                    leadingIcon = { Icon(Icons.Rounded.CloudUpload, contentDescription = null) },
                                    onClick = { showMoreMenu = false; onSaveToCloud() }
                                )
                            }
                        }
                    }
                }

                val liveLine = remember(currentLyrics, playbackState.positionMs) {
                    if (currentLyrics != null && currentLyrics.lines.isNotEmpty()) {
                        val idx = LyricsRepository.findActiveLineIndex(currentLyrics.lines, playbackState.positionMs)
                        if (idx >= 0) currentLyrics.lines[idx].text else currentLyrics.lines.firstOrNull { it.text.isNotBlank() }?.text
                    } else null
                }
                if (!liveLine.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenLyrics)
                            .testTag("player_live_lyric_preview")
                    ) {
                        Text(
                            text = "♪ $liveLine",
                            style = MaterialTheme.typography.bodySmall,
                            color = primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isScrubbing = true
                        scrubPosition = it
                    },
                    onValueChangeFinished = {
                        onSeekTo((scrubPosition * duration).toLong())
                        isScrubbing = false
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = primary,
                        activeTrackColor = primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val displayedMs = if (isScrubbing) (scrubPosition * duration).toLong() else playbackState.positionMs
                    Text(formatDurationMs(displayedMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDurationMs(playbackState.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Alternar aleatorio",
                            tint = if (playbackState.shuffleEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onSkipPrevious, modifier = Modifier.size(50.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = primary, contentColor = Color.White),
                        modifier = Modifier.size(70.dp).testTag("player_sheet_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    IconButton(onClick = onSkipNext, modifier = Modifier.size(50.dp)) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                        val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                        val repeatTint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(imageVector = repeatIcon, contentDescription = "Repetir", tint = repeatTint)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = com.example.ui.theme.NeonSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenEqualizer)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Sonido 3D Dolby Atmos · 32-bit Hi-Fi",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Activo · ${activeSlot.displayName} · ${track.formatLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.EmeraldSync
                                )
                            }
                        }
                        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mockup Panel 3: Bottom 5-icon tool bar (Letras | Compartir | TV Cast | Voz | Ocultar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onOpenLyrics, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_lyrics_button")) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = extra.lyricsLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onShareTrack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Share, contentDescription = strings.shareTrack, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenCastSheet, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Cast, contentDescription = strings.castToTv, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenVoiceControl, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_voice_button")) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Voz sin internet",
                            tint = if (handsFreeVoiceActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onOpenSleepTimer, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_sleep_timer_button")) {
                        Icon(
                            Icons.Rounded.Bedtime,
                            contentDescription = "Temporizador",
                            tint = if (sleepTimerState.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = if (isMinimal) 0.06f else 0.18f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = "Cerrar reproductor")
                }
                Surface(shape = RoundedCornerShape(50), color = primary.copy(alpha = if (isMinimal) 0.12f else 0.18f)) {
                    Text(
                        text = "${activeSlot.displayName} · ${track.formatLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Row {
                    IconButton(onClick = onOpenVoiceControl, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_voice_button")) {
                        Icon(Icons.Rounded.Mic, contentDescription = "Voz sin internet", tint = primary)
                    }
                    IconButton(onClick = onOpenCastSheet, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.Cast, contentDescription = strings.castToTv, tint = primary)
                    }
                    IconButton(onClick = onOpenSleepTimer, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_sleep_timer_button")) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = "Temporizador", tint = if (sleepTimerState.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onOpenEqualizer, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_open_eq_button")) {
                        Icon(Icons.Rounded.Equalizer, contentDescription = "Ecualizador y 3D", tint = primary)
                    }
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.minimumInteractiveComponentSize().testTag("player_favorite_button")) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                    .clickable(onClick = onChangeCover)
            ) {
                AlbumArtwork(
                    albumArtUri = track.albumArtUri,
                    contentDescription = "Portada del álbum ${track.album}",
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 28.dp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(track.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(10.dp))

            val liveLine = remember(currentLyrics, playbackState.positionMs) {
                if (currentLyrics != null && currentLyrics.lines.isNotEmpty()) {
                    val idx = LyricsRepository.findActiveLineIndex(currentLyrics.lines, playbackState.positionMs)
                    if (idx >= 0) currentLyrics.lines[idx].text else currentLyrics.lines.firstOrNull { it.text.isNotBlank() }?.text
                } else null
            }
            if (!liveLine.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = primary.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenLyrics)
                        .testTag("player_live_lyric_preview")
                ) {
                    Text(
                        text = "♪ $liveLine",
                        style = MaterialTheme.typography.bodySmall,
                        color = primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onChangeCover) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.changeCover, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenLyrics,
                    modifier = Modifier.testTag("player_lyrics_button")
                ) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(extra.lyricsLabel, maxLines = 1)
                }
                OutlinedButton(onClick = onShareTrack) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.shareTrack, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Slider(
                value = sliderValue,
                onValueChange = {
                    isScrubbing = true
                    scrubPosition = it
                },
                onValueChangeFinished = {
                    onSeekTo((scrubPosition * duration).toLong())
                    isScrubbing = false
                },
                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary, inactiveTrackColor = MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("player_seek_slider")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val displayedMs = if (isScrubbing) (scrubPosition * duration).toLong() else playbackState.positionMs
                Text(formatDurationMs(displayedMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDurationMs(playbackState.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Alternar aleatorio",
                        tint = if (playbackState.shuffleEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(34.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.size(72.dp).testTag("player_sheet_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                    val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                    val repeatTint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(imageVector = repeatIcon, contentDescription = "Repetir", tint = repeatTint)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSaveToCloud,
                enabled = !isSyncing,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = primary),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("player_save_cloud_button")
            ) {
                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSyncing -> "Guardando progreso…"
                        isSignedIn -> "Guardar progreso y posición en la nube"
                        else -> "Guardar progreso localmente (Modo Offline)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

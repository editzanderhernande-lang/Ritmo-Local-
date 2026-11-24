package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.Player
import com.example.data.LyricsRepository
import com.example.data.TrackLyrics
import com.example.model.ArtworkPositionMode
import com.example.model.LocalTrack
import com.example.model.PlayerBackgroundMode
import com.example.model.PlayerControlStyle
import com.example.model.PlayerDesignerConfig
import com.example.model.PlayerLayoutMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerVisualPrefs
import com.example.model.PlayerVisualStyle
import com.example.model.ProgressBarVisualStyle
import com.example.model.TimeOfDaySlot
import com.example.model.formatDurationMs
import com.example.player.EqualizerUiState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerUiState
import com.example.player.formatSleepTimerCountdown
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.AudioVisualizerView
import com.example.ui.components.CoverPaletteColors
import com.example.ui.components.defaultCoverPalette
import com.example.ui.components.extractCoverPaletteColors

@Composable
fun AdvancedNowPlayingScreen(
    track: LocalTrack,
    playbackState: PlaybackUiState,
    activeSlot: TimeOfDaySlot,
    isFavorite: Boolean,
    sleepTimerState: SleepTimerUiState,
    eqState: EqualizerUiState,
    audioSessionId: Int,
    visualPrefs: PlayerVisualPrefs,
    designerConfig: PlayerDesignerConfig = PlayerDesignerConfig.defaultConfig(),
    isDarkTheme: Boolean,
    animationsEnabled: Boolean,
    isSignedIn: Boolean,
    currentLyrics: TrackLyrics?,
    isLoadingLyrics: Boolean,
    handsFreeVoiceActive: Boolean,
    crossfadeOrDjActive: Boolean = false,
    onOpenProControls: () -> Unit = {},
    onOpenPlayerDesigner: () -> Unit = {},
    onOpenImmersiveVisual: () -> Unit = {},
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenEqualizerSheet: () -> Unit,
    onOpenCustomizeSheet: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenCastSheet: () -> Unit,
    onOpenVoiceControl: () -> Unit,
    onOpenFullLyricsEditor: () -> Unit,
    onSearchOnlineLyrics: () -> Unit,
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
    BackHandler { onDismiss() }

    val context = LocalContext.current
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val effectiveIsDark = when (visualPrefs.backgroundMode) {
        PlayerBackgroundMode.DARK_SOLID -> true
        PlayerBackgroundMode.LIGHT_CLEAN -> false
        else -> isDarkTheme
    }

    var coverPalette by remember(track.albumArtUri, effectiveIsDark, themePrimary) {
        mutableStateOf(defaultCoverPalette(themePrimary, effectiveIsDark))
    }
    LaunchedEffect(track.albumArtUri, effectiveIsDark, themePrimary) {
        coverPalette = extractCoverPaletteColors(context, track.albumArtUri, themePrimary, effectiveIsDark)
    }

    val primaryAccent = when {
        visualPrefs.customPrimaryColorArgb != 0L -> Color(visualPrefs.customPrimaryColorArgb)
        visualPrefs.backgroundMode == PlayerBackgroundMode.DYNAMIC_COVER -> coverPalette.vibrant
        else -> themePrimary
    }
    val buttonAccent = when {
        visualPrefs.customButtonColorArgb != 0L -> Color(visualPrefs.customButtonColorArgb)
        else -> primaryAccent
    }
    val onScreenText = if (effectiveIsDark) Color(0xFFF8F7FC) else Color(0xFF14111F)
    val onScreenSubtle = if (effectiveIsDark) Color(0xFFB5B0C9) else Color(0xFF57526A)

    val bgBrush = remember(visualPrefs.backgroundMode, coverPalette, primaryAccent, themeSecondary, effectiveIsDark) {
        when (visualPrefs.backgroundMode) {
            PlayerBackgroundMode.DYNAMIC_COVER -> Brush.verticalGradient(
                listOf(coverPalette.bgTop, coverPalette.bgBottom)
            )
            PlayerBackgroundMode.GRADIENT_CUSTOM -> Brush.verticalGradient(
                listOf(
                    primaryAccent.copy(alpha = if (effectiveIsDark) 0.42f else 0.25f),
                    if (effectiveIsDark) Color(0xFF0A0814) else Color(0xFFF5F3FA),
                    themeSecondary.copy(alpha = if (effectiveIsDark) 0.22f else 0.14f)
                )
            )
            PlayerBackgroundMode.DARK_SOLID -> Brush.verticalGradient(
                listOf(Color(0xFF120F1D), Color(0xFF06040A))
            )
            PlayerBackgroundMode.LIGHT_CLEAN -> Brush.verticalGradient(
                listOf(Color(0xFFF7F5FB), Color(0xFFFFFFFF))
            )
        }
    }

    var showInlineLyricsPanel by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .testTag("advanced_now_playing_screen")
    ) {
        if (visualPrefs.backgroundMode == PlayerBackgroundMode.DYNAMIC_COVER &&
            visualPrefs.blurIntensity > 0.05f &&
            !track.albumArtUri.isNullOrBlank()
        ) {
            val blurRadiusDp = (visualPrefs.blurIntensity.coerceIn(0f, 1f) * 26f)
            val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            AsyncImage(
                model = ImageRequest.Builder(context).data(track.albumArtUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canBlur) Modifier.blur(blurRadiusDp.dp) else Modifier)
                    .graphicsLayer { alpha = if (canBlur) 0.45f else 0.25f }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                (if (effectiveIsDark) Color(0xCC0A0814) else Color(0xCCEFEBF9)),
                                (if (effectiveIsDark) Color(0xEB07050E) else Color(0xEBF7F5FB))
                            )
                        )
                    )
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 16.dp else 20.dp,
                    vertical = if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 6.dp else 10.dp
                )
        ) {
            val constraintMaxHeight = maxHeight
            val isWideOrLandscape = maxWidth >= 600.dp || (maxWidth > constraintMaxHeight && maxWidth >= 480.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                NowPlayingTopBar(
                    track = track,
                    activeSlot = activeSlot,
                    sleepTimerState = sleepTimerState,
                    eqEnabled = eqState.enabled,
                    crossfadeOrDjActive = crossfadeOrDjActive,
                    accentColor = primaryAccent,
                    textColor = onScreenText,
                    subtleColor = onScreenSubtle,
                    isSignedIn = isSignedIn,
                    onDismiss = onDismiss,
                    onOpenSleepTimer = onOpenSleepTimer,
                    onOpenEqualizerSheet = onOpenEqualizerSheet,
                    onOpenProControls = onOpenProControls,
                    onOpenPlayerDesigner = onOpenPlayerDesigner,
                    onOpenImmersiveVisual = onOpenImmersiveVisual,
                    onOpenCustomizeSheet = onOpenCustomizeSheet,
                    onOpenFullLyricsEditor = onOpenFullLyricsEditor,
                    onChangeCover = onChangeCover,
                    onSaveToCloud = onSaveToCloud,
                    onShareTrack = onShareTrack,
                    onOpenCastSheet = onOpenCastSheet
                )

                if (isWideOrLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            CenterpieceArtworkOrLyrics(
                                track = track,
                                playbackState = playbackState,
                                visualPrefs = visualPrefs,
                                animationsEnabled = animationsEnabled,
                                primaryAccent = primaryAccent,
                                secondaryAccent = themeSecondary,
                                textColor = onScreenText,
                                subtleColor = onScreenSubtle,
                                isDarkSurface = effectiveIsDark,
                                showInlineLyricsPanel = showInlineLyricsPanel,
                                currentLyrics = currentLyrics,
                                isLoadingLyrics = isLoadingLyrics,
                                maxArtSizeDp = 260.dp,
                                onToggleLyricsPanel = { showInlineLyricsPanel = !showInlineLyricsPanel },
                                onChangeCover = onChangeCover,
                                onSeekTo = onSeekTo,
                                onSearchOnlineLyrics = onSearchOnlineLyrics,
                                onOpenFullLyricsEditor = onOpenFullLyricsEditor
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TrackMetadataAndFavorite(
                                track = track,
                                isFavorite = isFavorite,
                                buttonAccent = buttonAccent,
                                textColor = onScreenText,
                                subtleColor = onScreenSubtle,
                                currentLyrics = currentLyrics,
                                positionMs = playbackState.positionMs,
                                showInlineLyricsBar = visualPrefs.showInlineLyricsBar && !showInlineLyricsPanel,
                                onToggleFavorite = onToggleFavorite,
                                onOpenInlineLyrics = { showInlineLyricsPanel = true }
                            )
                            AudioVisualizerView(
                                audioSessionId = audioSessionId,
                                isPlaying = playbackState.isPlaying,
                                animationsEnabled = animationsEnabled,
                                showVisualizer = visualPrefs.showVisualizer,
                                accentColor = primaryAccent,
                                secondaryColor = themeSecondary,
                                style = visualPrefs.style,
                                visualizerStyle = visualPrefs.visualizerStyle,
                                intensity = visualPrefs.visualizerIntensity
                            )
                            SeekProgressAndTransport(
                                playbackState = playbackState,
                                animationsEnabled = animationsEnabled,
                                controlStyle = visualPrefs.controlStyle,
                                designerConfig = designerConfig,
                                buttonAccent = buttonAccent,
                                textColor = onScreenText,
                                subtleColor = onScreenSubtle,
                                onSeekTo = onSeekTo,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipPrevious = onSkipPrevious,
                                onSkipNext = onSkipNext,
                                onToggleShuffle = onToggleShuffle,
                                onCycleRepeat = onCycleRepeat
                            )
                            NowPlayingActionDock(
                                queueCount = playbackState.queue.size,
                                showLyricsActive = showInlineLyricsPanel,
                                sleepTimerActive = sleepTimerState.isActive,
                                eqEnabled = eqState.enabled,
                                handsFreeVoiceActive = handsFreeVoiceActive,
                                controlStyle = visualPrefs.controlStyle,
                                designerConfig = designerConfig,
                                accentColor = primaryAccent,
                                subtleColor = onScreenSubtle,
                                isDarkSurface = effectiveIsDark,
                                onOpenQueue = onOpenQueue,
                                onOpenEqualizerSheet = onOpenEqualizerSheet,
                                onToggleLyrics = { showInlineLyricsPanel = !showInlineLyricsPanel },
                                onOpenSleepTimer = onOpenSleepTimer,
                                onOpenVoiceControl = onOpenVoiceControl
                            )
                        }
                    }
                } else {
                    val baseArtMax = when (visualPrefs.layoutMode) {
                        PlayerLayoutMode.IMMERSIVE -> (constraintMaxHeight * 0.44f).coerceIn(220.dp, 350.dp)
                        PlayerLayoutMode.COMPACT -> (constraintMaxHeight * 0.28f).coerceIn(145.dp, 230.dp)
                        PlayerLayoutMode.STANDARD -> (constraintMaxHeight * 0.38f).coerceIn(190.dp, 310.dp)
                    }
                    val dynamicArtMax = baseArtMax * designerConfig.artworkScale.coerceIn(0.75f, 1.18f)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 2.dp else 6.dp))
                        if (designerConfig.artworkPosition == ArtworkPositionMode.CENTER) {
                            TrackMetadataAndFavorite(
                                track = track,
                                isFavorite = isFavorite,
                                buttonAccent = buttonAccent,
                                textColor = onScreenText,
                                subtleColor = onScreenSubtle,
                                currentLyrics = currentLyrics,
                                positionMs = playbackState.positionMs,
                                showInlineLyricsBar = visualPrefs.showInlineLyricsBar && !showInlineLyricsPanel,
                                onToggleFavorite = onToggleFavorite,
                                onOpenInlineLyrics = { showInlineLyricsPanel = true }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        CenterpieceArtworkOrLyrics(
                            track = track,
                            playbackState = playbackState,
                            visualPrefs = visualPrefs,
                            animationsEnabled = animationsEnabled,
                            primaryAccent = primaryAccent,
                            secondaryAccent = themeSecondary,
                            textColor = onScreenText,
                            subtleColor = onScreenSubtle,
                            isDarkSurface = effectiveIsDark,
                            showInlineLyricsPanel = showInlineLyricsPanel,
                            currentLyrics = currentLyrics,
                            isLoadingLyrics = isLoadingLyrics,
                            maxArtSizeDp = dynamicArtMax,
                            onToggleLyricsPanel = { showInlineLyricsPanel = !showInlineLyricsPanel },
                            onChangeCover = onChangeCover,
                            onSeekTo = onSeekTo,
                            onSearchOnlineLyrics = onSearchOnlineLyrics,
                            onOpenFullLyricsEditor = onOpenFullLyricsEditor
                        )
                        if (designerConfig.artworkPosition != ArtworkPositionMode.CENTER) {
                            Spacer(modifier = Modifier.height(if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 6.dp else 14.dp))
                            TrackMetadataAndFavorite(
                                track = track,
                                isFavorite = isFavorite,
                                buttonAccent = buttonAccent,
                                textColor = onScreenText,
                                subtleColor = onScreenSubtle,
                                currentLyrics = currentLyrics,
                                positionMs = playbackState.positionMs,
                                showInlineLyricsBar = visualPrefs.showInlineLyricsBar && !showInlineLyricsPanel,
                                onToggleFavorite = onToggleFavorite,
                                onOpenInlineLyrics = { showInlineLyricsPanel = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 4.dp else 8.dp))
                        AudioVisualizerView(
                            audioSessionId = audioSessionId,
                            isPlaying = playbackState.isPlaying,
                            animationsEnabled = animationsEnabled,
                            showVisualizer = visualPrefs.showVisualizer,
                            accentColor = primaryAccent,
                            secondaryColor = themeSecondary,
                            style = visualPrefs.style,
                            visualizerStyle = visualPrefs.visualizerStyle,
                            intensity = visualPrefs.visualizerIntensity
                        )
                        SeekProgressAndTransport(
                            playbackState = playbackState,
                            animationsEnabled = animationsEnabled,
                            controlStyle = visualPrefs.controlStyle,
                            designerConfig = designerConfig,
                            buttonAccent = buttonAccent,
                            textColor = onScreenText,
                            subtleColor = onScreenSubtle,
                            onSeekTo = onSeekTo,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipPrevious = onSkipPrevious,
                            onSkipNext = onSkipNext,
                            onToggleShuffle = onToggleShuffle,
                            onCycleRepeat = onCycleRepeat
                        )
                        Spacer(modifier = Modifier.height(if (visualPrefs.layoutMode == PlayerLayoutMode.COMPACT) 4.dp else 10.dp))
                        NowPlayingActionDock(
                            queueCount = playbackState.queue.size,
                            showLyricsActive = showInlineLyricsPanel,
                            sleepTimerActive = sleepTimerState.isActive,
                            eqEnabled = eqState.enabled,
                            handsFreeVoiceActive = handsFreeVoiceActive,
                            controlStyle = visualPrefs.controlStyle,
                            designerConfig = designerConfig,
                            accentColor = primaryAccent,
                            subtleColor = onScreenSubtle,
                            isDarkSurface = effectiveIsDark,
                            onOpenQueue = onOpenQueue,
                            onOpenEqualizerSheet = onOpenEqualizerSheet,
                            onToggleLyrics = { showInlineLyricsPanel = !showInlineLyricsPanel },
                            onOpenSleepTimer = onOpenSleepTimer,
                            onOpenVoiceControl = onOpenVoiceControl
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingTopBar(
    track: LocalTrack,
    activeSlot: TimeOfDaySlot,
    sleepTimerState: SleepTimerUiState,
    eqEnabled: Boolean,
    crossfadeOrDjActive: Boolean,
    accentColor: Color,
    textColor: Color,
    subtleColor: Color,
    isSignedIn: Boolean,
    onDismiss: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizerSheet: () -> Unit,
    onOpenProControls: () -> Unit,
    onOpenPlayerDesigner: () -> Unit,
    onOpenImmersiveVisual: () -> Unit,
    onOpenCustomizeSheet: () -> Unit,
    onOpenFullLyricsEditor: () -> Unit,
    onChangeCover: () -> Unit,
    onSaveToCloud: () -> Unit,
    onShareTrack: () -> Unit,
    onOpenCastSheet: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("player_collapse_button")
        ) {
            Icon(Icons.Rounded.ExpandMore, contentDescription = "Minimizar reproductor", tint = textColor)
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = accentColor.copy(alpha = 0.16f),
            modifier = Modifier
                .clickable(onClick = onOpenProControls)
                .padding(horizontal = 4.dp)
                .testTag("player_header_status_pill")
        ) {
            val statusLabel = when {
                sleepTimerState.isActive && sleepTimerState.remainingSongsToStop > 0 ->
                    "⏰ ${sleepTimerState.remainingSongsToStop} pistas"
                sleepTimerState.isActive && sleepTimerState.stopAfterCurrentTrack ->
                    "⏰ Fin de canción"
                sleepTimerState.isActive ->
                    "⏰ ${formatSleepTimerCountdown(sleepTimerState.remainingSeconds)}"
                crossfadeOrDjActive -> "${activeSlot.displayName} · DJ / Mix Pro"
                eqEnabled -> "${activeSlot.displayName} · ${track.formatLabel} · EQ"
                else -> "${activeSlot.displayName} · ${track.formatLabel}"
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenImmersiveVisual,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_immersive_mode_button")
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = "Modo Visual Inmersivo",
                    tint = if (crossfadeOrDjActive) accentColor else textColor
                )
            }
            IconButton(
                onClick = onOpenCustomizeSheet,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_customize_button")
            ) {
                Icon(Icons.Rounded.Palette, contentDescription = "Personalizar reproductor", tint = accentColor)
            }
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_more_menu_button")
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Más opciones", tint = textColor)
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Diseñador del reproductor") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenPlayerDesigner() },
                        modifier = Modifier.testTag("player_menu_open_designer")
                    )
                    DropdownMenuItem(
                        text = { Text("Modo Visual Inmersivo") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenImmersiveVisual() }
                    )
                    DropdownMenuItem(
                        text = { Text("Motor DJ, Crossfade y Ondas") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenProControls() }
                    )
                    DropdownMenuItem(
                        text = { Text("Ecualizador rápido") },
                        leadingIcon = { Icon(Icons.Rounded.Equalizer, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenEqualizerSheet() }
                    )
                    DropdownMenuItem(
                        text = { Text("Temporizador de apagado") },
                        leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenSleepTimer() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cambiar carátula") },
                        leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onChangeCover() }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar o editar letra (.LRC)") },
                        leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenFullLyricsEditor() }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar a TV") },
                        leadingIcon = { Icon(Icons.Rounded.Cast, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onOpenCastSheet() }
                    )
                    DropdownMenuItem(
                        text = { Text("Compartir canción") },
                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onShareTrack() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isSignedIn) "Guardar progreso en nube" else "Guardar progreso local") },
                        leadingIcon = { Icon(Icons.Rounded.CloudUpload, contentDescription = null) },
                        onClick = { showOverflowMenu = false; onSaveToCloud() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterpieceArtworkOrLyrics(
    track: LocalTrack,
    playbackState: PlaybackUiState,
    visualPrefs: PlayerVisualPrefs,
    animationsEnabled: Boolean,
    primaryAccent: Color,
    secondaryAccent: Color,
    textColor: Color,
    subtleColor: Color,
    isDarkSurface: Boolean,
    showInlineLyricsPanel: Boolean,
    currentLyrics: TrackLyrics?,
    isLoadingLyrics: Boolean,
    maxArtSizeDp: androidx.compose.ui.unit.Dp,
    onToggleLyricsPanel: () -> Unit,
    onChangeCover: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSearchOnlineLyrics: () -> Unit,
    onOpenFullLyricsEditor: () -> Unit
) {
    if (showInlineLyricsPanel) {
        InlineSyncedLyricsCard(
            track = track,
            lyrics = currentLyrics,
            positionMs = playbackState.positionMs,
            isLoading = isLoadingLyrics,
            accentColor = primaryAccent,
            textColor = textColor,
            subtleColor = subtleColor,
            isDarkSurface = isDarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(maxArtSizeDp + 24.dp),
            onSeekTo = onSeekTo,
            onSearchOnline = onSearchOnlineLyrics,
            onOpenEditor = onOpenFullLyricsEditor,
            onCloseLyrics = onToggleLyricsPanel
        )
        return
    }

    val isVinyl = visualPrefs.style == PlayerVisualStyle.VINYL_STUDIO
    val isNeon = visualPrefs.style == PlayerVisualStyle.NEON_PULSE
    val isMinimal = visualPrefs.style == PlayerVisualStyle.MINIMAL_ZEN

    val playScale by animateFloatAsState(
        targetValue = if (!animationsEnabled || playbackState.isPlaying) 1.0f else 0.93f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "art_scale"
    )
    val vinylSpinAngle = if (isVinyl && animationsEnabled && playbackState.isPlaying) {
        val infinite = rememberInfiniteTransition(label = "vinyl_spin")
        val deg by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
            label = "vinyl_deg"
        )
        deg
    } else 0f

    val cornerDp = if (isVinyl) 999.dp else if (isMinimal) 22.dp else 28.dp

    val renderArt: @Composable (LocalTrack) -> Unit = { targetTrack ->
        Box(
            modifier = Modifier
                .size(maxArtSizeDp)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                    rotationZ = vinylSpinAngle
                }
                .shadow(
                    elevation = if (isNeon) 22.dp else if (isMinimal) 8.dp else 16.dp,
                    shape = RoundedCornerShape(cornerDp),
                    ambientColor = primaryAccent,
                    spotColor = primaryAccent
                )
                .clip(RoundedCornerShape(cornerDp))
                .border(
                    width = when {
                        isNeon -> 2.dp
                        isVinyl -> 6.dp
                        isMinimal -> 0.75.dp
                        else -> 1.5.dp
                    },
                    brush = Brush.linearGradient(
                        listOf(
                            primaryAccent.copy(alpha = if (isNeon) 0.9f else 0.55f),
                            secondaryAccent.copy(alpha = if (isNeon) 0.75f else 0.35f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerDp)
                )
                .clickable(onClick = onChangeCover),
            contentAlignment = Alignment.Center
        ) {
            AlbumArtwork(
                albumArtUri = targetTrack.albumArtUri,
                contentDescription = "Portada del álbum ${targetTrack.album}",
                modifier = Modifier.fillMaxSize(),
                cornerRadius = cornerDp
            )
            if (isVinyl) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF14111D))
                        .border(2.dp, primaryAccent.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                    )
                }
            }
        }
    }

    if (animationsEnabled) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f, animationSpec = tween(260))) togetherWith
                    (fadeOut(tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)))
            },
            label = "track_art_transition"
        ) { animatedTrack ->
            renderArt(animatedTrack)
        }
    } else {
        renderArt(track)
    }
}

@Composable
private fun InlineSyncedLyricsCard(
    track: LocalTrack,
    lyrics: TrackLyrics?,
    positionMs: Long,
    isLoading: Boolean,
    accentColor: Color,
    textColor: Color,
    subtleColor: Color,
    isDarkSurface: Boolean,
    modifier: Modifier = Modifier,
    onSeekTo: (Long) -> Unit,
    onSearchOnline: () -> Unit,
    onOpenEditor: () -> Unit,
    onCloseLyrics: () -> Unit
) {
    val listState = rememberLazyListState()
    val activeIdx = remember(lyrics, positionMs) {
        if (lyrics != null) LyricsRepository.findActiveLineIndex(lyrics.lines, positionMs) else -1
    }
    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = (if (isDarkSurface) Color(0xFF141022) else Color.White).copy(alpha = 0.78f),
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            .testTag("player_inline_lyrics_panel")
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lyrics, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lyrics?.isSynced == true) "Letras sincronizadas" else "Letras · ${track.title}",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row {
                    IconButton(onClick = onOpenEditor, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Editar o importar letra", tint = subtleColor, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onCloseLyrics, modifier = Modifier.size(36.dp).testTag("inline_lyrics_close_btn")) {
                        Icon(Icons.Rounded.Close, contentDescription = "Volver a la portada", tint = textColor, modifier = Modifier.size(18.dp))
                    }
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }
                lyrics == null || lyrics.lines.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No hay letras disponibles para esta canción",
                            style = MaterialTheme.typography.titleSmall,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Búscala en fuentes autorizadas o importa un archivo .LRC para verla sincronizada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = subtleColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onSearchOnline,
                                modifier = Modifier.testTag("inline_lyrics_search_btn")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Buscar letra")
                            }
                            OutlinedButton(
                                onClick = onOpenEditor,
                                modifier = Modifier.testTag("inline_lyrics_editor_btn")
                            ) {
                                Text("Importar / Editar")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lyrics.lines) { idx, line ->
                            val isActive = idx == activeIdx
                            Text(
                                text = line.text.ifBlank { "♪" },
                                fontSize = if (isActive) 18.sp else 15.sp,
                                lineHeight = if (isActive) 25.sp else 21.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isActive -> accentColor
                                    lyrics.isSynced -> subtleColor.copy(alpha = 0.72f)
                                    else -> textColor
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (line.timestampMs != null) {
                                            Modifier.clickable { onSeekTo(line.timestampMs) }
                                        } else Modifier
                                    )
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackMetadataAndFavorite(
    track: LocalTrack,
    isFavorite: Boolean,
    buttonAccent: Color,
    textColor: Color,
    subtleColor: Color,
    currentLyrics: TrackLyrics?,
    positionMs: Long,
    showInlineLyricsBar: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenInlineLyrics: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("player_track_title")
                )
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = subtleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("player_track_artist")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Album,
                        contentDescription = null,
                        tint = subtleColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = track.album.ifBlank { "Álbum desconocido" },
                        style = MaterialTheme.typography.bodySmall,
                        color = subtleColor.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("player_track_album")
                    )
                }
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_favorite_button")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                    tint = if (isFavorite) Color(0xFFEF4444) else subtleColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showInlineLyricsBar) {
            val liveLine = remember(currentLyrics, positionMs) {
                if (currentLyrics != null && currentLyrics.lines.isNotEmpty()) {
                    val idx = LyricsRepository.findActiveLineIndex(currentLyrics.lines, positionMs)
                    if (idx >= 0) currentLyrics.lines[idx].text else currentLyrics.lines.firstOrNull { it.text.isNotBlank() }?.text
                } else null
            }
            if (!liveLine.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = buttonAccent.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenInlineLyrics)
                        .testTag("player_live_lyric_preview")
                ) {
                    Text(
                        text = "♪ $liveLine",
                        style = MaterialTheme.typography.bodySmall,
                        color = buttonAccent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekProgressAndTransport(
    playbackState: PlaybackUiState,
    animationsEnabled: Boolean,
    controlStyle: PlayerControlStyle = PlayerControlStyle.PILL,
    designerConfig: PlayerDesignerConfig = PlayerDesignerConfig.defaultConfig(),
    buttonAccent: Color,
    textColor: Color,
    subtleColor: Color,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    val duration = playbackState.durationMs.coerceAtLeast(1L)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }

    val sliderValue = if (isScrubbing) {
        scrubPosition
    } else {
        (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
    val displayedElapsedMs = if (isScrubbing) (scrubPosition * duration).toLong() else playbackState.positionMs
    val remainingMs = (playbackState.durationMs - displayedElapsedMs).coerceAtLeast(0L)

    val scaleFactor = designerConfig.buttonScale.coerceIn(0.8f, 1.25f)
    val playButtonScale by animateFloatAsState(
        targetValue = if (animationsEnabled && playbackState.isPlaying) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "play_btn_scale"
    )
    val contentOnButton = if (controlStyle == PlayerControlStyle.MINIMAL_OUTLINE) {
        buttonAccent
    } else if (buttonAccent.luminance() > 0.55f) {
        Color(0xFF100C18)
    } else {
        Color.White
    }
    val playShape = when (controlStyle) {
        PlayerControlStyle.PILL -> CircleShape
        PlayerControlStyle.MINIMAL_OUTLINE -> CircleShape
        PlayerControlStyle.CLASSIC_STUDIO -> RoundedCornerShape(18.dp)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (designerConfig.progressBarStyle == ProgressBarVisualStyle.WAVEFORM_BAR) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barCount = 28
                for (index in 0 until barCount) {
                    val ratio = index / (barCount - 1f)
                    val active = ratio <= sliderValue
                    val amp = 0.3f + (((index * 7 + 3) % 10) / 14f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(amp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (active) buttonAccent else subtleColor.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }

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
                thumbColor = if (designerConfig.progressBarStyle == ProgressBarVisualStyle.THIN_LINE) {
                    buttonAccent.copy(alpha = 0.85f)
                } else {
                    buttonAccent
                },
                activeTrackColor = buttonAccent,
                inactiveTrackColor = subtleColor.copy(
                    alpha = if (designerConfig.progressBarStyle == ProgressBarVisualStyle.THIN_LINE) 0.18f else 0.28f
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    when (designerConfig.progressBarStyle) {
                        ProgressBarVisualStyle.THIN_LINE -> 24.dp
                        ProgressBarVisualStyle.WAVEFORM_BAR -> 28.dp
                        ProgressBarVisualStyle.SLIDER_GLOW -> 36.dp
                    }
                )
                .testTag("player_seek_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDurationMs(displayedElapsedMs),
                style = MaterialTheme.typography.labelMedium,
                color = subtleColor,
                modifier = Modifier.testTag("player_elapsed_time")
            )
            Text(
                text = "-${formatDurationMs(remainingMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = subtleColor,
                modifier = Modifier.testTag("player_remaining_time")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val visiblePrimary = designerConfig.primaryControlsOrder
            .filterNot { it in designerConfig.hiddenActions }
            .ifEmpty { listOf(PlayerQuickAction.PLAY_PAUSE) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visiblePrimary.forEach { action ->
                when (action) {
                    PlayerQuickAction.SHUFFLE -> IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_shuffle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Modo aleatorio",
                            tint = if (playbackState.shuffleEnabled) buttonAccent else subtleColor,
                            modifier = Modifier.size((24f * scaleFactor).dp)
                        )
                    }
                    PlayerQuickAction.PREVIOUS -> IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .size((54f * scaleFactor).coerceAtLeast(48f).dp)
                            .testTag("player_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Canción anterior",
                            tint = textColor,
                            modifier = Modifier.size((36f * scaleFactor).dp)
                        )
                    }
                    PlayerQuickAction.PLAY_PAUSE -> FilledIconButton(
                        onClick = onTogglePlayPause,
                        shape = playShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (controlStyle == PlayerControlStyle.MINIMAL_OUTLINE) {
                                buttonAccent.copy(alpha = 0.14f)
                            } else {
                                buttonAccent
                            },
                            contentColor = contentOnButton
                        ),
                        modifier = Modifier
                            .size((72f * scaleFactor).coerceIn(56f, 88f).dp)
                            .graphicsLayer {
                                scaleX = playButtonScale
                                scaleY = playButtonScale
                            }
                            .then(
                                if (controlStyle == PlayerControlStyle.MINIMAL_OUTLINE) {
                                    Modifier.border(2.dp, buttonAccent, playShape)
                                } else Modifier
                            )
                            .testTag("player_sheet_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size((38f * scaleFactor).coerceIn(30f, 48f).dp)
                        )
                    }
                    PlayerQuickAction.NEXT -> IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size((54f * scaleFactor).coerceAtLeast(48f).dp)
                            .testTag("player_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Siguiente canción",
                            tint = textColor,
                            modifier = Modifier.size((36f * scaleFactor).dp)
                        )
                    }
                    PlayerQuickAction.REPEAT -> IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("player_repeat_button")
                    ) {
                        val repeatIcon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Rounded.RepeatOne
                        } else {
                            Icons.Rounded.Repeat
                        }
                        val repeatDesc = when (playbackState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repetir una canción"
                            Player.REPEAT_MODE_ALL -> "Repetir toda la lista"
                            else -> "Repetición desactivada"
                        }
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = repeatDesc,
                            tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) buttonAccent else subtleColor,
                            modifier = Modifier.size((24f * scaleFactor).dp)
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun NowPlayingActionDock(
    queueCount: Int,
    showLyricsActive: Boolean,
    sleepTimerActive: Boolean,
    eqEnabled: Boolean,
    handsFreeVoiceActive: Boolean,
    controlStyle: PlayerControlStyle = PlayerControlStyle.PILL,
    designerConfig: PlayerDesignerConfig = PlayerDesignerConfig.defaultConfig(),
    accentColor: Color,
    subtleColor: Color,
    isDarkSurface: Boolean,
    onOpenQueue: () -> Unit,
    onOpenEqualizerSheet: () -> Unit,
    onToggleLyrics: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenVoiceControl: () -> Unit
) {
    val visibleTools = remember(designerConfig) {
        listOf(
            PlayerQuickAction.QUEUE,
            PlayerQuickAction.LYRICS,
            PlayerQuickAction.SLEEP_TIMER,
            PlayerQuickAction.VOICE
        ).filterNot { it in designerConfig.hiddenActions }
    }
    val dockShape = when (controlStyle) {
        PlayerControlStyle.PILL -> RoundedCornerShape(24.dp)
        PlayerControlStyle.MINIMAL_OUTLINE -> RoundedCornerShape(16.dp)
        PlayerControlStyle.CLASSIC_STUDIO -> RoundedCornerShape(12.dp)
    }
    Surface(
        shape = dockShape,
        color = (if (isDarkSurface) Color.White else Color.Black).copy(
            alpha = if (controlStyle == PlayerControlStyle.MINIMAL_OUTLINE) 0.02f else 0.06f
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (controlStyle == PlayerControlStyle.MINIMAL_OUTLINE) {
                    Modifier.border(1.dp, accentColor.copy(alpha = 0.28f), dockShape)
                } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (PlayerQuickAction.QUEUE in visibleTools) {
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_queue_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Cola de reproducción ($queueCount)",
                        tint = subtleColor
                    )
                }
            }
            IconButton(
                onClick = onOpenEqualizerSheet,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("player_open_eq_button")
            ) {
                Icon(
                    Icons.Rounded.Equalizer,
                    contentDescription = "Ecualizador",
                    tint = if (eqEnabled) accentColor else subtleColor
                )
            }
            if (PlayerQuickAction.LYRICS in visibleTools) {
                IconButton(
                    onClick = onToggleLyrics,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_lyrics_button")
                ) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Letras de canción",
                        tint = if (showLyricsActive) accentColor else subtleColor
                    )
                }
            }
            if (PlayerQuickAction.SLEEP_TIMER in visibleTools) {
                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_sleep_timer_button")
                ) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = "Temporizador de sueño",
                        tint = if (sleepTimerActive) accentColor else subtleColor
                    )
                }
            }
            if (PlayerQuickAction.VOICE in visibleTools) {
                IconButton(
                    onClick = onOpenVoiceControl,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_voice_button")
                ) {
                    Icon(
                        Icons.Rounded.Mic,
                        contentDescription = "Control por voz",
                        tint = if (handsFreeVoiceActive) accentColor else subtleColor
                    )
                }
            }
        }
    }
}

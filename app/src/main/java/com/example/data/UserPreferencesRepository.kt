package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppAccentPalette
import com.example.model.AppFontStyle
import com.example.model.AppLanguage
import com.example.model.AppSettingsPrefs
import com.example.model.AudioChannelMode
import com.example.model.AudioDeviceProfileType
import com.example.model.ChatRegion
import com.example.model.CrossfadeCurve
import com.example.model.CustomPlaylist
import com.example.model.CustomThemeConfig
import com.example.model.DeviceSoundProfile
import com.example.model.DjTransitionStyle
import com.example.model.HearingProfile
import com.example.model.HomeLayoutConfig
import com.example.model.HomeLayoutPreset
import com.example.model.LocalTrack
import com.example.model.PlaybackEnginePrefs
import com.example.model.PlaybackProgress
import com.example.model.PlayerBackgroundMode
import com.example.model.PlayerControlStyle
import com.example.model.PlayerLayoutMode
import com.example.model.PlayerVisualPrefs
import com.example.model.PlayerVisualStyle
import com.example.model.SavedAmbientMix
import com.example.model.SavedCustomEqProfile
import com.example.model.SavedQueuePreset
import com.example.model.Spatial3dPreset
import com.example.model.StudioAudioMode
import com.example.model.TimeOfDaySlot
import com.example.model.UiStyleMode
import com.example.model.UserAppearancePrefs
import com.example.model.VisualizerStyle
import com.example.recognition.SongRecognitionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.userSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ritmolocal_settings")

data class EqualizerStoredPrefs(
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val trebleBoostMb: Int = 0,
    val spatial3dEnabled: Boolean = false,
    val spatial3dPreset: Spatial3dPreset = Spatial3dPreset.MUSIC_3D,
    val spatial3dStrength: Int = 650,
    val clarityBoostMb: Int = 220,
    val hearingProfile: HearingProfile = HearingProfile.STANDARD,
    val audioChannelMode: AudioChannelMode = AudioChannelMode.STEREO,
    val balanceLR: Float = 0f, // -1.0 (Full Left) .. 0.0 (Center) .. +1.0 (Full Right)
    val hiResMasteringEnabled: Boolean = true,
    val studioAudioMode: StudioAudioMode = StudioAudioMode.HIFI_MASTER,
    val dynamicPunchStrength: Int = 550,
    val antiClipLimiterEnabled: Boolean = true,
    val autoSwitchDeviceProfile: Boolean = true
)

class UserPreferencesRepository(context: Context) {

    private val dataStore = context.applicationContext.userSettingsDataStore

    private val darkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val accentPaletteKey = stringPreferencesKey("accent_palette_id")
    private val uiStyleModeKey = stringPreferencesKey("ui_style_mode")
    private val fontStyleKey = stringPreferencesKey("app_font_style")
    private val wallpaperUriKey = stringPreferencesKey("wallpaper_uri")
    private val wallpaperDimAlphaKey = floatPreferencesKey("wallpaper_dim_alpha")
    private val wallpaperBlurDpKey = floatPreferencesKey("wallpaper_blur_dp")
    private val fontSizeScaleKey = floatPreferencesKey("font_size_scale")
    private val activeCustomThemeIdKey = stringPreferencesKey("active_custom_theme_id")
    private val useDynamicMaterialYouKey = booleanPreferencesKey("use_dynamic_material_you")
    private val customThemesJsonKey = stringPreferencesKey("custom_themes_json")
    private val homeLayoutConfigJsonKey = stringPreferencesKey("home_layout_config_json")
    private val homeLayoutPresetsJsonKey = stringPreferencesKey("home_layout_presets_json")
    private val v22DesignInitializedKey = booleanPreferencesKey("v22_design_initialized")

    // Equalizer, Dolby Atmos 3D, Hearing Adaptation, Audio Channel
    private val eqEnabledKey = booleanPreferencesKey("eq_enabled")
    private val eqPresetIndexKey = intPreferencesKey("eq_preset_index")
    private val eqBandsCsvKey = stringPreferencesKey("eq_bands_csv")
    private val eqBassBoostKey = intPreferencesKey("eq_bass_boost")
    private val eqTrebleBoostKey = intPreferencesKey("eq_treble_boost_mb")
    private val spatial3dEnabledKey = booleanPreferencesKey("spatial_3d_enabled")
    private val spatial3dPresetKey = stringPreferencesKey("spatial_3d_preset")
    private val spatial3dStrengthKey = intPreferencesKey("spatial_3d_strength")
    private val clarityBoostMbKey = intPreferencesKey("clarity_boost_mb")
    private val hearingProfileKey = stringPreferencesKey("hearing_profile")
    private val audioChannelModeKey = stringPreferencesKey("audio_channel_mode")
    private val balanceLRKey = floatPreferencesKey("balance_lr")
    private val hiResMasteringEnabledKey = booleanPreferencesKey("hires_mastering_enabled")
    private val studioAudioModeKey = stringPreferencesKey("studio_audio_mode")
    private val dynamicPunchStrengthKey = intPreferencesKey("dynamic_punch_strength")
    private val antiClipLimiterKey = booleanPreferencesKey("anti_clip_limiter_enabled")
    private val autoSwitchDeviceProfileKey = booleanPreferencesKey("eq_auto_switch_device_profile")
    private val deviceSoundProfilesJsonKey = stringPreferencesKey("eq_device_sound_profiles_json")

    // General App Settings
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val voiceLanguageKey = stringPreferencesKey("voice_language_code")
    private val handsFreeVoiceKey = booleanPreferencesKey("hands_free_voice_enabled")
    private val screenOffVoiceKey = booleanPreferencesKey("screen_off_voice_enabled")
    private val animationsEnabledKey = booleanPreferencesKey("animations_enabled")
    private val hideShortAudioKey = booleanPreferencesKey("hide_short_whatsapp_audio")
    private val autoDownloadArtworkKey = booleanPreferencesKey("auto_download_artwork")
    private val playAlongOtherAppsKey = booleanPreferencesKey("play_along_other_apps")
    private val notifShowArtworkKey = booleanPreferencesKey("notif_show_artwork")
    private val notifShowFavoriteKey = booleanPreferencesKey("notif_show_favorite")
    private val notifShowModeToggleKey = booleanPreferencesKey("notif_show_mode_toggle")
    private val scheduledMixRemindersKey = booleanPreferencesKey("scheduled_mix_reminders")
    private val chatPushNotificationsKey = booleanPreferencesKey("chat_push_notifications_enabled")
    private val chatPushOnlyActiveRegionKey = booleanPreferencesKey("chat_push_only_active_region")
    private val cachedFcmTokenKey = stringPreferencesKey("cached_fcm_token")
    private val savedShuffleKey = booleanPreferencesKey("saved_shuffle_enabled")
    private val savedRepeatModeKey = intPreferencesKey("saved_repeat_mode")
    private val guestModeKey = booleanPreferencesKey("guest_mode_enabled")
    private val preferredChatRegionKey = stringPreferencesKey("preferred_chat_region")
    private val customChatUsernameKey = stringPreferencesKey("custom_chat_username")
    private val chatAvatarUriKey = stringPreferencesKey("chat_avatar_uri")
    private val chatAvatarBase64Key = stringPreferencesKey("chat_avatar_base64")
    private val diagnosticModeKey = booleanPreferencesKey("diagnostic_mode_enabled")

    // Custom Playlists & Cover Overrides
    private val customPlaylistsJsonKey = stringPreferencesKey("custom_playlists_json")
    private val customCoverMapJsonKey = stringPreferencesKey("custom_cover_map_json")
    private val customEqProfilesJsonKey = stringPreferencesKey("custom_eq_profiles_json")

    // Advanced Now Playing Visual Customization
    private val playerStyleKey = stringPreferencesKey("player_visual_style")
    private val playerBackgroundModeKey = stringPreferencesKey("player_background_mode")
    private val playerPrimaryColorArgbKey = longPreferencesKey("player_primary_color_argb")
    private val playerButtonColorArgbKey = longPreferencesKey("player_button_color_argb")
    private val playerShowVisualizerKey = booleanPreferencesKey("player_show_visualizer")
    private val playerShowInlineLyricsKey = booleanPreferencesKey("player_show_inline_lyrics")
    private val playerVisualizerStyleKey = stringPreferencesKey("player_visualizer_style")
    private val playerVisualizerIntensityKey = floatPreferencesKey("player_visualizer_intensity")
    private val playerBlurIntensityKey = floatPreferencesKey("player_blur_intensity")
    private val playerControlStyleKey = stringPreferencesKey("player_control_style")
    private val playerLayoutModeKey = stringPreferencesKey("player_layout_mode")

    // Advanced Playback Engine (Crossfade, Fades, Smart DJ, Queue Persistence, Sleep Timer)
    private val playbackEngineJsonKey = stringPreferencesKey("playback_engine_prefs_json")
    private val savedQueuePresetsJsonKey = stringPreferencesKey("saved_queue_presets_json")
    private val activeQueueSnapshotJsonKey = stringPreferencesKey("active_queue_snapshot_json")
    private val sleepTimerFadeOutSecKey = intPreferencesKey("sleep_timer_fade_out_seconds")
    private val sleepTimerPresetsCsvKey = stringPreferencesKey("sleep_timer_presets_csv")

    // Offline / Local fallback tracking when user skips sign-in
    private val offlineFavoriteKeysKey = stringSetPreferencesKey("offline_favorite_keys")
    private val offlineTotalSecondsKey = longPreferencesKey("offline_total_seconds")
    private val offlineLastTrackJsonKey = stringPreferencesKey("offline_last_track_json")

    // Stage 4: Ambient mixes, song recognition history
    private val savedAmbientMixesJsonKey = stringPreferencesKey("saved_ambient_mixes_json")
    private val songRecognitionHistoryJsonKey = stringPreferencesKey("song_recognition_history_json")

    val appearancePrefs: Flow<UserAppearancePrefs> = dataStore.data.map { prefs ->
        UserAppearancePrefs(
            isDarkTheme = prefs[darkThemeKey] ?: true,
            accentPalette = AppAccentPalette.fromId(prefs[accentPaletteKey]),
            uiStyleMode = UiStyleMode.fromId(prefs[uiStyleModeKey]),
            fontStyle = AppFontStyle.fromId(prefs[fontStyleKey]),
            wallpaperUri = prefs[wallpaperUriKey]?.takeIf { it.isNotBlank() },
            wallpaperDimAlpha = (prefs[wallpaperDimAlphaKey] ?: 0.62f).coerceIn(0.15f, 0.92f),
            wallpaperBlurDp = (prefs[wallpaperBlurDpKey] ?: 0f).coerceIn(0f, 24f),
            fontSizeScale = (prefs[fontSizeScaleKey] ?: 1.0f).coerceIn(0.85f, 1.25f),
            activeCustomThemeId = prefs[activeCustomThemeIdKey]?.takeIf { it.isNotBlank() },
            useDynamicMaterialYou = prefs[useDynamicMaterialYouKey] ?: false
        )
    }

    val customThemes: Flow<List<CustomThemeConfig>> = dataStore.data.map { prefs ->
        CustomThemeConfig.decodeList(prefs[customThemesJsonKey].orEmpty())
    }

    val homeLayoutConfig: Flow<HomeLayoutConfig> = dataStore.data.map { prefs ->
        HomeLayoutConfig.decode(prefs[homeLayoutConfigJsonKey].orEmpty())
    }

    val homeLayoutPresets: Flow<List<HomeLayoutPreset>> = dataStore.data.map { prefs ->
        HomeLayoutPreset.decodeList(prefs[homeLayoutPresetsJsonKey].orEmpty())
    }

    val equalizerPrefs: Flow<EqualizerStoredPrefs> = dataStore.data.map { prefs ->
        val rawBands = prefs[eqBandsCsvKey]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(0, 0, 0, 0, 0)
        EqualizerStoredPrefs(
            enabled = prefs[eqEnabledKey] ?: true,
            presetIndex = prefs[eqPresetIndexKey] ?: 0,
            bandLevelsMb = rawBands,
            bassBoostStrength = (prefs[eqBassBoostKey] ?: 0).coerceIn(0, 1000),
            trebleBoostMb = (prefs[eqTrebleBoostKey] ?: 0).coerceIn(-900, 900),
            spatial3dEnabled = prefs[spatial3dEnabledKey] ?: false,
            spatial3dPreset = Spatial3dPreset.fromId(prefs[spatial3dPresetKey]),
            spatial3dStrength = (prefs[spatial3dStrengthKey] ?: 650).coerceIn(0, 1000),
            clarityBoostMb = (prefs[clarityBoostMbKey] ?: 220).coerceIn(0, 1000),
            hearingProfile = HearingProfile.fromId(prefs[hearingProfileKey]),
            audioChannelMode = AudioChannelMode.fromId(prefs[audioChannelModeKey]),
            balanceLR = (prefs[balanceLRKey] ?: 0f).coerceIn(-1f, 1f),
            hiResMasteringEnabled = prefs[hiResMasteringEnabledKey] ?: true,
            studioAudioMode = StudioAudioMode.fromId(prefs[studioAudioModeKey]),
            dynamicPunchStrength = (prefs[dynamicPunchStrengthKey] ?: 550).coerceIn(0, 1000),
            antiClipLimiterEnabled = prefs[antiClipLimiterKey] ?: true,
            autoSwitchDeviceProfile = prefs[autoSwitchDeviceProfileKey] ?: true
        )
    }

    val deviceSoundProfiles: Flow<Map<AudioDeviceProfileType, DeviceSoundProfile>> = dataStore.data.map { prefs ->
        decodeDeviceSoundProfiles(prefs[deviceSoundProfilesJsonKey].orEmpty())
    }

    val appSettingsPrefs: Flow<AppSettingsPrefs> = dataStore.data.map { prefs ->
        AppSettingsPrefs(
            appLanguage = AppLanguage.fromCode(prefs[appLanguageKey]),
            voiceLanguageCode = prefs[voiceLanguageKey].orEmpty(),
            handsFreeVoiceEnabled = prefs[handsFreeVoiceKey] ?: false,
            screenOffVoiceEnabled = prefs[screenOffVoiceKey] ?: true,
            animationsEnabled = prefs[animationsEnabledKey] ?: true,
            hideShortWhatsAppAudio = prefs[hideShortAudioKey] ?: false,
            autoDownloadArtwork = prefs[autoDownloadArtworkKey] ?: false,
            playAlongOtherApps = prefs[playAlongOtherAppsKey] ?: false,
            notifShowArtwork = prefs[notifShowArtworkKey] ?: true,
            notifShowFavorite = prefs[notifShowFavoriteKey] ?: true,
            notifShowModeToggle = prefs[notifShowModeToggleKey] ?: true,
            scheduledMixRemindersEnabled = prefs[scheduledMixRemindersKey] ?: false,
            chatPushNotificationsEnabled = prefs[chatPushNotificationsKey] ?: true,
            chatPushOnlyActiveRegion = prefs[chatPushOnlyActiveRegionKey] ?: true,
            savedShuffleEnabled = prefs[savedShuffleKey] ?: false,
            savedRepeatMode = (prefs[savedRepeatModeKey] ?: 0).coerceIn(0, 2),
            guestModeEnabled = prefs[guestModeKey] ?: false,
            preferredChatRegion = ChatRegion.fromId(prefs[preferredChatRegionKey]),
            customChatUsername = prefs[customChatUsernameKey].orEmpty(),
            chatAvatarUri = prefs[chatAvatarUriKey]?.takeIf { it.isNotBlank() },
            chatAvatarBase64 = prefs[chatAvatarBase64Key].orEmpty(),
            diagnosticModeEnabled = prefs[diagnosticModeKey] ?: false,
            sleepTimerFadeOutSeconds = (prefs[sleepTimerFadeOutSecKey] ?: 3).coerceIn(1, 15)
        )
    }

    val cachedFcmToken: Flow<String> = dataStore.data.map { prefs ->
        prefs[cachedFcmTokenKey].orEmpty()
    }

    val customPlaylists: Flow<List<CustomPlaylist>> = dataStore.data.map { prefs ->
        decodeCustomPlaylists(prefs[customPlaylistsJsonKey].orEmpty())
    }

    val customCoverMap: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        decodeCoverMap(prefs[customCoverMapJsonKey].orEmpty())
    }

    val playerVisualPrefs: Flow<PlayerVisualPrefs> = dataStore.data.map { prefs ->
        PlayerVisualPrefs(
            style = PlayerVisualStyle.fromId(prefs[playerStyleKey]),
            backgroundMode = PlayerBackgroundMode.fromId(prefs[playerBackgroundModeKey]),
            customPrimaryColorArgb = prefs[playerPrimaryColorArgbKey] ?: 0L,
            customButtonColorArgb = prefs[playerButtonColorArgbKey] ?: 0L,
            showVisualizer = prefs[playerShowVisualizerKey] ?: true,
            showInlineLyricsBar = prefs[playerShowInlineLyricsKey] ?: true,
            visualizerStyle = VisualizerStyle.fromId(prefs[playerVisualizerStyleKey]),
            visualizerIntensity = (prefs[playerVisualizerIntensityKey] ?: 1.0f).coerceIn(0.35f, 1.5f),
            blurIntensity = (prefs[playerBlurIntensityKey] ?: 0.65f).coerceIn(0f, 1f),
            controlStyle = PlayerControlStyle.fromId(prefs[playerControlStyleKey]),
            layoutMode = PlayerLayoutMode.fromId(prefs[playerLayoutModeKey])
        )
    }

    val playbackEnginePrefs: Flow<PlaybackEnginePrefs> = dataStore.data.map { prefs ->
        decodePlaybackEnginePrefs(prefs[playbackEngineJsonKey].orEmpty())
    }

    val savedQueuePresets: Flow<List<SavedQueuePreset>> = dataStore.data.map { prefs ->
        decodeQueuePresets(prefs[savedQueuePresetsJsonKey].orEmpty())
    }

    val activeQueueSnapshot: Flow<Triple<List<String>, Int, Long>?> = dataStore.data.map { prefs ->
        decodeActiveQueueSnapshot(prefs[activeQueueSnapshotJsonKey].orEmpty())
    }

    val sleepTimerCustomPresets: Flow<List<Int>> = dataStore.data.map { prefs ->
        prefs[sleepTimerPresetsCsvKey]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull()?.takeIf { m -> m in 1..180 } }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: listOf(5, 10, 15, 30, 45, 60)
    }

    val savedCustomEqProfiles: Flow<List<SavedCustomEqProfile>> = dataStore.data.map { prefs ->
        decodeCustomEqProfiles(prefs[customEqProfilesJsonKey].orEmpty())
    }

    val offlineFavoriteKeys: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[offlineFavoriteKeysKey].orEmpty()
    }

    val offlineTotalSeconds: Flow<Long> = dataStore.data.map { prefs ->
        (prefs[offlineTotalSecondsKey] ?: 0L).coerceAtLeast(0L)
    }

    val offlineLastProgress: Flow<PlaybackProgress?> = dataStore.data.map { prefs ->
        decodeOfflineProgress(prefs[offlineLastTrackJsonKey], prefs[offlineTotalSecondsKey] ?: 0L)
    }

    val savedAmbientMixes: Flow<List<SavedAmbientMix>> = dataStore.data.map { prefs ->
        SavedAmbientMix.decodeList(prefs[savedAmbientMixesJsonKey].orEmpty())
    }

    val songRecognitionHistory: Flow<List<SongRecognitionRecord>> = dataStore.data.map { prefs ->
        SongRecognitionRecord.decodeList(prefs[songRecognitionHistoryJsonKey].orEmpty())
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { it[darkThemeKey] = isDark }
    }

    suspend fun setAccentPalette(palette: AppAccentPalette) {
        dataStore.edit { it[accentPaletteKey] = palette.id }
    }

    suspend fun setUiStyleMode(mode: UiStyleMode) {
        dataStore.edit { it[uiStyleModeKey] = mode.id }
    }

    suspend fun setFontStyle(style: AppFontStyle) {
        dataStore.edit { it[fontStyleKey] = style.id }
    }

    suspend fun ensureV22DefaultDesignIfNeeded() {
        dataStore.edit { prefs ->
            if (prefs[v22DesignInitializedKey] != true) {
                prefs[v22DesignInitializedKey] = true
                prefs[uiStyleModeKey] = UiStyleMode.NEON_MODERN.id
                prefs[accentPaletteKey] = AppAccentPalette.NEON_VIOLET.id
                prefs[darkThemeKey] = true
            }
        }
    }

    suspend fun setWallpaperUri(uri: String?) {
        dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(wallpaperUriKey) else it[wallpaperUriKey] = uri
        }
    }

    suspend fun setWallpaperDimAlpha(alpha: Float) {
        dataStore.edit { it[wallpaperDimAlphaKey] = alpha.coerceIn(0.15f, 0.92f) }
    }

    suspend fun setWallpaperBlurDp(blurDp: Float) {
        dataStore.edit { it[wallpaperBlurDpKey] = blurDp.coerceIn(0f, 24f) }
    }

    suspend fun setFontSizeScale(scale: Float) {
        dataStore.edit { it[fontSizeScaleKey] = scale.coerceIn(0.85f, 1.25f) }
    }

    suspend fun setActiveCustomThemeId(themeId: String?) {
        dataStore.edit {
            if (themeId.isNullOrBlank()) it.remove(activeCustomThemeIdKey) else it[activeCustomThemeIdKey] = themeId
        }
    }

    suspend fun setUseDynamicMaterialYou(enabled: Boolean) {
        dataStore.edit {
            it[useDynamicMaterialYouKey] = enabled
            if (enabled) it.remove(activeCustomThemeIdKey)
        }
    }

    suspend fun saveCustomTheme(theme: CustomThemeConfig, setAsActive: Boolean = true) {
        dataStore.edit { prefs ->
            val current = CustomThemeConfig.decodeList(prefs[customThemesJsonKey].orEmpty())
                .filterNot { it.id == theme.id || it.name.equals(theme.name, ignoreCase = true) }
            prefs[customThemesJsonKey] = CustomThemeConfig.encodeList((listOf(theme) + current).take(24))
            if (setAsActive) {
                prefs[activeCustomThemeIdKey] = theme.id
                prefs[useDynamicMaterialYouKey] = false
                prefs[darkThemeKey] = theme.isDark
            }
        }
    }

    suspend fun deleteCustomTheme(themeId: String) {
        dataStore.edit { prefs ->
            val filtered = CustomThemeConfig.decodeList(prefs[customThemesJsonKey].orEmpty())
                .filterNot { it.id == themeId }
            prefs[customThemesJsonKey] = CustomThemeConfig.encodeList(filtered)
            if (prefs[activeCustomThemeIdKey] == themeId) {
                prefs.remove(activeCustomThemeIdKey)
            }
        }
    }

    suspend fun saveHomeLayoutConfig(config: HomeLayoutConfig) {
        dataStore.edit { it[homeLayoutConfigJsonKey] = HomeLayoutConfig.encode(config) }
    }

    suspend fun saveHomeLayoutPreset(preset: HomeLayoutPreset) {
        dataStore.edit { prefs ->
            val current = HomeLayoutPreset.decodeList(prefs[homeLayoutPresetsJsonKey].orEmpty())
                .filterNot { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
            prefs[homeLayoutPresetsJsonKey] = HomeLayoutPreset.encodeList((listOf(preset) + current).take(16))
        }
    }

    suspend fun deleteHomeLayoutPreset(presetId: String) {
        dataStore.edit { prefs ->
            val filtered = HomeLayoutPreset.decodeList(prefs[homeLayoutPresetsJsonKey].orEmpty())
                .filterNot { it.id == presetId }
            prefs[homeLayoutPresetsJsonKey] = HomeLayoutPreset.encodeList(filtered)
        }
    }

    suspend fun restoreOriginalAppearanceDefaults() {
        dataStore.edit { prefs ->
            prefs[darkThemeKey] = true
            prefs[accentPaletteKey] = AppAccentPalette.NEON_VIOLET.id
            prefs[uiStyleModeKey] = UiStyleMode.NEON_MODERN.id
            prefs[fontStyleKey] = AppFontStyle.SPACE_JAKARTA.id
            prefs[fontSizeScaleKey] = 1.0f
            prefs[wallpaperBlurDpKey] = 0f
            prefs[wallpaperDimAlphaKey] = 0.62f
            prefs[useDynamicMaterialYouKey] = false
            prefs.remove(activeCustomThemeIdKey)
            prefs.remove(wallpaperUriKey)
        }
    }

    suspend fun restoreOriginalPlayerVisualDefaults() {
        savePlayerVisualPrefs(PlayerVisualPrefs())
    }

    suspend fun saveEqualizerPrefs(prefsModel: EqualizerStoredPrefs) {
        dataStore.edit { prefs ->
            prefs[eqEnabledKey] = prefsModel.enabled
            prefs[eqPresetIndexKey] = prefsModel.presetIndex
            prefs[eqBandsCsvKey] = prefsModel.bandLevelsMb.joinToString(",")
            prefs[eqBassBoostKey] = prefsModel.bassBoostStrength.coerceIn(0, 1000)
            prefs[eqTrebleBoostKey] = prefsModel.trebleBoostMb.coerceIn(-900, 900)
            prefs[spatial3dEnabledKey] = prefsModel.spatial3dEnabled
            prefs[spatial3dPresetKey] = prefsModel.spatial3dPreset.id
            prefs[spatial3dStrengthKey] = prefsModel.spatial3dStrength.coerceIn(0, 1000)
            prefs[clarityBoostMbKey] = prefsModel.clarityBoostMb.coerceIn(0, 1000)
            prefs[hearingProfileKey] = prefsModel.hearingProfile.id
            prefs[audioChannelModeKey] = prefsModel.audioChannelMode.id
            prefs[balanceLRKey] = prefsModel.balanceLR.coerceIn(-1f, 1f)
            prefs[hiResMasteringEnabledKey] = prefsModel.hiResMasteringEnabled
            prefs[studioAudioModeKey] = prefsModel.studioAudioMode.id
            prefs[dynamicPunchStrengthKey] = prefsModel.dynamicPunchStrength.coerceIn(0, 1000)
            prefs[antiClipLimiterKey] = prefsModel.antiClipLimiterEnabled
            prefs[autoSwitchDeviceProfileKey] = prefsModel.autoSwitchDeviceProfile
        }
    }

    suspend fun saveDeviceSoundProfile(profile: DeviceSoundProfile) {
        dataStore.edit { prefs ->
            val current = decodeDeviceSoundProfiles(prefs[deviceSoundProfilesJsonKey].orEmpty()).toMutableMap()
            current[profile.deviceType] = profile
            prefs[deviceSoundProfilesJsonKey] = encodeDeviceSoundProfiles(current)
        }
    }

    suspend fun deleteDeviceSoundProfile(deviceType: AudioDeviceProfileType) {
        dataStore.edit { prefs ->
            val current = decodeDeviceSoundProfiles(prefs[deviceSoundProfilesJsonKey].orEmpty()).toMutableMap()
            current.remove(deviceType)
            prefs[deviceSoundProfilesJsonKey] = encodeDeviceSoundProfiles(current)
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[appLanguageKey] = language.code }
    }

    suspend fun setVoiceLanguageCode(code: String) {
        dataStore.edit { it[voiceLanguageKey] = code }
    }

    suspend fun setHandsFreeVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[handsFreeVoiceKey] = enabled }
    }

    suspend fun setScreenOffVoiceEnabled(enabled: Boolean) {
        dataStore.edit { it[screenOffVoiceKey] = enabled }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { it[animationsEnabledKey] = enabled }
    }

    suspend fun setHideShortWhatsAppAudio(hide: Boolean) {
        dataStore.edit { it[hideShortAudioKey] = hide }
    }

    suspend fun setAutoDownloadArtwork(enabled: Boolean) {
        dataStore.edit { it[autoDownloadArtworkKey] = enabled }
    }

    suspend fun setPlayAlongOtherApps(enabled: Boolean) {
        dataStore.edit { it[playAlongOtherAppsKey] = enabled }
    }

    suspend fun setNotificationOptions(
        showArtwork: Boolean,
        showFavorite: Boolean,
        showModeToggle: Boolean
    ) {
        dataStore.edit {
            it[notifShowArtworkKey] = showArtwork
            it[notifShowFavoriteKey] = showFavorite
            it[notifShowModeToggleKey] = showModeToggle
        }
    }

    suspend fun setScheduledMixReminders(enabled: Boolean) {
        dataStore.edit { it[scheduledMixRemindersKey] = enabled }
    }

    suspend fun setChatPushNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[chatPushNotificationsKey] = enabled }
    }

    suspend fun setChatPushOnlyActiveRegion(onlyActive: Boolean) {
        dataStore.edit { it[chatPushOnlyActiveRegionKey] = onlyActive }
    }

    suspend fun setCachedFcmToken(token: String) {
        dataStore.edit {
            if (token.isBlank()) it.remove(cachedFcmTokenKey) else it[cachedFcmTokenKey] = token.trim()
        }
    }

    suspend fun savePlaybackModes(shuffleEnabled: Boolean, repeatMode: Int) {
        dataStore.edit {
            it[savedShuffleKey] = shuffleEnabled
            it[savedRepeatModeKey] = repeatMode.coerceIn(0, 2)
        }
    }

    suspend fun setGuestModeEnabled(enabled: Boolean) {
        dataStore.edit { it[guestModeKey] = enabled }
    }

    suspend fun setPreferredChatRegion(region: ChatRegion) {
        dataStore.edit { it[preferredChatRegionKey] = region.id }
    }

    suspend fun setCustomChatUsername(username: String) {
        dataStore.edit { it[customChatUsernameKey] = username.trim().take(40) }
    }

    suspend fun setChatAvatar(uri: String?, base64: String) {
        dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(chatAvatarUriKey) else it[chatAvatarUriKey] = uri
            if (base64.isBlank()) it.remove(chatAvatarBase64Key) else it[chatAvatarBase64Key] = base64.take(35_000)
        }
    }

    suspend fun setDiagnosticModeEnabled(enabled: Boolean) {
        dataStore.edit { it[diagnosticModeKey] = enabled }
    }

    suspend fun saveCustomPlaylists(playlists: List<CustomPlaylist>) {
        val json = encodeCustomPlaylists(playlists)
        dataStore.edit { it[customPlaylistsJsonKey] = json }
    }

    suspend fun setCustomCoverForKey(key: String, localUri: String) {
        dataStore.edit { prefs ->
            val current = decodeCoverMap(prefs[customCoverMapJsonKey].orEmpty()).toMutableMap()
            current[key] = localUri
            prefs[customCoverMapJsonKey] = encodeCoverMap(current)
        }
    }

    suspend fun savePlayerVisualPrefs(visualPrefs: PlayerVisualPrefs) {
        dataStore.edit { prefs ->
            prefs[playerStyleKey] = visualPrefs.style.id
            prefs[playerBackgroundModeKey] = visualPrefs.backgroundMode.id
            prefs[playerPrimaryColorArgbKey] = visualPrefs.customPrimaryColorArgb
            prefs[playerButtonColorArgbKey] = visualPrefs.customButtonColorArgb
            prefs[playerShowVisualizerKey] = visualPrefs.showVisualizer
            prefs[playerShowInlineLyricsKey] = visualPrefs.showInlineLyricsBar
            prefs[playerVisualizerStyleKey] = visualPrefs.visualizerStyle.id
            prefs[playerVisualizerIntensityKey] = visualPrefs.visualizerIntensity.coerceIn(0.35f, 1.5f)
            prefs[playerBlurIntensityKey] = visualPrefs.blurIntensity.coerceIn(0f, 1f)
            prefs[playerControlStyleKey] = visualPrefs.controlStyle.id
            prefs[playerLayoutModeKey] = visualPrefs.layoutMode.id
        }
    }

    suspend fun savePlaybackEnginePrefs(enginePrefs: PlaybackEnginePrefs) {
        val json = encodePlaybackEnginePrefs(enginePrefs)
        dataStore.edit { it[playbackEngineJsonKey] = json }
    }

    suspend fun saveQueuePreset(preset: SavedQueuePreset) {
        dataStore.edit { prefs ->
            val current = decodeQueuePresets(prefs[savedQueuePresetsJsonKey].orEmpty())
                .filterNot { it.id == preset.id || it.name.equals(preset.name, ignoreCase = true) }
            prefs[savedQueuePresetsJsonKey] = encodeQueuePresets((listOf(preset) + current).take(20))
        }
    }

    suspend fun deleteQueuePreset(presetId: String) {
        dataStore.edit { prefs ->
            val filtered = decodeQueuePresets(prefs[savedQueuePresetsJsonKey].orEmpty())
                .filterNot { it.id == presetId }
            prefs[savedQueuePresetsJsonKey] = encodeQueuePresets(filtered)
        }
    }

    suspend fun saveActiveQueueSnapshot(trackKeys: List<String>, index: Int, positionMs: Long) {
        if (trackKeys.isEmpty()) return
        val json = encodeActiveQueueSnapshot(trackKeys.take(250), index.coerceAtLeast(0), positionMs.coerceAtLeast(0L))
        dataStore.edit { it[activeQueueSnapshotJsonKey] = json }
    }

    suspend fun setSleepTimerFadeOutSeconds(seconds: Int) {
        dataStore.edit { it[sleepTimerFadeOutSecKey] = seconds.coerceIn(1, 15) }
    }

    suspend fun saveSleepTimerCustomPresets(minutesList: List<Int>) {
        val clean = minutesList.map { it.coerceIn(1, 180) }.distinct().take(9)
        dataStore.edit { it[sleepTimerPresetsCsvKey] = clean.joinToString(",") }
    }

    suspend fun saveCustomEqProfile(profile: SavedCustomEqProfile) {
        dataStore.edit { prefs ->
            val current = decodeCustomEqProfiles(prefs[customEqProfilesJsonKey].orEmpty())
                .filterNot { it.id == profile.id || it.name.equals(profile.name, ignoreCase = true) }
            prefs[customEqProfilesJsonKey] = encodeCustomEqProfiles(listOf(profile) + current)
        }
    }

    suspend fun deleteCustomEqProfile(profileId: String) {
        dataStore.edit { prefs ->
            val filtered = decodeCustomEqProfiles(prefs[customEqProfilesJsonKey].orEmpty())
                .filterNot { it.id == profileId }
            prefs[customEqProfilesJsonKey] = encodeCustomEqProfiles(filtered)
        }
    }

    suspend fun toggleOfflineFavorite(trackKey: String): Boolean {
        var added = false
        dataStore.edit { prefs ->
            val current = prefs[offlineFavoriteKeysKey].orEmpty().toMutableSet()
            added = if (current.contains(trackKey)) {
                current.remove(trackKey)
                false
            } else {
                current.add(trackKey)
                true
            }
            prefs[offlineFavoriteKeysKey] = current
        }
        return added
    }

    suspend fun recordOfflineListening(
        track: LocalTrack,
        positionMs: Long,
        deltaSeconds: Long,
        slot: TimeOfDaySlot
    ) {
        dataStore.edit { prefs ->
            val total = (prefs[offlineTotalSecondsKey] ?: 0L) + deltaSeconds.coerceAtLeast(0L)
            prefs[offlineTotalSecondsKey] = total
            val json = JSONObject().apply {
                put("trackKey", track.trackKey)
                put("trackTitle", track.title)
                put("artistName", track.artist)
                put("albumName", track.album)
                put("trackUri", track.contentUri)
                put("albumId", track.albumId)
                put("positionMs", positionMs.coerceAtLeast(0L))
                put("durationMs", track.durationMs.coerceAtLeast(0L))
                put("timeOfDaySlot", slot.wireValue)
            }.toString()
            prefs[offlineLastTrackJsonKey] = json
        }
    }

    suspend fun saveAmbientMix(mix: SavedAmbientMix) {
        dataStore.edit { prefs ->
            val current = SavedAmbientMix.decodeList(prefs[savedAmbientMixesJsonKey].orEmpty())
                .filterNot { it.id == mix.id }
            prefs[savedAmbientMixesJsonKey] = SavedAmbientMix.encodeList((listOf(mix) + current).take(24))
        }
    }

    suspend fun deleteAmbientMix(mixId: String) {
        dataStore.edit { prefs ->
            val filtered = SavedAmbientMix.decodeList(prefs[savedAmbientMixesJsonKey].orEmpty())
                .filterNot { it.id == mixId }
            prefs[savedAmbientMixesJsonKey] = SavedAmbientMix.encodeList(filtered)
        }
    }

    suspend fun appendSongRecognitionRecord(record: SongRecognitionRecord) {
        dataStore.edit { prefs ->
            val current = SongRecognitionRecord.decodeList(prefs[songRecognitionHistoryJsonKey].orEmpty())
                .filterNot {
                    it.id == record.id ||
                        (it.title.equals(record.title, ignoreCase = true) && it.artist.equals(record.artist, ignoreCase = true))
                }
            prefs[songRecognitionHistoryJsonKey] = SongRecognitionRecord.encodeList((listOf(record) + current).take(40))
        }
    }

    suspend fun deleteSongRecognitionRecord(recordId: String) {
        dataStore.edit { prefs ->
            val filtered = SongRecognitionRecord.decodeList(prefs[songRecognitionHistoryJsonKey].orEmpty())
                .filterNot { it.id == recordId }
            prefs[songRecognitionHistoryJsonKey] = SongRecognitionRecord.encodeList(filtered)
        }
    }

    suspend fun clearSongRecognitionHistory() {
        dataStore.edit { it.remove(songRecognitionHistoryJsonKey) }
    }

    companion object {
        fun encodeCustomPlaylists(playlists: List<CustomPlaylist>): String {
            val arr = JSONArray()
            playlists.forEach { pl ->
                val obj = JSONObject()
                obj.put("id", pl.id)
                obj.put("name", pl.name)
                obj.put("createdAtMs", pl.createdAtMs)
                val keysArr = JSONArray()
                pl.trackKeys.forEach { keysArr.put(it) }
                obj.put("trackKeys", keysArr)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun decodeCustomPlaylists(raw: String): List<CustomPlaylist> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val result = mutableListOf<CustomPlaylist>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val created = obj.optLong("createdAtMs", System.currentTimeMillis())
                    val keysJson = obj.optJSONArray("trackKeys")
                    val keys = mutableListOf<String>()
                    if (keysJson != null) {
                        for (j in 0 until keysJson.length()) {
                            val k = keysJson.optString(j)
                            if (k.isNotBlank()) keys.add(k)
                        }
                    }
                    result.add(CustomPlaylist(id = id, name = name, trackKeys = keys, createdAtMs = created))
                }
                result
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun encodeCoverMap(map: Map<String, String>): String {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            return obj.toString()
        }

        private fun decodeCoverMap(raw: String): Map<String, String> {
            if (raw.isBlank()) return emptyMap()
            return try {
                val obj = JSONObject(raw)
                val result = mutableMapOf<String, String>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val valStr = obj.optString(key)
                    if (valStr.isNotBlank()) result[key] = valStr
                }
                result
            } catch (_: Exception) {
                emptyMap()
            }
        }

        fun encodeCustomEqProfiles(profiles: List<SavedCustomEqProfile>): String {
            val arr = JSONArray()
            profiles.forEach { profile ->
                val obj = JSONObject()
                obj.put("id", profile.id)
                obj.put("name", profile.name)
                obj.put("bassBoostStrength", profile.bassBoostStrength)
                obj.put("spatial3dEnabled", profile.spatial3dEnabled)
                val bandsArr = JSONArray()
                profile.bandLevelsMb.forEach { bandsArr.put(it) }
                obj.put("bandLevelsMb", bandsArr)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun decodeCustomEqProfiles(raw: String): List<SavedCustomEqProfile> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val result = mutableListOf<SavedCustomEqProfile>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val bandsJson = obj.optJSONArray("bandLevelsMb")
                    val bands = mutableListOf<Int>()
                    if (bandsJson != null) {
                        for (j in 0 until bandsJson.length()) {
                            bands.add(bandsJson.optInt(j, 0))
                        }
                    }
                    result.add(
                        SavedCustomEqProfile(
                            id = id,
                            name = name,
                            bandLevelsMb = bands,
                            bassBoostStrength = obj.optInt("bassBoostStrength", 0),
                            spatial3dEnabled = obj.optBoolean("spatial3dEnabled", false)
                        )
                    )
                }
                result
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun decodeOfflineProgress(raw: String?, totalSeconds: Long): PlaybackProgress? {
            if (raw.isNullOrBlank()) return null
            return try {
                val obj = JSONObject(raw)
                PlaybackProgress(
                    id = "offline",
                    userId = "offline",
                    trackKey = obj.optString("trackKey"),
                    trackTitle = obj.optString("trackTitle"),
                    artistName = obj.optString("artistName"),
                    albumName = obj.optString("albumName"),
                    trackUri = obj.optString("trackUri"),
                    albumId = obj.optLong("albumId", 0L),
                    positionMs = obj.optLong("positionMs", 0L),
                    durationMs = obj.optLong("durationMs", 0L),
                    totalPlaybackSeconds = totalSeconds,
                    timeOfDaySlot = obj.optString("timeOfDaySlot", TimeOfDaySlot.MORNING.wireValue)
                )
            } catch (_: Exception) {
                null
            }
        }

        fun encodePlaybackEnginePrefs(prefs: PlaybackEnginePrefs): String {
            return JSONObject().apply {
                put("crossfadeEnabled", prefs.crossfadeEnabled)
                put("crossfadeDurationSec", prefs.crossfadeDurationSec.coerceIn(1, 12))
                put("crossfadeCurve", prefs.crossfadeCurve.id)
                put("fadeInEnabled", prefs.fadeInEnabled)
                put("fadeOutEnabled", prefs.fadeOutEnabled)
                put("fadeDurationMs", prefs.fadeDurationMs.coerceIn(200, 2500))
                put("smartDjEnabled", prefs.smartDjEnabled)
                put("djTransitionStyle", prefs.djTransitionStyle.id)
                put("djSkipSilenceEnabled", prefs.djSkipSilenceEnabled)
                put("djVolumeNormalization", prefs.djVolumeNormalization)
                put("avoidQueueRepeats", prefs.avoidQueueRepeats)
            }.toString()
        }

        fun decodePlaybackEnginePrefs(raw: String): PlaybackEnginePrefs {
            if (raw.isBlank()) return PlaybackEnginePrefs()
            return try {
                val obj = JSONObject(raw)
                PlaybackEnginePrefs(
                    crossfadeEnabled = obj.optBoolean("crossfadeEnabled", false),
                    crossfadeDurationSec = obj.optInt("crossfadeDurationSec", 5).coerceIn(1, 12),
                    crossfadeCurve = CrossfadeCurve.fromId(obj.optString("crossfadeCurve")),
                    fadeInEnabled = obj.optBoolean("fadeInEnabled", true),
                    fadeOutEnabled = obj.optBoolean("fadeOutEnabled", true),
                    fadeDurationMs = obj.optInt("fadeDurationMs", 650).coerceIn(200, 2500),
                    smartDjEnabled = obj.optBoolean("smartDjEnabled", false),
                    djTransitionStyle = DjTransitionStyle.fromId(obj.optString("djTransitionStyle")),
                    djSkipSilenceEnabled = obj.optBoolean("djSkipSilenceEnabled", true),
                    djVolumeNormalization = obj.optBoolean("djVolumeNormalization", true),
                    avoidQueueRepeats = obj.optBoolean("avoidQueueRepeats", true)
                )
            } catch (_: Exception) {
                PlaybackEnginePrefs()
            }
        }

        fun encodeQueuePresets(presets: List<SavedQueuePreset>): String {
            val arr = JSONArray()
            presets.forEach { preset ->
                val obj = JSONObject()
                obj.put("id", preset.id)
                obj.put("name", preset.name)
                obj.put("createdAtMs", preset.createdAtMs)
                val keysArr = JSONArray()
                preset.trackKeys.forEach { keysArr.put(it) }
                obj.put("trackKeys", keysArr)
                arr.put(obj)
            }
            return arr.toString()
        }

        fun decodeQueuePresets(raw: String): List<SavedQueuePreset> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val list = mutableListOf<SavedQueuePreset>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val created = obj.optLong("createdAtMs", System.currentTimeMillis())
                    val keysJson = obj.optJSONArray("trackKeys")
                    val keys = mutableListOf<String>()
                    if (keysJson != null) {
                        for (j in 0 until keysJson.length()) {
                            val k = keysJson.optString(j)
                            if (k.isNotBlank()) keys.add(k)
                        }
                    }
                    if (keys.isNotEmpty()) {
                        list.add(SavedQueuePreset(id = id, name = name, trackKeys = keys, createdAtMs = created))
                    }
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeActiveQueueSnapshot(trackKeys: List<String>, index: Int, positionMs: Long): String {
            val obj = JSONObject()
            obj.put("index", index)
            obj.put("positionMs", positionMs)
            val arr = JSONArray()
            trackKeys.forEach { arr.put(it) }
            obj.put("trackKeys", arr)
            return obj.toString()
        }

        fun decodeActiveQueueSnapshot(raw: String): Triple<List<String>, Int, Long>? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val arr = obj.optJSONArray("trackKeys") ?: return null
                val keys = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val k = arr.optString(i)
                    if (k.isNotBlank()) keys.add(k)
                }
                if (keys.isEmpty()) return null
                Triple(keys, obj.optInt("index", 0).coerceAtLeast(0), obj.optLong("positionMs", 0L).coerceAtLeast(0L))
            } catch (_: Exception) {
                null
            }
        }

        fun encodeDeviceSoundProfiles(profiles: Map<AudioDeviceProfileType, DeviceSoundProfile>): String {
            val root = JSONObject()
            profiles.forEach { (type, profile) ->
                val obj = JSONObject().apply {
                    put("deviceType", type.id)
                    put("enabled", profile.enabled)
                    put("presetIndex", profile.presetIndex)
                    put("bassBoostStrength", profile.bassBoostStrength.coerceIn(0, 1000))
                    put("trebleBoostMb", profile.trebleBoostMb.coerceIn(-900, 900))
                    put("spatial3dEnabled", profile.spatial3dEnabled)
                    put("spatial3dStrength", profile.spatial3dStrength.coerceIn(0, 1000))
                    put("clarityBoostMb", profile.clarityBoostMb.coerceIn(0, 1000))
                    put("balanceLR", profile.balanceLR.toDouble())
                    put("updatedAtMs", profile.updatedAtMs)
                    val bandsArr = JSONArray()
                    profile.bandLevelsMb.forEach { bandsArr.put(it) }
                    put("bandLevelsMb", bandsArr)
                }
                root.put(type.id, obj)
            }
            return root.toString()
        }

        fun decodeDeviceSoundProfiles(raw: String): Map<AudioDeviceProfileType, DeviceSoundProfile> {
            if (raw.isBlank()) return emptyMap()
            return try {
                val root = JSONObject(raw)
                val result = mutableMapOf<AudioDeviceProfileType, DeviceSoundProfile>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.optJSONObject(key) ?: continue
                    val type = AudioDeviceProfileType.fromId(obj.optString("deviceType", key))
                    val bandsJson = obj.optJSONArray("bandLevelsMb")
                    val bands = mutableListOf<Int>()
                    if (bandsJson != null) {
                        for (i in 0 until bandsJson.length()) {
                            bands.add(bandsJson.optInt(i, 0))
                        }
                    }
                    result[type] = DeviceSoundProfile(
                        deviceType = type,
                        enabled = obj.optBoolean("enabled", true),
                        presetIndex = obj.optInt("presetIndex", 0),
                        bandLevelsMb = if (bands.isNotEmpty()) bands else listOf(0, 0, 0, 0, 0),
                        bassBoostStrength = obj.optInt("bassBoostStrength", 0).coerceIn(0, 1000),
                        trebleBoostMb = obj.optInt("trebleBoostMb", 0).coerceIn(-900, 900),
                        spatial3dEnabled = obj.optBoolean("spatial3dEnabled", false),
                        spatial3dStrength = obj.optInt("spatial3dStrength", 650).coerceIn(0, 1000),
                        clarityBoostMb = obj.optInt("clarityBoostMb", 220).coerceIn(0, 1000),
                        balanceLR = obj.optDouble("balanceLR", 0.0).toFloat().coerceIn(-1f, 1f),
                        updatedAtMs = obj.optLong("updatedAtMs", 0L)
                    )
                }
                result
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}

package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import android.util.Log
import com.example.data.EqualizerStoredPrefs
import com.example.diagnostics.AppDiagnosticsManager
import com.example.diagnostics.DiagnosticModule
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.RepairActionId
import com.example.model.AudioChannelMode
import com.example.model.AudioDeviceProfileType
import com.example.model.DeviceSoundProfile
import com.example.model.HearingProfile
import com.example.model.Spatial3dPreset
import com.example.model.StudioAudioMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqPresetDefinition(
    val index: Int,
    val name: String,
    val bandOffsetsMb: List<Int>,
    val bassBoostStrength: Int,
    val trebleBoostMb: Int = 0
)

data class EqBandUi(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
)

data class EqualizerUiState(
    val enabled: Boolean = true,
    val selectedPresetIndex: Int = 0,
    val presets: List<EqPresetDefinition> = DEFAULT_EQ_PRESETS,
    val bands: List<EqBandUi> = DEFAULT_BANDS,
    val bassBoostStrength: Int = 0,
    val trebleBoostMb: Int = 0,
    val isHardwareAttached: Boolean = false,
    val spatial3dEnabled: Boolean = false,
    val spatial3dPreset: Spatial3dPreset = Spatial3dPreset.MUSIC_3D,
    val spatial3dStrength: Int = 650,
    val clarityBoostMb: Int = 220,
    val isVirtualizerSupported: Boolean = false,
    val hearingProfile: HearingProfile = HearingProfile.STANDARD,
    val audioChannelMode: AudioChannelMode = AudioChannelMode.STEREO,
    val balanceLR: Float = 0f,
    val hiResMasteringEnabled: Boolean = true,
    val studioAudioMode: StudioAudioMode = StudioAudioMode.HIFI_MASTER,
    val dynamicPunchStrength: Int = 550,
    val antiClipLimiterEnabled: Boolean = true,
    val autoSwitchDeviceProfile: Boolean = true,
    val activeDeviceProfileType: AudioDeviceProfileType = AudioDeviceProfileType.PHONE_SPEAKER,
    val isDynamicsProcessingSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
    val isDynamicsProcessingAttached: Boolean = false,
    val isFloat32BitEnabled: Boolean = true
)

private val DEFAULT_BANDS = listOf(
    EqBandUi(0, 60, 0, -1500, 1500),
    EqBandUi(1, 230, 0, -1500, 1500),
    EqBandUi(2, 910, 0, -1500, 1500),
    EqBandUi(3, 3600, 0, -1500, 1500),
    EqBandUi(4, 14000, 0, -1500, 1500)
)

val DEFAULT_EQ_PRESETS = listOf(
    EqPresetDefinition(0, "Plano", listOf(0, 0, 0, 0, 0), 0, 0),
    EqPresetDefinition(1, "Graves Profundos", listOf(750, 450, 0, -100, 150), 700, 0),
    EqPresetDefinition(2, "Rock", listOf(500, 250, -150, 350, 600), 350, 250),
    EqPresetDefinition(3, "Pop", listOf(-150, 300, 500, 300, -100), 200, 150),
    EqPresetDefinition(4, "Jazz", listOf(350, 200, -150, 300, 450), 250, 200),
    EqPresetDefinition(5, "Clásica", listOf(450, 300, -100, 300, 500), 150, 300),
    EqPresetDefinition(6, "Electrónica", listOf(650, 350, 0, 250, 650), 600, 350),
    EqPresetDefinition(7, "Vocal", listOf(-250, 0, 550, 450, 100), 0, 200),
    EqPresetDefinition(8, "Hip-Hop", listOf(700, 500, 0, -100, 350), 650, 150),
    EqPresetDefinition(9, "Acústico", listOf(350, 150, 200, 450, 400), 150, 300),
    EqPresetDefinition(10, "Latino", listOf(550, 200, 300, 250, 500), 450, 250)
)

object AudioEffectsController {

    private const val TAG = "AudioEffects"
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    @Suppress("DEPRECATION")
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var currentAudioSessionId: Int = 0

    val channelProcessor = MonoAndBalanceAudioProcessor()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    fun loadStoredPrefs(stored: EqualizerStoredPrefs) {
        val current = _state.value
        val updatedBands = current.bands.mapIndexed { idx, band ->
            val level = stored.bandLevelsMb.getOrElse(idx) { band.levelMb }
                .coerceIn(band.minLevelMb, band.maxLevelMb)
            band.copy(levelMb = level)
        }
        _state.value = current.copy(
            enabled = stored.enabled,
            selectedPresetIndex = stored.presetIndex,
            bands = updatedBands,
            bassBoostStrength = stored.bassBoostStrength.coerceIn(0, 1000),
            trebleBoostMb = stored.trebleBoostMb.coerceIn(-900, 900),
            spatial3dEnabled = stored.spatial3dEnabled,
            spatial3dPreset = stored.spatial3dPreset,
            spatial3dStrength = stored.spatial3dStrength.coerceIn(0, 1000),
            clarityBoostMb = stored.clarityBoostMb.coerceIn(0, 1000),
            hearingProfile = stored.hearingProfile,
            audioChannelMode = stored.audioChannelMode,
            balanceLR = stored.balanceLR.coerceIn(-1f, 1f),
            hiResMasteringEnabled = stored.hiResMasteringEnabled,
            studioAudioMode = stored.studioAudioMode,
            dynamicPunchStrength = stored.dynamicPunchStrength.coerceIn(0, 1000),
            antiClipLimiterEnabled = stored.antiClipLimiterEnabled,
            autoSwitchDeviceProfile = stored.autoSwitchDeviceProfile
        )
        channelProcessor.setChannelConfig(stored.audioChannelMode, stored.balanceLR)
        applyToHardware()
    }

    @Suppress("DEPRECATION")
    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || (audioSessionId == currentAudioSessionId && equalizer != null)) return
        release()
        currentAudioSessionId = audioSessionId
        _audioSessionId.value = audioSessionId
        try {
            val eq = Equalizer(0, audioSessionId)
            val numBands = eq.numberOfBands.toInt().coerceAtLeast(0)
            val range = eq.bandLevelRange
            val minMb = range.getOrNull(0)?.toInt() ?: -1500
            val maxMb = range.getOrNull(1)?.toInt() ?: 1500

            val existingBands = _state.value.bands
            val hardwareBands = (0 until numBands).map { index ->
                val centerHz = (eq.getCenterFreq(index.toShort()) / 1000).coerceAtLeast(20)
                val targetLevel = existingBands.getOrNull(index)?.levelMb?.coerceIn(minMb, maxMb) ?: 0
                EqBandUi(
                    index = index,
                    centerFreqHz = centerHz,
                    levelMb = targetLevel,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb
                )
            }

            val bb = try {
                BassBoost(0, audioSessionId)
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost unavailable: ${e.message}")
                null
            }

            val virt = try {
                Virtualizer(0, audioSessionId)
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer unavailable: ${e.message}")
                null
            }

            val le = try {
                LoudnessEnhancer(audioSessionId)
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer unavailable: ${e.message}")
                null
            }

            val dp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val cfg = DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2,
                        true,
                        10,
                        true,
                        3,
                        true,
                        10,
                        true
                    )
                        .setPreferredFrameDuration(10f)
                        .build()
                    DynamicsProcessing(0, audioSessionId, cfg)
                } catch (e: Exception) {
                    Log.w(TAG, "DynamicsProcessing unavailable: ${e.message}")
                    null
                }
            } else null

            equalizer = eq
            bassBoost = bb
            virtualizer = virt
            loudnessEnhancer = le
            dynamicsProcessing = dp
            _state.value = _state.value.copy(
                bands = if (hardwareBands.isNotEmpty()) hardwareBands else existingBands,
                isHardwareAttached = true,
                isVirtualizerSupported = virt != null,
                isDynamicsProcessingAttached = dp != null
            )
            applyToHardware()
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Equalizer unavailable on this device: ${e.message}")
            _state.value = _state.value.copy(isHardwareAttached = false, isDynamicsProcessingAttached = false)
            AppDiagnosticsManager.logTech(
                level = "WARN",
                module = DiagnosticModule.EQUALIZER,
                message = "Ecualizador hardware no disponible en sesión $audioSessionId: ${e.message}"
            )
        }
    }

    fun setEnabled(enabled: Boolean): EqualizerStoredPrefs {
        _state.value = _state.value.copy(enabled = enabled)
        applyToHardware()
        return toStoredPrefs()
    }

    fun applyPreset(presetIndex: Int): EqualizerStoredPrefs {
        val preset = _state.value.presets.getOrNull(presetIndex) ?: return toStoredPrefs()
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val offset = preset.bandOffsetsMb.getOrElse(idx) { 0 }
                .coerceIn(band.minLevelMb, band.maxLevelMb)
            band.copy(levelMb = offset)
        }
        _state.value = _state.value.copy(
            enabled = true,
            selectedPresetIndex = preset.index,
            bands = updatedBands,
            bassBoostStrength = preset.bassBoostStrength,
            trebleBoostMb = preset.trebleBoostMb
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun applyCustomBandsAndBoost(
        bandLevelsMb: List<Int>,
        bassBoostStrength: Int,
        spatial3dEnabled: Boolean,
        trebleBoostMb: Int = _state.value.trebleBoostMb
    ): EqualizerStoredPrefs {
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val level = bandLevelsMb.getOrElse(idx) { band.levelMb }
                .coerceIn(band.minLevelMb, band.maxLevelMb)
            band.copy(levelMb = level)
        }
        _state.value = _state.value.copy(
            enabled = true,
            selectedPresetIndex = -1,
            bands = updatedBands,
            bassBoostStrength = bassBoostStrength.coerceIn(0, 1000),
            trebleBoostMb = trebleBoostMb.coerceIn(-900, 900),
            spatial3dEnabled = spatial3dEnabled
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateBandLevel(bandIndex: Int, levelMb: Int): EqualizerStoredPrefs {
        val updatedBands = _state.value.bands.map { band ->
            if (band.index == bandIndex) {
                band.copy(levelMb = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb))
            } else band
        }
        _state.value = _state.value.copy(
            enabled = true,
            selectedPresetIndex = -1,
            bands = updatedBands
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateBassBoost(strength: Int): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            enabled = true,
            selectedPresetIndex = -1,
            bassBoostStrength = strength.coerceIn(0, 1000)
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateTrebleBoost(trebleMb: Int): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            enabled = true,
            selectedPresetIndex = -1,
            trebleBoostMb = trebleMb.coerceIn(-900, 900)
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun setActiveDeviceProfileType(deviceType: AudioDeviceProfileType) {
        _state.value = _state.value.copy(activeDeviceProfileType = deviceType)
    }

    fun setAutoSwitchDeviceProfile(enabled: Boolean): EqualizerStoredPrefs {
        _state.value = _state.value.copy(autoSwitchDeviceProfile = enabled)
        return toStoredPrefs()
    }

    fun buildDeviceSoundProfile(deviceType: AudioDeviceProfileType): DeviceSoundProfile {
        val cur = _state.value
        return DeviceSoundProfile(
            deviceType = deviceType,
            enabled = cur.enabled,
            presetIndex = cur.selectedPresetIndex,
            bandLevelsMb = cur.bands.map { it.levelMb },
            bassBoostStrength = cur.bassBoostStrength,
            trebleBoostMb = cur.trebleBoostMb,
            spatial3dEnabled = cur.spatial3dEnabled,
            spatial3dStrength = cur.spatial3dStrength,
            clarityBoostMb = cur.clarityBoostMb,
            balanceLR = cur.balanceLR,
            updatedAtMs = System.currentTimeMillis()
        )
    }

    fun applyDeviceSoundProfile(profile: DeviceSoundProfile): EqualizerStoredPrefs {
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val level = profile.bandLevelsMb.getOrElse(idx) { 0 }
                .coerceIn(band.minLevelMb, band.maxLevelMb)
            band.copy(levelMb = level)
        }
        val clampedBalance = profile.balanceLR.coerceIn(-1f, 1f)
        _state.value = _state.value.copy(
            enabled = profile.enabled,
            selectedPresetIndex = profile.presetIndex,
            bands = updatedBands,
            bassBoostStrength = profile.bassBoostStrength.coerceIn(0, 1000),
            trebleBoostMb = profile.trebleBoostMb.coerceIn(-900, 900),
            spatial3dEnabled = profile.spatial3dEnabled,
            spatial3dStrength = profile.spatial3dStrength.coerceIn(0, 1000),
            clarityBoostMb = profile.clarityBoostMb.coerceIn(0, 1000),
            balanceLR = clampedBalance,
            activeDeviceProfileType = profile.deviceType
        )
        channelProcessor.setChannelConfig(_state.value.audioChannelMode, clampedBalance)
        applyToHardware()
        return toStoredPrefs()
    }

    fun setSpatial3dEnabled(enabled: Boolean): EqualizerStoredPrefs {
        _state.value = _state.value.copy(spatial3dEnabled = enabled)
        applyToHardware()
        return toStoredPrefs()
    }

    fun applySpatial3dPreset(preset: Spatial3dPreset): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            spatial3dEnabled = true,
            spatial3dPreset = preset,
            spatial3dStrength = preset.virtualizerStrength,
            clarityBoostMb = preset.clarityBoostMb
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateSpatial3dStrength(strength: Int): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            spatial3dEnabled = true,
            spatial3dPreset = Spatial3dPreset.CUSTOM,
            spatial3dStrength = strength.coerceIn(0, 1000)
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateClarityBoost(gainMb: Int): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            spatial3dEnabled = true,
            spatial3dPreset = Spatial3dPreset.CUSTOM,
            clarityBoostMb = gainMb.coerceIn(0, 1000)
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun selectHearingProfile(profile: HearingProfile): EqualizerStoredPrefs {
        _state.value = _state.value.copy(hearingProfile = profile)
        applyToHardware()
        return toStoredPrefs()
    }

    fun setAudioChannelMode(mode: AudioChannelMode): EqualizerStoredPrefs {
        _state.value = _state.value.copy(audioChannelMode = mode)
        channelProcessor.setChannelConfig(mode, _state.value.balanceLR)
        return toStoredPrefs()
    }

    fun setBalanceLR(balance: Float): EqualizerStoredPrefs {
        val clamped = balance.coerceIn(-1f, 1f)
        _state.value = _state.value.copy(balanceLR = clamped)
        channelProcessor.setChannelConfig(_state.value.audioChannelMode, clamped)
        return toStoredPrefs()
    }

    fun setHiResMasteringEnabled(enabled: Boolean): EqualizerStoredPrefs {
        _state.value = _state.value.copy(hiResMasteringEnabled = enabled)
        applyToHardware()
        return toStoredPrefs()
    }

    fun selectStudioAudioMode(mode: StudioAudioMode): EqualizerStoredPrefs {
        _state.value = _state.value.copy(hiResMasteringEnabled = true, studioAudioMode = mode)
        applyToHardware()
        return toStoredPrefs()
    }

    fun updateDynamicPunch(strength: Int): EqualizerStoredPrefs {
        _state.value = _state.value.copy(
            hiResMasteringEnabled = true,
            dynamicPunchStrength = strength.coerceIn(0, 1000)
        )
        applyToHardware()
        return toStoredPrefs()
    }

    fun setAntiClipLimiterEnabled(enabled: Boolean): EqualizerStoredPrefs {
        _state.value = _state.value.copy(antiClipLimiterEnabled = enabled)
        applyToHardware()
        return toStoredPrefs()
    }

    fun toStoredPrefs(): EqualizerStoredPrefs {
        val cur = _state.value
        return EqualizerStoredPrefs(
            enabled = cur.enabled,
            presetIndex = cur.selectedPresetIndex,
            bandLevelsMb = cur.bands.map { it.levelMb },
            bassBoostStrength = cur.bassBoostStrength,
            trebleBoostMb = cur.trebleBoostMb,
            spatial3dEnabled = cur.spatial3dEnabled,
            spatial3dPreset = cur.spatial3dPreset,
            spatial3dStrength = cur.spatial3dStrength,
            clarityBoostMb = cur.clarityBoostMb,
            hearingProfile = cur.hearingProfile,
            audioChannelMode = cur.audioChannelMode,
            balanceLR = cur.balanceLR,
            hiResMasteringEnabled = cur.hiResMasteringEnabled,
            studioAudioMode = cur.studioAudioMode,
            dynamicPunchStrength = cur.dynamicPunchStrength,
            antiClipLimiterEnabled = cur.antiClipLimiterEnabled,
            autoSwitchDeviceProfile = cur.autoSwitchDeviceProfile
        )
    }

    @Suppress("DEPRECATION")
    private fun applyToHardware() {
        val cur = _state.value
        val hearingActive = cur.hearingProfile != HearingProfile.STANDARD
        val eqShouldEnable = cur.enabled || hearingActive
        try {
            equalizer?.let { eq ->
                eq.enabled = eqShouldEnable
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minMb = range.getOrNull(0)?.toInt() ?: -1500
                val maxMb = range.getOrNull(1)?.toInt() ?: 1500
                for (i in 0 until bandCount) {
                    val userLevel = if (cur.enabled) cur.bands.getOrNull(i)?.levelMb ?: 0 else 0
                    val hearingOffset = cur.hearingProfile.bandOffsetsMb.getOrElse(i) { 0 }
                    val trebleOffset = if (cur.enabled) trebleOffsetForBand(i, bandCount, cur.trebleBoostMb) else 0
                    val combined = (userLevel + hearingOffset + trebleOffset).coerceIn(minMb, maxMb)
                    eq.setBandLevel(i.toShort(), combined.toShort())
                }
            }
            bassBoost?.let { bb ->
                bb.enabled = cur.enabled && cur.bassBoostStrength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(cur.bassBoostStrength.toShort())
                }
            }
            virtualizer?.let { virt ->
                virt.enabled = cur.spatial3dEnabled && cur.spatial3dStrength > 0
                if (virt.strengthSupported) {
                    virt.setStrength(cur.spatial3dStrength.toShort())
                }
            }
            loudnessEnhancer?.let { le ->
                val spatialClarity = if (cur.spatial3dEnabled) cur.clarityBoostMb else 0
                val totalGain = (spatialClarity + cur.hearingProfile.loudnessGainMb).coerceIn(0, 1200)
                le.enabled = totalGain > 0
                if (totalGain > 0) {
                    le.setTargetGain(totalGain)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.let { dp ->
                    dp.enabled = cur.hiResMasteringEnabled || (cur.enabled && cur.trebleBoostMb != 0)
                    if (dp.enabled) {
                        val mode = cur.studioAudioMode
                        val punchScale = cur.dynamicPunchStrength / 1000f
                        dp.setInputGainAllChannelsTo(-1.2f)

                        val preEq = dp.getPreEqByChannelIndex(0)
                        val cutoffs = floatArrayOf(32f, 64f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
                        val trebleDb = if (cur.enabled) cur.trebleBoostMb / 100f else 0f
                        val masterSub = if (cur.hiResMasteringEnabled) mode.subLiftDb else 0f
                        val masterMid = if (cur.hiResMasteringEnabled) mode.warmMidDb else 0f
                        val masterAir = if (cur.hiResMasteringEnabled) mode.airLiftDb else 0f
                        val gains = floatArrayOf(
                            masterSub * 0.9f,
                            masterSub,
                            masterSub * 0.4f,
                            0f,
                            masterMid * 0.4f,
                            masterMid * 0.7f,
                            masterMid,
                            masterAir * 0.7f + trebleDb * 0.4f,
                            masterAir * 0.9f + trebleDb * 0.8f,
                            masterAir + trebleDb
                        )
                        preEq.isEnabled = (cur.hiResMasteringEnabled && mode != StudioAudioMode.PURE_DIRECT) || trebleDb != 0f
                        for (b in 0 until minOf(preEq.bandCount, cutoffs.size)) {
                            val band = preEq.getBand(b)
                            band.isEnabled = true
                            band.cutoffFrequency = cutoffs[b]
                            band.gain = gains[b]
                        }
                        dp.setPreEqAllChannelsTo(preEq)

                        val mbc = dp.getMbcByChannelIndex(0)
                        val mbcCutoffs = floatArrayOf(220f, 2800f, 20000f)
                        val ratio = 1f + (mode.mbcRatio - 1f) * punchScale
                        mbc.isEnabled = mode != StudioAudioMode.PURE_DIRECT && cur.dynamicPunchStrength > 25
                        for (b in 0 until minOf(mbc.bandCount, 3)) {
                            val band = mbc.getBand(b)
                            band.isEnabled = mbc.isEnabled
                            band.cutoffFrequency = mbcCutoffs[b]
                            band.attackTime = if (b == 0) 12f else 6f
                            band.releaseTime = if (b == 0) 140f else 85f
                            band.ratio = ratio.coerceIn(1f, 3.5f)
                            band.threshold = -18f - (punchScale * 6f)
                            band.kneeWidth = 4f
                            band.postGain = 0.8f * punchScale
                        }
                        dp.setMbcAllChannelsTo(mbc)

                        val postEq = dp.getPostEqByChannelIndex(0)
                        postEq.isEnabled = false
                        dp.setPostEqAllChannelsTo(postEq)

                        val limiter = dp.getLimiterByChannelIndex(0)
                        limiter.isEnabled = cur.antiClipLimiterEnabled
                        limiter.attackTime = 1f
                        limiter.releaseTime = 60f
                        limiter.ratio = 10f
                        limiter.threshold = -0.8f
                        limiter.postGain = if (mode == StudioAudioMode.PURE_DIRECT) 0f else 0.8f
                        dp.setLimiterAllChannelsTo(limiter)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying DSP hardware params: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dynamicsProcessing?.release()
            }
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        dynamicsProcessing = null
        currentAudioSessionId = 0
        _audioSessionId.value = 0
        _state.value = _state.value.copy(
            isHardwareAttached = false,
            isVirtualizerSupported = false,
            isDynamicsProcessingAttached = false
        )
    }

    fun trebleOffsetForBand(bandIndex: Int, bandCount: Int, trebleBoostMb: Int): Int {
        if (trebleBoostMb == 0 || bandCount <= 0) return 0
        return when {
            bandCount >= 5 && bandIndex == bandCount - 1 -> trebleBoostMb
            bandCount >= 5 && bandIndex == bandCount - 2 -> (trebleBoostMb * 0.45f).toInt()
            bandCount in 2..4 && bandIndex == bandCount - 1 -> trebleBoostMb
            bandCount == 1 -> (trebleBoostMb * 0.5f).toInt()
            else -> 0
        }
    }
}

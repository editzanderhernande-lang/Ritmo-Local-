package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.player.VoiceAudioFocusCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceUiState(
    val isAvailable: Boolean = true,
    val isListening: Boolean = false,
    val handsFreeEnabled: Boolean = false,
    val awaitingCommandAfterWake: Boolean = false,
    val partialTranscript: String = "",
    val lastTranscript: String = "",
    val statusMessage: String? = null,
    val aecActive: Boolean = false,
    val noiseSuppressionActive: Boolean = false,
    val agcActive: Boolean = false,
    val audioPipelineLabel: String = "VOICE_COMMUNICATION",
    val audioRoute: VoiceAudioRoute = VoiceAudioRoute.PHONE_SPEAKER,
    val pausedPlaybackForListening: Boolean = false
)

class OfflineVoiceRecognizer(
    private val context: Context,
    private val onRequestPauseForVoice: () -> Boolean = { false },
    private val onRequestResumeAfterVoice: () -> Unit = {},
    private val onHypothesesReady: (hypotheses: List<String>, requireWakeWord: Boolean) -> Unit
) {
    private val captureEngine = VoiceAudioCaptureEngine(context.applicationContext)
    private val initialCaps = captureEngine.queryDeviceCapabilities()

    private val _state = MutableStateFlow(
        VoiceUiState(
            isAvailable = isSpeechRecognitionSupported(context),
            aecActive = initialCaps.aecAvailable,
            noiseSuppressionActive = initialCaps.nsAvailable,
            agcActive = initialCaps.agcAvailable,
            audioPipelineLabel = initialCaps.audioSourceLabel,
            audioRoute = VoiceAudioRouteDetector.detect(context.applicationContext)
        )
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var activeReadPipe: ParcelFileDescriptor? = null
    private var activeLanguageCode: String = "es-419"
    private var handsFreeRequested: Boolean = false
    private var screenOffWakeEnabled: Boolean = true
    private var isAppInForeground: Boolean = true
    private var awaitingCommandUntilMs: Long = 0L
    private var manualPushToTalk: Boolean = false
    private var pausedMusicForActiveVoice: Boolean = false
    private var currentRoute: VoiceAudioRoute = VoiceAudioRouteDetector.detect(context.applicationContext)

    // Android 13+ supports RecognizerIntent.EXTRA_AUDIO_SOURCE with our AEC+NS+AGC PCM pipe on Phone Speaker.
    // On Bluetooth headsets we always bypass the custom pipe to avoid interfering with BT A2DP / AVRCP.
    private var customAudioPipeSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    private var lastSessionUsedCustomPipe: Boolean = false

    init {
        VoiceWakeService.onServiceRunningChanged = { running ->
            mainHandler.post {
                if (handsFreeRequested && screenOffWakeEnabled && running && !manualPushToTalk) {
                    VoiceAudioFocusCoordinator.setVoiceMicActive(true)
                    if (!_state.value.isListening) {
                        scheduleRestart(300L)
                    }
                } else if (!running && !isAppInForeground && !manualPushToTalk) {
                    mainHandler.removeCallbacks(restartRunnable)
                    destroyRecognizerOnly()
                    VoiceAudioFocusCoordinator.setVoiceMicActive(false)
                }
            }
        }
    }

    private fun refreshAudioRoute(): VoiceAudioRoute {
        val detected = VoiceAudioRouteDetector.detect(context.applicationContext)
        currentRoute = detected
        _state.value = _state.value.copy(audioRoute = detected)
        return detected
    }

    private fun canKeepHandsFreeActive(): Boolean =
        handsFreeRequested && (isAppInForeground || (screenOffWakeEnabled && VoiceWakeService.isRunning.value))

    private val restartRunnable = Runnable {
        if (canKeepHandsFreeActive() && !manualPushToTalk) {
            beginRecognitionSession(languageCode = activeLanguageCode, fromAutoRearm = true)
        }
    }

    private val expireCommandWindowRunnable = Runnable {
        awaitingCommandUntilMs = 0L
        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
        resumePlaybackIfPausedForVoice()
        _state.value = _state.value.copy(
            awaitingCommandAfterWake = false,
            pausedPlaybackForListening = false
        )
    }

    private fun resumePlaybackIfPausedForVoice() {
        if (pausedMusicForActiveVoice) {
            pausedMusicForActiveVoice = false
            _state.value = _state.value.copy(pausedPlaybackForListening = false)
            onRequestResumeAfterVoice()
        }
    }

    fun setAppForeground(inForeground: Boolean) {
        isAppInForeground = inForeground
        if (inForeground || (handsFreeRequested && screenOffWakeEnabled && VoiceWakeService.isRunning.value)) {
            refreshAudioRoute()
            if (handsFreeRequested) {
                VoiceAudioFocusCoordinator.setVoiceMicActive(true)
                if (!_state.value.isListening) {
                    scheduleRestart(350L)
                }
            }
        } else {
            mainHandler.removeCallbacks(restartRunnable)
            if (handsFreeRequested && !manualPushToTalk) {
                destroyRecognizerOnly()
            }
            if (!manualPushToTalk) {
                VoiceAudioFocusCoordinator.setVoiceMicActive(false)
            }
        }
    }

    fun configureHandsFree(enabled: Boolean, languageCode: String, screenOffEnabled: Boolean = true) {
        val prevScreenOff = screenOffWakeEnabled
        activeLanguageCode = languageCode
        screenOffWakeEnabled = screenOffEnabled
        if (handsFreeRequested == enabled && prevScreenOff == screenOffEnabled && (!enabled || _state.value.isListening)) {
            return
        }
        handsFreeRequested = enabled
        awaitingCommandUntilMs = 0L
        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
        if (!enabled) {
            resumePlaybackIfPausedForVoice()
        }
        val route = refreshAudioRoute()
        _state.value = _state.value.copy(
            handsFreeEnabled = enabled,
            awaitingCommandAfterWake = false,
            audioRoute = route
        )
        if (enabled) {
            manualPushToTalk = false
            VoiceAudioFocusCoordinator.setVoiceMicActive(true)
            if (canKeepHandsFreeActive()) {
                scheduleRestart(250L)
            }
        } else {
            mainHandler.removeCallbacks(restartRunnable)
            mainHandler.removeCallbacks(expireCommandWindowRunnable)
            if (!manualPushToTalk) {
                VoiceAudioFocusCoordinator.setVoiceMicActive(false)
                destroyRecognizerOnly()
            }
        }
    }

    fun startListening(languageCode: String) {
        activeLanguageCode = languageCode
        manualPushToTalk = true
        awaitingCommandUntilMs = 0L
        VoiceAudioFocusCoordinator.setVoiceMicActive(true)

        val route = refreshAudioRoute()
        val startDelayMs = if (route == VoiceAudioRoute.PHONE_SPEAKER) {
            // Phone loudspeaker: pause music temporarily while listening so command is heard cleanly
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            val didPause = onRequestPauseForVoice()
            if (didPause) pausedMusicForActiveVoice = true
            _state.value = _state.value.copy(
                pausedPlaybackForListening = pausedMusicForActiveVoice,
                statusMessage = if (pausedMusicForActiveVoice) {
                    "Música pausada brevemente · escuchando comando…"
                } else {
                    "Escuchando comando en altavoz…"
                }
            )
            200L
        } else {
            // Bluetooth headset: NEVER pause music or disturb BT A2DP stream
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            _state.value = _state.value.copy(
                pausedPlaybackForListening = false,
                statusMessage = "Bluetooth activo · escuchando sin pausar música…"
            )
            60L
        }

        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed({
            if (manualPushToTalk) {
                beginRecognitionSession(languageCode = languageCode, fromAutoRearm = false)
            }
        }, startDelayMs)
    }

    fun stopListening() {
        manualPushToTalk = false
        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.removeCallbacks(expireCommandWindowRunnable)
        destroyRecognizerOnly()
        resumePlaybackIfPausedForVoice()
        if (!handsFreeRequested) {
            VoiceAudioFocusCoordinator.setVoiceMicActive(false)
        }
    }

    fun enterWakeCommandWindow() {
        val windowDurationMs = 7_500L
        awaitingCommandUntilMs = System.currentTimeMillis() + windowDurationMs
        val route = refreshAudioRoute()

        val restartDelayMs = if (route == VoiceAudioRoute.PHONE_SPEAKER) {
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            if (!pausedMusicForActiveVoice) {
                pausedMusicForActiveVoice = onRequestPauseForVoice()
            }
            230L
        } else {
            // Keep Bluetooth music playing without pause or BT interruption
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            80L
        }

        mainHandler.removeCallbacks(expireCommandWindowRunnable)
        mainHandler.postDelayed(expireCommandWindowRunnable, windowDurationMs)
        _state.value = _state.value.copy(
            awaitingCommandAfterWake = true,
            pausedPlaybackForListening = pausedMusicForActiveVoice,
            statusMessage = "¡Te escucho! Di: siguiente, anterior, pausar, reanudar, subir o bajar volumen…"
        )
        if (route == VoiceAudioRoute.PHONE_SPEAKER) {
            playChime(isConfirm = false)
        }
        if (canKeepHandsFreeActive()) {
            scheduleRestart(restartDelayMs)
        }
    }

    fun markCommandExecuted(feedback: String, keepPaused: Boolean = false) {
        awaitingCommandUntilMs = 0L
        mainHandler.removeCallbacks(expireCommandWindowRunnable)
        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)

        if (keepPaused) {
            pausedMusicForActiveVoice = false
        } else {
            resumePlaybackIfPausedForVoice()
        }

        _state.value = _state.value.copy(
            awaitingCommandAfterWake = false,
            pausedPlaybackForListening = false,
            statusMessage = feedback
        )
        if (handsFreeRequested && currentRoute == VoiceAudioRoute.PHONE_SPEAKER) {
            playChime(isConfirm = true)
        }
    }

    fun completeUnrecognizedVoiceAttempt(message: String) {
        awaitingCommandUntilMs = 0L
        mainHandler.removeCallbacks(expireCommandWindowRunnable)
        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
        resumePlaybackIfPausedForVoice()
        _state.value = _state.value.copy(
            awaitingCommandAfterWake = false,
            pausedPlaybackForListening = false,
            statusMessage = message
        )
    }

    fun updateStatus(message: String) {
        _state.value = _state.value.copy(statusMessage = message)
    }

    fun release() {
        VoiceWakeService.onServiceRunningChanged = null
        handsFreeRequested = false
        manualPushToTalk = false
        pausedMusicForActiveVoice = false
        VoiceAudioFocusCoordinator.setVoiceMicActive(false)
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.removeCallbacks(expireCommandWindowRunnable)
        destroyRecognizerOnly()
    }

    private fun scheduleRestart(delayMs: Long) {
        mainHandler.removeCallbacks(restartRunnable)
        mainHandler.postDelayed(restartRunnable, delayMs)
    }

    private fun destroyRecognizerOnly() {
        try {
            recognizer?.stopListening()
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
        captureEngine.stop()
        runCatching { activeReadPipe?.close() }
        activeReadPipe = null
        _state.value = _state.value.copy(isListening = false, partialTranscript = "")
    }

    private fun createBestSpeechRecognizer(): SpeechRecognizer {
        val appCtx = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val onDeviceOk = runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(appCtx) }.getOrDefault(false)
            if (onDeviceOk) {
                val onDevice = runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(appCtx) }.getOrNull()
                if (onDevice != null) return onDevice
            }
        }
        return SpeechRecognizer.createSpeechRecognizer(appCtx)
    }

    private fun beginRecognitionSession(languageCode: String, fromAutoRearm: Boolean) {
        if (!isSpeechRecognitionSupported(context)) {
            handsFreeRequested = false
            manualPushToTalk = false
            resumePlaybackIfPausedForVoice()
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            _state.value = _state.value.copy(
                isAvailable = false,
                isListening = false,
                handsFreeEnabled = false,
                statusMessage = "El dispositivo no tiene un motor de reconocimiento de voz instalado. Usa el cuadro rápido de comando."
            )
            return
        }

        destroyRecognizerOnly()
        val route = refreshAudioRoute()

        try {
            // On Bluetooth headsets we do NOT start custom AudioRecord/VOICE_COMMUNICATION capture so BT A2DP/AVRCP stays untouched.
            val useSpeakerDspPipeline = (route == VoiceAudioRoute.PHONE_SPEAKER)
            val wantPipe = useSpeakerDspPipeline &&
                customAudioPipeSupported &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

            val readPipe: ParcelFileDescriptor?
            if (useSpeakerDspPipeline) {
                val (caps, pipe) = captureEngine.start(createOutputPipe = wantPipe)
                readPipe = pipe
                _state.value = _state.value.copy(
                    aecActive = caps.aecActive,
                    noiseSuppressionActive = caps.nsActive,
                    agcActive = caps.agcActive,
                    audioPipelineLabel = caps.audioSourceLabel,
                    audioRoute = route
                )
            } else {
                readPipe = null
                _state.value = _state.value.copy(
                    aecActive = false,
                    noiseSuppressionActive = false,
                    agcActive = false,
                    audioPipelineLabel = "BLUETOOTH_PASSIVE",
                    audioRoute = route
                )
            }

            activeReadPipe = readPipe
            lastSessionUsedCustomPipe = (readPipe != null)

            val sr = createBestSpeechRecognizer()
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    val inWakeWindow = System.currentTimeMillis() < awaitingCommandUntilMs
                    _state.value = _state.value.copy(
                        isListening = true,
                        partialTranscript = "",
                        awaitingCommandAfterWake = inWakeWindow,
                        statusMessage = if (fromAutoRearm && !inWakeWindow) null else _state.value.statusMessage
                    )
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _state.value = _state.value.copy(isListening = false)
                }

                override fun onError(error: Int) {
                    val usedPipe = lastSessionUsedCustomPipe
                    captureEngine.stop()
                    runCatching { activeReadPipe?.close() }
                    activeReadPipe = null
                    _state.value = _state.value.copy(isListening = false)

                    if (usedPipe &&
                        (error == SpeechRecognizer.ERROR_AUDIO ||
                            error == SpeechRecognizer.ERROR_CLIENT ||
                            error == SpeechRecognizer.ERROR_SERVER)
                    ) {
                        customAudioPipeSupported = false
                        scheduleRestart(250L)
                        return
                    }

                    val isFatalPermissionOrLang = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                        error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE

                    if (isFatalPermissionOrLang) {
                        handsFreeRequested = false
                        manualPushToTalk = false
                        resumePlaybackIfPausedForVoice()
                        VoiceAudioFocusCoordinator.setVoiceMicActive(false)
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso de micrófono para escuchar \"Ridmo\"."
                            else -> "Descarga el paquete de voz sin internet de este idioma en Ajustes de Android."
                        }
                        _state.value = _state.value.copy(
                            handsFreeEnabled = false,
                            awaitingCommandAfterWake = false,
                            statusMessage = msg
                        )
                        return
                    }

                    if (canKeepHandsFreeActive() && !manualPushToTalk) {
                        val inWakeWindow = System.currentTimeMillis() < awaitingCommandUntilMs
                        if (!inWakeWindow) {
                            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
                            resumePlaybackIfPausedForVoice()
                            _state.value = _state.value.copy(awaitingCommandAfterWake = false)
                        }
                        val backoffMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1100L else 420L
                        scheduleRestart(backoffMs)
                    } else {
                        manualPushToTalk = false
                        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
                        // No command detected -> automatically resume music if paused for Phone Speaker listening
                        resumePlaybackIfPausedForVoice()
                        if (!handsFreeRequested) VoiceAudioFocusCoordinator.setVoiceMicActive(false)
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó ningún comando. Música reanudada."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera finalizado."
                            else -> "No se pudo completar el reconocimiento (código $error)."
                        }
                        _state.value = _state.value.copy(statusMessage = msg)
                    }
                }

                override fun onResults(results: Bundle?) {
                    captureEngine.stop()
                    runCatching { activeReadPipe?.close() }
                    activeReadPipe = null

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val wasManual = manualPushToTalk
                    manualPushToTalk = false

                    val inWakeWindow = System.currentTimeMillis() < awaitingCommandUntilMs
                    val requireWake = !wasManual && !inWakeWindow

                    // Filter low-confidence music/lyric bleed in passive hands-free listening
                    val topConfidence = scores?.firstOrNull() ?: 1.0f
                    val filteredMatches = if (requireWake && scores != null && topConfidence < 0.30f) {
                        matches.filterIndexed { index, text ->
                            val score = scores.getOrNull(index) ?: 0f
                            score >= 0.30f || VoiceCommandParser.containsWakeWord(text)
                        }
                    } else {
                        matches
                    }
                    val first = filteredMatches.firstOrNull().orEmpty()

                    if (!inWakeWindow) {
                        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
                    }

                    _state.value = _state.value.copy(
                        isListening = false,
                        partialTranscript = "",
                        lastTranscript = if (filteredMatches.isNotEmpty() && (!requireWake || filteredMatches.any { VoiceCommandParser.containsWakeWord(it) })) {
                            first
                        } else {
                            _state.value.lastTranscript
                        }
                    )

                    if (filteredMatches.isNotEmpty()) {
                        onHypothesesReady(filteredMatches, requireWake)
                    } else {
                        resumePlaybackIfPausedForVoice()
                    }

                    if (canKeepHandsFreeActive()) {
                        scheduleRestart(380L)
                    } else {
                        VoiceAudioFocusCoordinator.setVoiceMicActive(false)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val partial = partialList.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        val wakeHeardNow = partialList.any { VoiceCommandParser.containsWakeWord(it) }
                        if (wakeHeardNow &&
                            awaitingCommandUntilMs <= System.currentTimeMillis() &&
                            currentRoute == VoiceAudioRoute.PHONE_SPEAKER &&
                            !pausedMusicForActiveVoice
                        ) {
                            // Immediately pause speaker playback on wake word detection so trailing noise stops
                            pausedMusicForActiveVoice = onRequestPauseForVoice()
                            _state.value = _state.value.copy(pausedPlaybackForListening = pausedMusicForActiveVoice)
                        }
                        _state.value = _state.value.copy(
                            partialTranscript = partial,
                            awaitingCommandAfterWake = wakeHeardNow || (System.currentTimeMillis() < awaitingCommandUntilMs)
                        )
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 700L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1150L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && readPipe != null) {
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readPipe)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, VoiceAudioCaptureEngine.SAMPLE_RATE_HZ)
                }
            }

            recognizer = sr
            _state.value = _state.value.copy(isListening = true)
            sr.startListening(intent)
        } catch (e: Exception) {
            manualPushToTalk = false
            captureEngine.stop()
            runCatching { activeReadPipe?.close() }
            activeReadPipe = null
            VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
            resumePlaybackIfPausedForVoice()
            if (!handsFreeRequested) VoiceAudioFocusCoordinator.setVoiceMicActive(false)
            _state.value = _state.value.copy(
                isListening = false,
                statusMessage = e.localizedMessage ?: "No se pudo abrir el micrófono."
            )
            if (canKeepHandsFreeActive()) {
                scheduleRestart(1500L)
            }
        }
    }

    private fun playChime(isConfirm: Boolean) {
        try {
            val tone = if (isConfirm) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_ACK
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 30)
            tg.startTone(tone, 95)
            mainHandler.postDelayed({ runCatching { tg.release() } }, 240L)
        } catch (_: Exception) {
        }
    }

    companion object {
        fun isSpeechRecognitionSupported(context: Context): Boolean {
            val standard = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
            val onDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)
            } else {
                false
            }
            return standard || onDevice
        }
    }
}

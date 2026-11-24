package com.example.recognition

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.model.LocalTrack
import com.example.voice.VoiceWakeService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

data class SongRecognitionRecord(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val sourceLabel: String,
    val artworkUrl: String?,
    val confidenceLabel: String,
    val recognizedAtMs: Long = System.currentTimeMillis(),
    val matchedLocalTrackKey: String? = null
) {
    companion object {
        fun encodeList(items: List<SongRecognitionRecord>): String {
            val arr = JSONArray()
            items.forEach { rec ->
                arr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("title", rec.title)
                        put("artist", rec.artist)
                        put("album", rec.album)
                        put("sourceLabel", rec.sourceLabel)
                        put("artworkUrl", rec.artworkUrl.orEmpty())
                        put("confidenceLabel", rec.confidenceLabel)
                        put("recognizedAtMs", rec.recognizedAtMs)
                        put("matchedLocalTrackKey", rec.matchedLocalTrackKey.orEmpty())
                    }
                )
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<SongRecognitionRecord> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val out = mutableListOf<SongRecognitionRecord>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
                    out.add(
                        SongRecognitionRecord(
                            id = id,
                            title = title,
                            artist = o.optString("artist", "Artista desconocido"),
                            album = o.optString("album", "Álbum"),
                            sourceLabel = o.optString("sourceLabel", "Reconocimiento"),
                            artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                            confidenceLabel = o.optString("confidenceLabel", "Verificado"),
                            recognizedAtMs = o.optLong("recognizedAtMs", 0L),
                            matchedLocalTrackKey = o.optString("matchedLocalTrackKey").takeIf { it.isNotBlank() }
                        )
                    )
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class SongRecognitionUiState(
    val isListening: Boolean = false,
    val isMatching: Boolean = false,
    val elapsedSeconds: Int = 0,
    val privacyNoticeAcknowledged: Boolean = false,
    val currentTranscript: String = "",
    val lastResult: SongRecognitionRecord? = null,
    val lastErrorMessage: String? = null,
    val statusDetail: String = "Toca el botón para identificar una canción cercana mediante el micrófono.",
    val acousticProxyConfigured: Boolean = hasConfiguredAcousticProxy()
) {
    companion object {
        fun isValidHttpsProxyUrl(rawUrl: String): Boolean {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) return false
            return try {
                val uri = URI(trimmed)
                uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
            } catch (_: Exception) {
                false
            }
        }

        fun hasConfiguredAcousticProxy(): Boolean =
            isValidHttpsProxyUrl(BuildConfig.SONG_RECOGNITION_PROXY_URL)
    }
}

class SongRecognitionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPauseMusicForCapture: () -> Boolean,
    private val onResumeMusicAfterCapture: () -> Unit,
    private val onRestoreHandsFreeIfNeeded: () -> Unit,
    private val onRecordPersisted: suspend (SongRecognitionRecord) -> Unit
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var timerJob: Job? = null
    private var pausedPlaybackForCapture = false
    private var pausedWakeService = false

    private val _state = MutableStateFlow(SongRecognitionUiState())
    val state: StateFlow<SongRecognitionUiState> = _state.asStateFlow()

    fun acknowledgePrivacyNotice() {
        _state.value = _state.value.copy(privacyNoticeAcknowledged = true)
    }

    fun startRecognition(
        localTracks: List<LocalTrack>,
        languageCode: String
    ) {
        if (_state.value.isListening || _state.value.isMatching) return
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            _state.value = _state.value.copy(
                lastErrorMessage = "Se necesita permiso de micrófono para escuchar el audio ambiente.",
                statusDetail = "Permiso de micrófono requerido"
            )
            return
        }

        // Safely pause local music and background wake service so microphone access has no conflicts
        pausedPlaybackForCapture = onPauseMusicForCapture()
        if (VoiceWakeService.isRunning.value) {
            VoiceWakeService.stop(context)
            pausedWakeService = true
        }

        _state.value = _state.value.copy(
            isListening = true,
            isMatching = false,
            elapsedSeconds = 0,
            currentTranscript = "",
            lastErrorMessage = null,
            statusDetail = if (_state.value.acousticProxyConfigured) {
                "Escuchando huella acústica y letra cercana…"
            } else {
                "Escuchando frase o estribillo cercano…"
            }
        )

        timerJob?.cancel()
        timerJob = scope.launch {
            var sec = 0
            while (isActive && _state.value.isListening) {
                delay(1000L)
                sec++
                _state.value = _state.value.copy(elapsedSeconds = sec)
                if (sec >= 8 && _state.value.isListening) {
                    break
                }
            }
        }

        if (_state.value.acousticProxyConfigured) {
            captureAcousticSampleAndIdentify(localTracks, languageCode)
        } else {
            runSpeechPhraseRecognition(localTracks, languageCode)
        }
    }

    fun stopRecognition() {
        timerJob?.cancel()
        mainHandler.post {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
        }
        restoreAudioEnvironment()
        _state.value = _state.value.copy(
            isListening = false,
            isMatching = false,
            statusDetail = "Identificación cancelada."
        )
    }

    private fun captureAcousticSampleAndIdentify(
        localTracks: List<LocalTrack>,
        languageCode: String
    ) {
        scope.launch(Dispatchers.IO) {
            val wavBytes = recordPcmAsWav(durationMs = 4200L)
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(
                    isListening = false,
                    isMatching = true,
                    statusDetail = "Consultando coincidencia acústica…"
                )
            }
            val proxyMatch = if (wavBytes != null) queryAcousticProxyRecognition(wavBytes, localTracks) else null
            if (proxyMatch != null) {
                onMatchFound(proxyMatch)
            } else {
                // Fall back to live phrase recognition if acoustic proxy returned no match or user is signed out
                withContext(Dispatchers.Main) {
                    runSpeechPhraseRecognition(localTracks, languageCode)
                }
            }
        }
    }

    private fun runSpeechPhraseRecognition(
        localTracks: List<LocalTrack>,
        languageCode: String
    ) {
        mainHandler.post {
            val available = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
            if (!available) {
                finishNoMatch("El servicio de reconocimiento de voz del sistema no está disponible en este dispositivo.")
                return@post
            }
            try {
                val sr = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer = sr
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _state.value = _state.value.copy(
                            isListening = false,
                            isMatching = true,
                            statusDetail = "Analizando coincidencia…"
                        )
                    }

                    override fun onError(error: Int) {
                        val partial = _state.value.currentTranscript.trim()
                        if (partial.length >= 4) {
                            matchTranscriptQuery(listOf(partial), localTracks)
                        } else {
                            finishNoMatch("No se pudo identificar la canción con claridad. Acércate al sonido o intenta de nuevo.")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val hypotheses = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        if (hypotheses.isEmpty()) {
                            finishNoMatch("No se escuchó ninguna frase o melodía reconocible.")
                        } else {
                            matchTranscriptQuery(hypotheses, localTracks)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (text.isNotBlank()) {
                            _state.value = _state.value.copy(currentTranscript = text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode.ifBlank { "es-419" })
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                }
                sr.startListening(intent)
            } catch (e: Exception) {
                finishNoMatch(e.localizedMessage ?: "No se pudo abrir el micrófono para identificar la canción.")
            }
        }
    }

    private fun matchTranscriptQuery(
        hypotheses: List<String>,
        localTracks: List<LocalTrack>
    ) {
        val cleanQuery = hypotheses.firstOrNull { it.trim().length >= 3 }?.trim().orEmpty()
        _state.value = _state.value.copy(
            isListening = false,
            isMatching = true,
            currentTranscript = cleanQuery,
            statusDetail = "Buscando '$cleanQuery' en tu biblioteca, letras y catálogo…"
        )
        scope.launch(Dispatchers.IO) {
            val match = identifyFromQuery(cleanQuery, localTracks)
            if (match != null) {
                onMatchFound(match)
            } else {
                finishNoMatch("Sin coincidencias claras para \"$cleanQuery\". Prueba captar el coro de la canción.")
            }
        }
    }

    suspend fun identifyFromQuery(
        rawQuery: String,
        localTracks: List<LocalTrack>
    ): SongRecognitionRecord? {
        val query = rawQuery.trim()
        if (query.length < 3) return null

        // 1. Check local music library first
        val localMatch = findBestLocalMatch(query, localTracks)
        if (localMatch != null) {
            return SongRecognitionRecord(
                id = "rec_${System.currentTimeMillis()}",
                title = localMatch.title,
                artist = localMatch.artist,
                album = localMatch.album.ifBlank { "Biblioteca local" },
                sourceLabel = "Biblioteca local",
                artworkUrl = localMatch.albumArtUri,
                confidenceLabel = "Coincidencia exacta local",
                matchedLocalTrackKey = localMatch.trackKey
            )
        }

        // 2. Query LRCLib public lyric search (unauthenticated) for heard lyric phrases
        queryLrclibMatch(query, localTracks)?.let { return it }

        // 3. Query public iTunes Search API (unauthenticated)
        queryItunesMatch(query, localTracks)?.let { return it }

        return null
    }

    private suspend fun onMatchFound(record: SongRecognitionRecord) {
        timerJob?.cancel()
        onRecordPersisted(record)
        withContext(Dispatchers.Main) {
            mainHandler.post {
                runCatching { speechRecognizer?.destroy() }
                speechRecognizer = null
            }
            restoreAudioEnvironment()
            _state.value = _state.value.copy(
                isListening = false,
                isMatching = false,
                lastResult = record,
                lastErrorMessage = null,
                statusDetail = "Canción identificada (${record.sourceLabel})"
            )
        }
    }

    private fun finishNoMatch(message: String) {
        timerJob?.cancel()
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
            restoreAudioEnvironment()
            _state.value = _state.value.copy(
                isListening = false,
                isMatching = false,
                lastResult = null,
                lastErrorMessage = message,
                statusDetail = message
            )
        }
    }

    private fun restoreAudioEnvironment() {
        if (pausedPlaybackForCapture) {
            pausedPlaybackForCapture = false
            onResumeMusicAfterCapture()
        }
        if (pausedWakeService) {
            pausedWakeService = false
            onRestoreHandsFreeIfNeeded()
        }
    }

    private fun queryLrclibMatch(query: String, localTracks: List<LocalTrack>): SongRecognitionRecord? {
        return runCatching {
            val enc = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://lrclib.net/api/search?q=$enc")
                .header("User-Agent", "RitmoLocal/2.3 (Android Music Player)")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                val arr = JSONArray(body)
                if (arr.length() == 0) return@use null
                val first = arr.optJSONObject(0) ?: return@use null
                val title = first.optString("trackName").trim()
                val artist = first.optString("artistName").trim()
                if (title.isBlank() || artist.isBlank()) return@use null
                val album = first.optString("albumName").trim().ifBlank { "Catálogo de letras" }
                val localHit = findLocalTrackByTitleArtist(title, artist, localTracks)
                SongRecognitionRecord(
                    id = "rec_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = album,
                    sourceLabel = "Coincidencia de letra (LRCLib)",
                    artworkUrl = localHit?.albumArtUri,
                    confidenceLabel = "Letra verificada",
                    matchedLocalTrackKey = localHit?.trackKey
                )
            }
        }.getOrNull()
    }

    private fun queryItunesMatch(query: String, localTracks: List<LocalTrack>): SongRecognitionRecord? {
        return runCatching {
            val enc = URLEncoder.encode(query, "UTF-8")
            val req = Request.Builder()
                .url("https://itunes.apple.com/search?term=$enc&entity=song&limit=3")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string().orEmpty()
                val results = JSONObject(body).optJSONArray("results") ?: return@use null
                if (results.length() == 0) return@use null
                val top = results.optJSONObject(0) ?: return@use null
                val title = top.optString("trackName").trim()
                val artist = top.optString("artistName").trim()
                if (title.isBlank() || artist.isBlank()) return@use null
                val album = top.optString("collectionName").trim().ifBlank { "Sencillo" }
                val artwork = top.optString("artworkUrl100").replace("100x100bb", "400x400bb").takeIf { it.isNotBlank() }
                val localHit = findLocalTrackByTitleArtist(title, artist, localTracks)
                SongRecognitionRecord(
                    id = "rec_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = album,
                    sourceLabel = "Catálogo iTunes Search",
                    artworkUrl = localHit?.albumArtUri ?: artwork,
                    confidenceLabel = "Título / frase encontrada",
                    matchedLocalTrackKey = localHit?.trackKey
                )
            }
        }.getOrNull()
    }

    private fun queryAcousticProxyRecognition(
        wavBytes: ByteArray,
        localTracks: List<LocalTrack>
    ): SongRecognitionRecord? {
        val proxyUrl = BuildConfig.SONG_RECOGNITION_PROXY_URL.trim()
        if (!SongRecognitionUiState.isValidHttpsProxyUrl(proxyUrl)) return null
        val idToken = runCatching {
            val user = FirebaseAuth.getInstance().currentUser ?: return@runCatching null
            Tasks.await(user.getIdToken(false), 6, TimeUnit.SECONDS)?.token
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        return runCatching {
            val req = Request.Builder()
                .url(proxyUrl)
                .header("Authorization", "Bearer $idToken")
                .post(wavBytes.toRequestBody("audio/wav".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val json = JSONObject(resp.body?.string().orEmpty())
                if (json.optString("status") != "success") return@use null
                val result = json.optJSONObject("result") ?: return@use null
                val title = result.optString("title").trim()
                val artist = result.optString("artist").trim()
                if (title.isBlank() || artist.isBlank()) return@use null
                val album = result.optString("album").trim().ifBlank { "Álbum" }
                val art = result.optString("artworkUrl").trim().takeIf { it.isNotBlank() }
                    ?: result.optJSONObject("spotify")
                        ?.optJSONObject("album")
                        ?.optJSONArray("images")
                        ?.optJSONObject(0)
                        ?.optString("url")
                        ?.takeIf { it.isNotBlank() }
                val localHit = findLocalTrackByTitleArtist(title, artist, localTracks)
                SongRecognitionRecord(
                    id = "rec_${System.currentTimeMillis()}",
                    title = title,
                    artist = artist,
                    album = album,
                    sourceLabel = "Huella acústica segura",
                    artworkUrl = localHit?.albumArtUri ?: art,
                    confidenceLabel = "Audio verificado",
                    matchedLocalTrackKey = localHit?.trackKey
                )
            }
        }.getOrNull()
    }

    private fun recordPcmAsWav(durationMs: Long): ByteArray? {
        val sampleRate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return null
        val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        }
        var recorder: AudioRecord? = null
        return try {
            recorder = AudioRecord(
                audioSource,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuf * 2, sampleRate * 2)
            )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) return null
            val pcmOut = ByteArrayOutputStream()
            val chunk = ByteArray(minBuf)
            val deadline = System.currentTimeMillis() + durationMs
            recorder.startRecording()
            while (System.currentTimeMillis() < deadline) {
                val read = recorder.read(chunk, 0, chunk.size)
                if (read > 0) pcmOut.write(chunk, 0, read)
            }
            recorder.stop()
            val pcm = pcmOut.toByteArray()
            if (pcm.isEmpty()) null else wrapPcmAsWav(pcm, sampleRate, 1, 16)
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        } finally {
            runCatching { recorder?.release() }
        }
    }

    fun release() {
        timerJob?.cancel()
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        restoreAudioEnvironment()
    }

    companion object {
        fun findBestLocalMatch(query: String, localTracks: List<LocalTrack>): LocalTrack? {
            val clean = query.trim().lowercase()
            if (clean.length < 3 || localTracks.isEmpty()) return null
            val words = clean.split(Regex("\\s+")).filter { it.length >= 3 }
            var best: LocalTrack? = null
            var bestScore = 0
            localTracks.forEach { t ->
                val titleL = t.title.lowercase()
                val artistL = t.artist.lowercase()
                var score = 0
                if (titleL == clean || "$titleL $artistL" == clean) score += 100
                else if (clean.contains(titleL) && titleL.length >= 4) score += 60
                else if (titleL.contains(clean)) score += 50
                if (clean.contains(artistL) && artistL.length >= 3) score += 30
                words.forEach { w ->
                    if (titleL.contains(w)) score += 12
                    if (artistL.contains(w)) score += 8
                }
                if (score > bestScore && score >= 45) {
                    bestScore = score
                    best = t
                }
            }
            return best
        }

        private fun findLocalTrackByTitleArtist(
            title: String,
            artist: String,
            localTracks: List<LocalTrack>
        ): LocalTrack? {
            val tL = title.lowercase()
            val aL = artist.lowercase()
            return localTracks.firstOrNull {
                (it.title.lowercase().contains(tL) || tL.contains(it.title.lowercase())) &&
                    (it.artist.lowercase().contains(aL) || aL.contains(it.artist.lowercase()))
            }
        }

        fun wrapPcmAsWav(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val totalDataLen = pcmData.size + 36
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(totalDataLen)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1.toShort()) // PCM
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((channels * bitsPerSample / 8).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmData.size)
            }.array()
            return header + pcmData
        }
    }
}

package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.model.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.Charset
import java.security.MessageDigest

data class LyricLine(
    val timestampMs: Long?,
    val text: String
)

data class TrackLyrics(
    val trackKey: String,
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val sourceLabel: String,
    val rawText: String
)

class LyricsRepository(private val context: Context) {

    private val lyricsDir: File by lazy {
        File(context.filesDir, "lyrics").apply { mkdirs() }
    }

    suspend fun loadOfflineLyrics(track: LocalTrack): TrackLyrics? = withContext(Dispatchers.IO) {
        // 1. Saved or previously cached lyrics in app storage
        val savedFile = lyricsFileFor(track.trackKey)
        if (savedFile.exists()) {
            val raw = runCatching { savedFile.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
            if (raw.isNotBlank()) {
                return@withContext parseLyricsText(
                    trackKey = track.trackKey,
                    rawText = raw,
                    sourceLabel = if (isSyncedLrc(raw)) "Sincronizada (.lrc)" else "Letra guardada"
                )
            }
        }

        // 2. Companion .lrc / .txt file next to audio file (when path resolvable)
        val audioPath = resolveAudioFilePath(track.contentUri)
        if (!audioPath.isNullOrBlank()) {
            val audioFile = File(audioPath)
            val baseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile
            if (parent != null && parent.exists()) {
                listOf(".lrc", ".txt").forEach { ext ->
                    val sidecar = File(parent, baseName + ext)
                    if (sidecar.exists() && sidecar.canRead()) {
                        val content = runCatching { sidecar.readText(Charsets.UTF_8) }.getOrNull().orEmpty()
                        if (content.isNotBlank()) {
                            return@withContext parseLyricsText(
                                trackKey = track.trackKey,
                                rawText = content,
                                sourceLabel = "Archivo local ($ext)"
                            )
                        }
                    }
                }
            }

            // 3. Embedded ID3v2 USLT lyrics tag inside MP3 files
            if (audioFile.exists() && audioFile.canRead()) {
                val embedded = extractEmbeddedId3Lyrics(audioFile)
                if (!embedded.isNullOrBlank()) {
                    return@withContext parseLyricsText(
                        trackKey = track.trackKey,
                        rawText = embedded,
                        sourceLabel = "Etiqueta embebida"
                    )
                }
            }
        }
        null
    }

    suspend fun saveCustomLyrics(track: LocalTrack, rawText: String): TrackLyrics? = withContext(Dispatchers.IO) {
        val cleaned = rawText.trim()
        if (cleaned.isBlank()) {
            clearLyrics(track.trackKey)
            return@withContext null
        }
        lyricsFileFor(track.trackKey).writeText(cleaned, Charsets.UTF_8)
        parseLyricsText(
            trackKey = track.trackKey,
            rawText = cleaned,
            sourceLabel = if (isSyncedLrc(cleaned)) "Sincronizada (.lrc)" else "Letra personalizada"
        )
    }

    suspend fun importLyricsFromUri(track: LocalTrack, uri: Uri): Result<TrackLyrics> = withContext(Dispatchers.IO) {
        try {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?.trim()
                ?: return@withContext Result.failure(IllegalStateException("No se pudo leer el archivo de letra."))
            if (raw.isBlank()) {
                return@withContext Result.failure(IllegalStateException("El archivo está vacío."))
            }
            lyricsFileFor(track.trackKey).writeText(raw, Charsets.UTF_8)
            Result.success(
                parseLyricsText(
                    trackKey = track.trackKey,
                    rawText = raw,
                    sourceLabel = if (isSyncedLrc(raw)) "Archivo .lrc importado" else "Archivo .txt importado"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearLyrics(trackKey: String) = withContext(Dispatchers.IO) {
        val file = lyricsFileFor(trackKey)
        if (file.exists()) file.delete()
    }

    suspend fun fetchOnlineLyrics(track: LocalTrack): Result<TrackLyrics> = withContext(Dispatchers.IO) {
        try {
            val titleEnc = URLEncoder.encode(track.title, "UTF-8")
            val artistEnc = URLEncoder.encode(track.artist, "UTF-8")
            val albumEnc = URLEncoder.encode(track.album, "UTF-8")
            val durationSec = (track.durationMs / 1000L).coerceAtLeast(1L)

            val exactUrl = "https://lrclib.net/api/get?track_name=$titleEnc&artist_name=$artistEnc&album_name=$albumEnc&duration=$durationSec"
            var rawCandidate = queryLrclibObject(exactUrl)

            if (rawCandidate == null) {
                val searchUrl = "https://lrclib.net/api/search?track_name=$titleEnc&artist_name=$artistEnc"
                rawCandidate = queryLrclibSearch(searchUrl)
            }

            if (rawCandidate == null) {
                return@withContext Result.failure(IllegalStateException("No se encontró letra en línea para '${track.title}'."))
            }

            val synced = rawCandidate.optString("syncedLyrics").trim()
            val plain = rawCandidate.optString("plainLyrics").trim()
            val chosen = synced.ifBlank { plain }
            if (chosen.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Pista instrumental o sin letra disponible."))
            }

            lyricsFileFor(track.trackKey).writeText(chosen, Charsets.UTF_8)
            Result.success(
                parseLyricsText(
                    trackKey = track.trackKey,
                    rawText = chosen,
                    sourceLabel = if (synced.isNotBlank()) "Sincronizada (en línea · guardada)" else "En línea · guardada offline"
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Sin conexión o error al buscar letra: ${e.localizedMessage ?: "reintenta luego"}"))
        }
    }

    private fun queryLrclibObject(urlString: String): JSONObject? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            setRequestProperty("User-Agent", "RitmoLocal/2.1 (Android Music Player)")
        }
        return try {
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body)
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun queryLrclibSearch(urlString: String): JSONObject? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 6_000
            setRequestProperty("User-Agent", "RitmoLocal/2.1 (Android Music Player)")
        }
        return try {
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)
                if (arr.length() == 0) return null
                // Prefer result that contains syncedLyrics
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    if (obj.optString("syncedLyrics").isNotBlank()) return obj
                }
                arr.optJSONObject(0)
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun resolveAudioFilePath(contentUri: String): String? {
        return try {
            val uri = Uri.parse(contentUri)
            if (uri.scheme == "file") return uri.path
            if (uri.scheme == "content" && contentUri.contains("media")) {
                @Suppress("DEPRECATION")
                context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun extractEmbeddedId3Lyrics(file: File): String? {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 20L) return null
                val header = ByteArray(10)
                raf.readFully(header)
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                    return null
                }
                val versionMajor = header[3].toInt() and 0xFF
                val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                    ((header[7].toInt() and 0x7F) shl 14) or
                    ((header[8].toInt() and 0x7F) shl 7) or
                    (header[9].toInt() and 0x7F)
                val maxRead = minOf(tagSize, 512_000)
                val tagBytes = ByteArray(maxRead)
                raf.readFully(tagBytes)

                var pos = 0
                while (pos + 10 < tagBytes.size) {
                    val frameId = String(tagBytes, pos, 4, Charsets.US_ASCII)
                    if (frameId.all { it == '\u0000' }) break
                    val frameSize = if (versionMajor >= 4) {
                        ((tagBytes[pos + 4].toInt() and 0x7F) shl 21) or
                            ((tagBytes[pos + 5].toInt() and 0x7F) shl 14) or
                            ((tagBytes[pos + 6].toInt() and 0x7F) shl 7) or
                            (tagBytes[pos + 7].toInt() and 0x7F)
                    } else {
                        ((tagBytes[pos + 4].toInt() and 0xFF) shl 24) or
                            ((tagBytes[pos + 5].toInt() and 0xFF) shl 16) or
                            ((tagBytes[pos + 6].toInt() and 0xFF) shl 8) or
                            (tagBytes[pos + 7].toInt() and 0xFF)
                    }
                    if (frameSize <= 0 || pos + 10 + frameSize > tagBytes.size) break
                    if (frameId == "USLT") {
                        return decodeUsltFrame(tagBytes, pos + 10, frameSize)
                    }
                    pos += 10 + frameSize
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeUsltFrame(data: ByteArray, start: Int, size: Int): String? {
        if (size < 5) return null
        val encoding = data[start].toInt() and 0xFF
        val charset: Charset = when (encoding) {
            1, 2 -> Charsets.UTF_16
            3 -> Charsets.UTF_8
            else -> Charsets.ISO_8859_1
        }
        // Skip encoding byte (1) + 3-byte language code + content descriptor up to null terminator
        var textOffset = start + 4
        val isUtf16 = encoding == 1 || encoding == 2
        while (textOffset < start + size) {
            if (isUtf16) {
                if (textOffset + 1 < start + size && data[textOffset] == 0.toByte() && data[textOffset + 1] == 0.toByte()) {
                    textOffset += 2
                    break
                }
                textOffset += 2
            } else {
                if (data[textOffset] == 0.toByte()) {
                    textOffset += 1
                    break
                }
                textOffset += 1
            }
        }
        if (textOffset >= start + size) return null
        return String(data, textOffset, (start + size) - textOffset, charset)
            .replace("\u0000", "")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun lyricsFileFor(trackKey: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(trackKey.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }.take(28)
        return File(lyricsDir, "$hash.lrc")
    }

    companion object {
        private val lrcTimestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        private val lrcMetaRegex = Regex("^\\[(ti|ar|al|au|by|offset|re|ve|length):.*]$", RegexOption.IGNORE_CASE)

        fun isSyncedLrc(rawText: String): Boolean = lrcTimestampRegex.containsMatchIn(rawText)

        fun parseLyricsText(trackKey: String, rawText: String, sourceLabel: String): TrackLyrics {
            val rawLines = rawText.lines()
            val timedLines = mutableListOf<LyricLine>()
            val plainLines = mutableListOf<LyricLine>()

            for (rawLine in rawLines) {
                val trimmed = rawLine.trim()
                if (trimmed.isBlank()) {
                    if (!isSyncedLrc(rawText)) plainLines.add(LyricLine(null, ""))
                    continue
                }
                if (lrcMetaRegex.matches(trimmed)) continue

                val matches = lrcTimestampRegex.findAll(trimmed).toList()
                if (matches.isNotEmpty()) {
                    val lyricText = lrcTimestampRegex.replace(trimmed, "").trim()
                    if (lyricText.isNotBlank()) {
                        for (m in matches) {
                            val min = m.groupValues[1].toLongOrNull() ?: 0L
                            val sec = m.groupValues[2].toLongOrNull() ?: 0L
                            val fracStr = m.groupValues.getOrNull(3).orEmpty()
                            val millis = when (fracStr.length) {
                                3 -> fracStr.toLongOrNull() ?: 0L
                                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                                else -> 0L
                            }
                            timedLines.add(LyricLine(timestampMs = min * 60_000L + sec * 1_000L + millis, text = lyricText))
                        }
                    }
                } else {
                    plainLines.add(LyricLine(timestampMs = null, text = trimmed))
                }
            }

            return if (timedLines.isNotEmpty()) {
                val sorted = timedLines.sortedBy { it.timestampMs ?: 0L }
                TrackLyrics(trackKey, sorted, isSynced = true, sourceLabel = sourceLabel, rawText = rawText)
            } else {
                TrackLyrics(trackKey, plainLines, isSynced = false, sourceLabel = sourceLabel, rawText = rawText)
            }
        }

        fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
            if (lines.isEmpty() || lines.none { it.timestampMs != null }) return -1
            var active = -1
            for (i in lines.indices) {
                val ts = lines[i].timestampMs ?: continue
                if (ts <= positionMs + 250L) {
                    active = i
                } else {
                    break
                }
            }
            return active
        }
    }
}

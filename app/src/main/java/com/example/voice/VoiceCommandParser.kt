package com.example.voice

import com.example.model.LocalTrack
import java.text.Normalizer
import kotlin.math.min

sealed interface ParsedVoiceCommand {
    data object WakeOnly : ParsedVoiceCommand
    data object NextTrack : ParsedVoiceCommand
    data object PreviousTrack : ParsedVoiceCommand
    data object Pause : ParsedVoiceCommand
    data object Resume : ParsedVoiceCommand
    data object VolumeUp : ParsedVoiceCommand
    data object VolumeDown : ParsedVoiceCommand
    data object ShowLyrics : ParsedVoiceCommand
    data class PlayQuery(
        val songOrArtistQuery: String,
        val artistHint: String? = null
    ) : ParsedVoiceCommand
    data class Unknown(val rawText: String) : ParsedVoiceCommand
}

object VoiceCommandParser {

    private val wakeWords = listOf(
        "ritmo local",
        "ridmo local",
        "ritmo",
        "ridmo",
        "rithmo",
        "ritmos",
        "rhythm",
        "ritme",
        "リズム",
        "节奏"
    )

    private val fillerAttentionPrefixes = listOf(
        "oye", "hola", "hey", "ok", "okay", "por favor", "please"
    )

    private val nextPhrases = listOf(
        "siguiente cancion", "proxima cancion", "otra cancion", "siguiente pista",
        "siguiente tema", "proximo tema", "cambia la cancion", "cambia cancion",
        "pasa la cancion", "cambia de cancion", "siguiente", "proxima", "adelantar",
        "next song", "next track", "skip song", "skip track", "next", "skip",
        "次の曲", "次へ", "スキップ",
        "下一首", "下一曲", "跳过",
        "lagu berikutnya", "selanjutnya", "lewati",
        "अगला गाना", "अगला",
        "sonraki sarki", "sonraki", "gec"
    )

    private val prevPhrases = listOf(
        "cancion anterior", "anterior cancion", "pista anterior", "tema anterior",
        "volver cancion", "regresar cancion", "vuelve a la cancion",
        "anterior", "atras", "regresar", "previa",
        "previous song", "previous track", "last song", "go back", "previous", "back",
        "前の曲", "前へ", "戻る",
        "上一首", "上一曲", "返回",
        "lagu sebelumnya", "sebelumnya", "kembali",
        "पिछला गाना", "पिछला",
        "onceki sarki", "onceki", "geri"
    )

    private val pausePhrases = listOf(
        "pausar la musica", "pausa la musica", "pausar musica", "pausa musica",
        "pausar reproduccion", "detener musica", "deten la musica", "para la musica",
        "para musica", "pausa", "pausar", "detener", "deten", "silencio",
        "pause music", "pause the music", "pause", "stop music", "stop",
        "一時停止", "停止", "止めて",
        "暂停", "停止",
        "jeda", "berhenti",
        "रोकें", "रोको", "रुको",
        "duraklat", "durdur"
    )

    private val resumeExactPhrases = listOf(
        "reanudar la musica", "reanudar musica", "reanudar", "continua la musica",
        "continuar musica", "seguir reproduciendo", "pon la musica",
        "reproducir", "reproduce", "continua", "continuar", "play", "resume",
        "resume music", "play music",
        "再生", "播放", "继续", "putar", "lanjut", "चलाएं", "शुरू", "oynat", "cal", "devam"
    )

    private val volumeUpPhrases = listOf(
        "subir el volumen", "subir volumen", "sube el volumen", "sube volumen",
        "aumentar el volumen", "aumentar volumen", "mas volumen", "subele el volumen",
        "subele", "subir sonido", "sube la musica",
        "volume up", "turn up the volume", "turn up volume", "turn volume up",
        "increase volume", "raise volume", "louder",
        "音量上げて", "音量を上げて", "音量アップ",
        "调高音量", "放大声音", "大声一点",
        "naikkan volume", "besarkan volume", "kerasin suara",
        "आवाज बढ़ाओ", "आवाज़ बढ़ाओ", "वॉल्यूम बढ़ाओ",
        "sesi ac", "sesi yukselt", "ses ac"
    )

    private val volumeDownPhrases = listOf(
        "bajar el volumen", "bajar volumen", "baja el volumen", "baja volumen",
        "disminuir el volumen", "disminuir volumen", "menos volumen", "bajale el volumen",
        "bajale", "bajar sonido", "baja la musica",
        "volume down", "turn down the volume", "turn down volume", "turn volume down",
        "decrease volume", "lower volume", "quieter", "softer",
        "音量下げて", "音量を下げて", "音量ダウン",
        "调低音量", "减小声音", "小声一点",
        "turunkan volume", "kecilkan volume",
        "आवाज कम करो", "आवाज़ कम करो", "वॉल्यूम कम करो",
        "sesi kis", "sesi azalt", "ses kis"
    )

    private val lyricsPhrases = listOf(
        "ver letra", "mostrar letra", "letra de la cancion", "la letra", "letra", "letras",
        "show lyrics", "lyrics", "歌詞", "歌词", "lirik", "sarki sozleri"
    )

    private val playPrefixes = listOf(
        "reproducir la cancion ", "reproduce la cancion ", "reproducir cancion ",
        "reproducir ", "reproduce ", "pon la cancion ", "ponme ", "pon ",
        "escuchar ", "toca ", "quiero escuchar ",
        "play the song ", "play song ", "play ", "listen to ",
        "putar lagu ", "putar ", "mainkan ",
        "çalar mısın ", "cal ", "oynat ",
        "再生 ", "播放 ", "चलाएं ", "बजाओ "
    )

    private val playSuffixes = listOf(
        " を再生", " 再生", " 播放", " cal", " oynat", " चलाएं", " बजाओ"
    )

    fun containsWakeWord(rawTranscript: String): Boolean {
        val norm = normalizeText(rawTranscript)
        if (norm.isBlank()) return false
        return findWakeWordIndex(norm) != null
    }

    fun parse(rawTranscript: String, requireWakeWord: Boolean = false): ParsedVoiceCommand {
        val normalized = normalizeText(rawTranscript)
        if (normalized.isBlank()) return ParsedVoiceCommand.Unknown(rawTranscript)

        val wakeMatch = findWakeWordIndex(normalized)
        if (requireWakeWord && wakeMatch == null) {
            return ParsedVoiceCommand.Unknown(rawTranscript)
        }

        val hadWake = wakeMatch != null
        val body = if (wakeMatch != null) {
            normalized.substring(wakeMatch.second).trim()
        } else {
            normalized
        }

        if (body.isBlank()) {
            return if (hadWake) ParsedVoiceCommand.WakeOnly else ParsedVoiceCommand.Unknown(rawTranscript)
        }

        if (volumeUpPhrases.any { body == it || body.startsWith("$it ") || body.contains(it) }) {
            return ParsedVoiceCommand.VolumeUp
        }
        if (volumeDownPhrases.any { body == it || body.startsWith("$it ") || body.contains(it) }) {
            return ParsedVoiceCommand.VolumeDown
        }
        if (nextPhrases.any { body == it || body.startsWith("$it ") || body.contains(it) }) {
            return ParsedVoiceCommand.NextTrack
        }
        if (prevPhrases.any { body == it || body.startsWith("$it ") || body.contains(it) }) {
            return ParsedVoiceCommand.PreviousTrack
        }
        if (pausePhrases.any { body == it || body.startsWith("$it ") || body.contains(it) }) {
            return ParsedVoiceCommand.Pause
        }
        if (lyricsPhrases.any { body == it || body.startsWith("$it ") }) {
            return ParsedVoiceCommand.ShowLyrics
        }
        if (resumeExactPhrases.any { body == it || body.startsWith("$it ") && body.removePrefix(it).isBlank() }) {
            return ParsedVoiceCommand.Resume
        }

        val queryBody = extractPlayQueryBody(body)
        if (queryBody != null) {
            return splitSongAndArtist(queryBody)
        }

        // If user said "ritmo <song/artist>" directly without verb
        if (hadWake && body.length >= 2) {
            return splitSongAndArtist(body)
        }

        return ParsedVoiceCommand.Unknown(rawTranscript)
    }

    fun findBestTrackQueue(
        tracks: List<LocalTrack>,
        songOrArtistQuery: String,
        artistHint: String? = null
    ): List<LocalTrack> {
        if (tracks.isEmpty()) return emptyList()
        val qNorm = normalizeText(songOrArtistQuery)
        val aNorm = artistHint?.let(::normalizeText)?.takeIf { it.isNotBlank() }

        if (aNorm != null) {
            val artistMatches = tracks.filter { scoreField(normalizeText(it.artist), aNorm) >= 0.55 }
            if (artistMatches.isNotEmpty()) {
                val sortedBySong = artistMatches.sortedByDescending {
                    scoreField(normalizeText(it.title), qNorm)
                }
                val bestTitleScore = scoreField(normalizeText(sortedBySong.first().title), qNorm)
                if (bestTitleScore >= 0.45) {
                    val best = sortedBySong.first()
                    return listOf(best) + artistMatches.filterNot { it.trackKey == best.trackKey }
                }
                return sortedBySong
            }
        }

        val scored = tracks.map { track ->
            val titleScore = scoreField(normalizeText(track.title), qNorm)
            val artistScore = scoreField(normalizeText(track.artist), qNorm)
            val albumScore = scoreField(normalizeText(track.album), qNorm) * 0.75
            val combinedScore = if (aNorm != null) {
                titleScore * 0.65 + scoreField(normalizeText(track.artist), aNorm) * 0.55
            } else {
                maxOf(titleScore, artistScore, albumScore)
            }
            Triple(track, combinedScore, artistScore)
        }.sortedByDescending { it.second }

        val top = scored.firstOrNull() ?: return emptyList()
        if (top.second < 0.35) return emptyList()

        return if (top.third >= 0.72 && scoreField(normalizeText(top.first.title), qNorm) < top.third) {
            val artistName = top.first.artist
            val artistQueue = tracks.filter { it.artist.equals(artistName, ignoreCase = true) }
            if (artistQueue.isNotEmpty()) artistQueue else listOf(top.first)
        } else {
            val best = top.first
            listOf(best) + tracks.filterNot { it.trackKey == best.trackKey }
        }
    }

    private fun findWakeWordIndex(normalized: String): Pair<Int, Int>? {
        val cleaned = fillerAttentionPrefixes.fold(normalized) { acc, prefix ->
            acc.removePrefix("$prefix ").trim()
        }
        val offset = normalized.length - cleaned.length
        for (wake in wakeWords) {
            if (cleaned == wake) return (offset to offset + wake.length)
            if (cleaned.startsWith("$wake ")) return (offset to offset + wake.length)
            val idx = normalized.indexOf(" $wake ")
            if (idx >= 0) return (idx + 1 to idx + 1 + wake.length)
            if (normalized.endsWith(" $wake")) {
                val endIdx = normalized.length - wake.length
                return (endIdx to normalized.length)
            }
        }
        return null
    }

    private fun extractPlayQueryBody(body: String): String? {
        for (prefix in playPrefixes) {
            if (body.startsWith(prefix)) {
                return body.removePrefix(prefix).trim().takeIf { it.isNotBlank() }
            }
        }
        for (suffix in playSuffixes) {
            if (body.endsWith(suffix.trim())) {
                return body.removeSuffix(suffix.trim()).trim().takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun splitSongAndArtist(queryBody: String): ParsedVoiceCommand.PlayQuery {
        val separators = listOf(" del artista ", " de ", " by ", " oleh ", " から ", " の ")
        for (sep in separators) {
            val idx = queryBody.lastIndexOf(sep)
            if (idx > 0 && idx + sep.length < queryBody.length) {
                val song = queryBody.substring(0, idx).trim()
                val artist = queryBody.substring(idx + sep.length).trim()
                if (song.isNotBlank() && artist.isNotBlank()) {
                    return ParsedVoiceCommand.PlayQuery(songOrArtistQuery = song, artistHint = artist)
                }
            }
        }
        return ParsedVoiceCommand.PlayQuery(songOrArtistQuery = queryBody, artistHint = null)
    }

    internal fun normalizeText(raw: String): String {
        val decomposed = Normalizer.normalize(raw.lowercase().trim(), Normalizer.Form.NFD)
        val noMarks = decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return noMarks.replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun scoreField(candidate: String, query: String): Double {
        if (candidate.isBlank() || query.isBlank()) return 0.0
        if (candidate == query) return 1.0
        if (candidate.contains(query)) return 0.90
        if (query.contains(candidate) && candidate.length >= 3) return 0.80

        val qTokens = query.split(" ").filter { it.length >= 2 }
        val cTokens = candidate.split(" ").filter { it.length >= 2 }
        if (qTokens.isNotEmpty() && cTokens.isNotEmpty()) {
            val hits = qTokens.count { qt -> cTokens.any { ct -> ct.contains(qt) || qt.contains(ct) } }
            val tokenRatio = hits.toDouble() / qTokens.size.toDouble()
            if (tokenRatio >= 0.5) return 0.55 + tokenRatio * 0.30
        }

        val maxLen = maxOf(candidate.length, query.length)
        val dist = levenshtein(candidate, query)
        return (1.0 - dist.toDouble() / maxLen.toDouble()).coerceAtLeast(0.0)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}

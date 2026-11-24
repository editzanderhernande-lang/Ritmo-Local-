package com.example.data

import android.content.Context
import com.example.model.AlbumStatSummary
import com.example.model.ArtistStatSummary
import com.example.model.LocalTrack
import com.example.model.MonthlyStat
import com.example.model.MonthlyTrackStat
import com.example.model.TimeOfDaySlot
import com.example.model.currentMonthKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class StatsPeriodFilter(
    val id: String,
    val label: String
) {
    WEEKLY("weekly", "Semanal"),
    MONTHLY("monthly", "Mensual"),
    ANNUAL("annual", "Anual"),
    COMPARISON("comparison", "Comparativa");

    companion object {
        fun fromId(id: String?): StatsPeriodFilter =
            entries.firstOrNull { it.id == id } ?: MONTHLY
    }
}

data class PlaybackHistoryEntry(
    val id: String,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genreTag: String,
    val formatLabel: String,
    val durationListenedSec: Long,
    val playIncrement: Long,
    val timestampMs: Long,
    val slotWire: String
)

data class DailyListeningPoint(
    val label: String,
    val totalSeconds: Long,
    val playCount: Long
)

data class GenreDistributionSlice(
    val genre: String,
    val totalSeconds: Long,
    val playCount: Long,
    val fraction: Float
)

data class PeriodComparisonSummary(
    val currentLabel: String,
    val previousLabel: String,
    val currentSeconds: Long,
    val previousSeconds: Long,
    val currentPlays: Long,
    val previousPlays: Long,
    val percentDeltaSeconds: Int
)

data class AdvancedStatsSnapshot(
    val selectedMonthKey: String,
    val availableMonths: List<String>,
    val totalSecondsAllPeriods: Map<StatsPeriodFilter, Long>,
    val totalPlaysAllPeriods: Map<StatsPeriodFilter, Long>,
    val chartPointsByPeriod: Map<StatsPeriodFilter, List<DailyListeningPoint>>,
    val genreDistribution: List<GenreDistributionSlice>,
    val comparisonSummary: PeriodComparisonSummary,
    val recentHistory: List<PlaybackHistoryEntry>,
    val offlineMonthlySummaries: List<MonthlyStat>,
    val offlineTracksByMonth: Map<String, List<MonthlyTrackStat>>,
    val offlinePlayCountsByTrack: Map<String, Long>
)

class ListeningAnalyticsStore(
    private val storageFile: File
) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, "listening_analytics_v1.json"))

    private val lock = Any()
    private val _history = MutableStateFlow<List<PlaybackHistoryEntry>>(loadFromDisk())
    val history: StateFlow<List<PlaybackHistoryEntry>> = _history.asStateFlow()

    fun recordSession(
        track: LocalTrack,
        listenedSeconds: Long,
        incrementPlay: Boolean,
        slot: TimeOfDaySlot,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val safeSec = listenedSeconds.coerceAtLeast(0L)
        val inc = if (incrementPlay) 1L else 0L
        if (safeSec == 0L && inc == 0L) return

        val entry = PlaybackHistoryEntry(
            id = "h_${timestampMs}_${(100..999).random()}",
            trackKey = track.trackKey,
            title = track.title.ifBlank { "Sin título" },
            artist = track.artist.ifBlank { "Artista desconocido" },
            album = track.album.ifBlank { "Álbum desconocido" },
            albumId = track.albumId,
            genreTag = inferGenreTag(track),
            formatLabel = track.formatLabel.ifBlank { "AUDIO" },
            durationListenedSec = safeSec,
            playIncrement = inc,
            timestampMs = timestampMs,
            slotWire = slot.wireValue
        )

        synchronized(lock) {
            val current = _history.value.toMutableList()
            // Merge with very recent event on the same track within 45s to keep history compact and readable
            val latest = current.firstOrNull()
            if (latest != null &&
                latest.trackKey == entry.trackKey &&
                !incrementPlay &&
                (timestampMs - latest.timestampMs) in 0L..45_000L
            ) {
                current[0] = latest.copy(
                    durationListenedSec = latest.durationListenedSec + safeSec,
                    timestampMs = timestampMs
                )
            } else {
                current.add(0, entry)
            }
            val trimmed = current.take(MAX_HISTORY_ENTRIES)
            _history.value = trimmed
            saveToDisk(trimmed)
        }
    }

    fun buildSnapshot(
        selectedMonthKey: String,
        localTracks: List<LocalTrack>,
        cloudSummaries: List<MonthlyStat> = emptyList(),
        cloudMonthTracks: List<MonthlyTrackStat> = emptyList(),
        nowMs: Long = System.currentTimeMillis()
    ): AdvancedStatsSnapshot {
        val entries = _history.value
        val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.US)

        // Group local entries by yyyy-MM
        val entriesByMonth = entries.groupBy { monthFormatter.format(Date(it.timestampMs)) }

        val offlineSummaries = entriesByMonth.map { (mKey, list) ->
            MonthlyStat(
                id = mKey,
                userId = "offline",
                monthKey = mKey,
                totalSeconds = list.sumOf { it.durationListenedSec },
                totalPlays = list.sumOf { it.playIncrement }.coerceAtLeast(if (list.isNotEmpty()) 1L else 0L),
                morningSeconds = list.filter { it.slotWire == TimeOfDaySlot.MORNING.wireValue }.sumOf { it.durationListenedSec },
                afternoonSeconds = list.filter { it.slotWire == TimeOfDaySlot.AFTERNOON.wireValue }.sumOf { it.durationListenedSec },
                nightSeconds = list.filter { it.slotWire == TimeOfDaySlot.NIGHT.wireValue }.sumOf { it.durationListenedSec }
            )
        }

        val offlineTracksByMonth = entriesByMonth.mapValues { (mKey, list) ->
            list.groupBy { it.trackKey }.map { (tKey, tList) ->
                val sample = tList.first()
                MonthlyTrackStat(
                    id = tKey,
                    userId = "offline",
                    monthKey = mKey,
                    trackKey = tKey,
                    trackTitle = sample.title,
                    artistName = sample.artist,
                    albumName = sample.album,
                    albumId = sample.albumId,
                    playCount = tList.sumOf { it.playIncrement }.coerceAtLeast(1L),
                    totalSeconds = tList.sumOf { it.durationListenedSec }
                )
            }.sortedWith(compareByDescending<MonthlyTrackStat> { it.totalSeconds }.thenByDescending { it.playCount })
        }

        // Time windows for Weekly (last 7 days), Monthly (30 days / selected month), Annual (last 12 months)
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayShortFormat = SimpleDateFormat("EEE", Locale.forLanguageTag("es-ES"))
        val monthShortFormat = SimpleDateFormat("MMM", Locale.forLanguageTag("es-ES"))

        // Weekly chart: 7 days ending today
        val weekDays = (6 downTo 0).map { offset ->
            val c = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = dayKeyFormat.format(c.time)
            val label = dayShortFormat.format(c.time).take(3).replaceFirstChar { it.uppercase() }
            key to label
        }
        val entriesByDay = entries.groupBy { dayKeyFormat.format(Date(it.timestampMs)) }
        val weeklyPoints = weekDays.map { (key, label) ->
            val dayEntries = entriesByDay[key].orEmpty()
            DailyListeningPoint(
                label = label,
                totalSeconds = dayEntries.sumOf { it.durationListenedSec },
                playCount = dayEntries.sumOf { it.playIncrement }
            )
        }

        // Monthly chart: 4 weeks inside selected month (or last 28 days bucketed into S1..S4)
        val selectedMonthEntries = entriesByMonth[selectedMonthKey].orEmpty()
        val weekBuckets = MutableList(4) { mutableListOf<PlaybackHistoryEntry>() }
        selectedMonthEntries.forEach { e ->
            val c = Calendar.getInstance().apply { timeInMillis = e.timestampMs }
            val day = c.get(Calendar.DAY_OF_MONTH)
            val bucketIdx = ((day - 1) / 8).coerceIn(0, 3)
            weekBuckets[bucketIdx].add(e)
        }
        val monthlyPoints = weekBuckets.mapIndexed { idx, bucket ->
            var sec = bucket.sumOf { it.durationListenedSec }
            var plays = bucket.sumOf { it.playIncrement }
            // If cloud stats exist for the month without matching local events, attribute remainder to current bucket
            if (idx == 3 && sec == 0L) {
                val cloudSec = cloudSummaries.firstOrNull { it.monthKey == selectedMonthKey }?.totalSeconds ?: 0L
                val existingSum = weekBuckets.flatten().sumOf { it.durationListenedSec }
                if (cloudSec > existingSum) {
                    sec += (cloudSec - existingSum)
                    plays += (cloudSummaries.firstOrNull { it.monthKey == selectedMonthKey }?.totalPlays ?: 0L)
                }
            }
            DailyListeningPoint(label = "Sem ${idx + 1}", totalSeconds = sec, playCount = plays)
        }

        // Annual chart: last 6 months ending this month
        val yearMonths = (5 downTo 0).map { offset ->
            val c = (calNow.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
            val key = monthFormatter.format(c.time)
            val label = monthShortFormat.format(c.time).take(3).replaceFirstChar { it.uppercase() }
            key to label
        }
        val annualPoints = yearMonths.map { (mKey, label) ->
            val off = offlineSummaries.firstOrNull { it.monthKey == mKey }
            val cld = cloudSummaries.firstOrNull { it.monthKey == mKey }
            DailyListeningPoint(
                label = label,
                totalSeconds = maxOf(off?.totalSeconds ?: 0L, cld?.totalSeconds ?: 0L),
                playCount = maxOf(off?.totalPlays ?: 0L, cld?.totalPlays ?: 0L)
            )
        }

        // Period comparison: this month vs previous month (or this 7d vs previous 7d)
        val prevMonthCal = (calNow.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val prevMonthKey = monthFormatter.format(prevMonthCal.time)
        val curSummary = mergeMonthSummary(
            offlineSummaries.firstOrNull { it.monthKey == selectedMonthKey },
            cloudSummaries.firstOrNull { it.monthKey == selectedMonthKey },
            selectedMonthKey
        )
        val prevSummary = mergeMonthSummary(
            offlineSummaries.firstOrNull { it.monthKey == prevMonthKey },
            cloudSummaries.firstOrNull { it.monthKey == prevMonthKey },
            prevMonthKey
        )
        val curSec = curSummary?.totalSeconds ?: 0L
        val prevSec = prevSummary?.totalSeconds ?: 0L
        val pctDelta = when {
            prevSec <= 0L && curSec > 0L -> 100
            prevSec <= 0L -> 0
            else -> (((curSec - prevSec).toDouble() / prevSec.toDouble()) * 100.0).toInt()
        }
        val comparison = PeriodComparisonSummary(
            currentLabel = selectedMonthKey,
            previousLabel = prevMonthKey,
            currentSeconds = curSec,
            previousSeconds = prevSec,
            currentPlays = curSummary?.totalPlays ?: 0L,
            previousPlays = prevSummary?.totalPlays ?: 0L,
            percentDeltaSeconds = pctDelta
        )

        val comparisonPoints = listOf(
            DailyListeningPoint("Anterior", comparison.previousSeconds, comparison.previousPlays),
            DailyListeningPoint("Actual", comparison.currentSeconds, comparison.currentPlays)
        )

        // Genre distribution across entries (supplemented with localTracks / cloudMonthTracks)
        val genreCounts = mutableMapOf<String, Pair<Long, Long>>() // genre -> (seconds, plays)
        if (entries.isNotEmpty()) {
            entries.forEach { e ->
                val prev = genreCounts[e.genreTag] ?: (0L to 0L)
                genreCounts[e.genreTag] = (prev.first + e.durationListenedSec) to (prev.second + e.playIncrement)
            }
        } else if (cloudMonthTracks.isNotEmpty()) {
            val localByKey = localTracks.associateBy { it.trackKey }
            cloudMonthTracks.forEach { st ->
                val g = localByKey[st.trackKey]?.let(::inferGenreTag) ?: inferGenreFromStrings(st.trackTitle, st.artistName, st.albumName, "AUDIO")
                val prev = genreCounts[g] ?: (0L to 0L)
                genreCounts[g] = (prev.first + st.totalSeconds) to (prev.second + st.playCount)
            }
        }
        val totalGenreSec = genreCounts.values.sumOf { it.first }.coerceAtLeast(1L)
        val genreSlices = genreCounts.entries
            .sortedByDescending { it.value.first }
            .take(6)
            .map { (genre, sp) ->
                GenreDistributionSlice(
                    genre = genre,
                    totalSeconds = sp.first,
                    playCount = sp.second,
                    fraction = (sp.first.toFloat() / totalGenreSec.toFloat()).coerceIn(0.02f, 1f)
                )
            }

        val weeklyTotalSec = weeklyPoints.sumOf { it.totalSeconds }
        val weeklyTotalPlays = weeklyPoints.sumOf { it.playCount }
        val monthlyTotalSec = maxOf(
            curSummary?.totalSeconds ?: 0L,
            selectedMonthEntries.sumOf { it.durationListenedSec }
        )
        val monthlyTotalPlays = maxOf(
            curSummary?.totalPlays ?: 0L,
            selectedMonthEntries.sumOf { it.playIncrement }
        )
        val annualTotalSec = maxOf(
            entries.sumOf { it.durationListenedSec },
            cloudSummaries.sumOf { it.totalSeconds }
        )
        val annualTotalPlays = maxOf(
            entries.sumOf { it.playIncrement },
            cloudSummaries.sumOf { it.totalPlays }
        )

        val allMonths = (listOf(currentMonthKey()) + offlineSummaries.map { it.monthKey } + cloudSummaries.map { it.monthKey })
            .distinct()
            .sortedDescending()

        val playCountsByTrack = entries
            .groupBy { it.trackKey }
            .mapValues { (_, list) -> list.sumOf { it.playIncrement }.coerceAtLeast(1L) }

        return AdvancedStatsSnapshot(
            selectedMonthKey = selectedMonthKey,
            availableMonths = allMonths,
            totalSecondsAllPeriods = mapOf(
                StatsPeriodFilter.WEEKLY to weeklyTotalSec,
                StatsPeriodFilter.MONTHLY to monthlyTotalSec,
                StatsPeriodFilter.ANNUAL to annualTotalSec,
                StatsPeriodFilter.COMPARISON to comparison.currentSeconds
            ),
            totalPlaysAllPeriods = mapOf(
                StatsPeriodFilter.WEEKLY to weeklyTotalPlays,
                StatsPeriodFilter.MONTHLY to monthlyTotalPlays,
                StatsPeriodFilter.ANNUAL to annualTotalPlays,
                StatsPeriodFilter.COMPARISON to comparison.currentPlays
            ),
            chartPointsByPeriod = mapOf(
                StatsPeriodFilter.WEEKLY to weeklyPoints,
                StatsPeriodFilter.MONTHLY to monthlyPoints,
                StatsPeriodFilter.ANNUAL to annualPoints,
                StatsPeriodFilter.COMPARISON to comparisonPoints
            ),
            genreDistribution = genreSlices,
            comparisonSummary = comparison,
            recentHistory = entries.take(50),
            offlineMonthlySummaries = offlineSummaries,
            offlineTracksByMonth = offlineTracksByMonth,
            offlinePlayCountsByTrack = playCountsByTrack
        )
    }

    private fun mergeMonthSummary(offline: MonthlyStat?, cloud: MonthlyStat?, monthKey: String): MonthlyStat? {
        if (offline == null && cloud == null) return null
        if (offline == null) return cloud
        if (cloud == null) return offline
        return MonthlyStat(
            id = monthKey,
            userId = cloud.userId.ifBlank { offline.userId },
            monthKey = monthKey,
            totalSeconds = maxOf(offline.totalSeconds, cloud.totalSeconds),
            totalPlays = maxOf(offline.totalPlays, cloud.totalPlays),
            morningSeconds = maxOf(offline.morningSeconds, cloud.morningSeconds),
            afternoonSeconds = maxOf(offline.afternoonSeconds, cloud.afternoonSeconds),
            nightSeconds = maxOf(offline.nightSeconds, cloud.nightSeconds)
        )
    }

    private fun loadFromDisk(): List<PlaybackHistoryEntry> {
        if (!storageFile.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(storageFile.readText(Charsets.UTF_8))
            val out = mutableListOf<PlaybackHistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val tKey = o.optString("trackKey")
                if (tKey.isBlank()) continue
                out.add(
                    PlaybackHistoryEntry(
                        id = o.optString("id", "h_$i"),
                        trackKey = tKey,
                        title = o.optString("title", "Sin título"),
                        artist = o.optString("artist", "Artista desconocido"),
                        album = o.optString("album", "Álbum desconocido"),
                        albumId = o.optLong("albumId", 0L),
                        genreTag = o.optString("genreTag", "Audio Local"),
                        formatLabel = o.optString("formatLabel", "AUDIO"),
                        durationListenedSec = o.optLong("durationListenedSec", 0L).coerceAtLeast(0L),
                        playIncrement = o.optLong("playIncrement", 0L).coerceAtLeast(0L),
                        timestampMs = o.optLong("timestampMs", 0L),
                        slotWire = o.optString("slotWire", TimeOfDaySlot.MORNING.wireValue)
                    )
                )
            }
            out
        }.getOrDefault(emptyList())
    }

    private fun saveToDisk(entries: List<PlaybackHistoryEntry>) {
        runCatching {
            storageFile.parentFile?.mkdirs()
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(
                    JSONObject().apply {
                        put("id", e.id)
                        put("trackKey", e.trackKey)
                        put("title", e.title)
                        put("artist", e.artist)
                        put("album", e.album)
                        put("albumId", e.albumId)
                        put("genreTag", e.genreTag)
                        put("formatLabel", e.formatLabel)
                        put("durationListenedSec", e.durationListenedSec)
                        put("playIncrement", e.playIncrement)
                        put("timestampMs", e.timestampMs)
                        put("slotWire", e.slotWire)
                    }
                )
            }
            storageFile.writeText(arr.toString(), Charsets.UTF_8)
        }
    }

    companion object {
        private const val MAX_HISTORY_ENTRIES = 600

        fun inferGenreTag(track: LocalTrack): String =
            inferGenreFromStrings(track.title, track.artist, track.album, track.formatLabel)

        fun inferGenreFromStrings(title: String, artist: String, album: String, formatLabel: String): String {
            val hay = "$title $artist $album".lowercase(Locale.US)
            return when {
                listOf("rock", "metal", "punk", "grunge", "guitar").any { hay.contains(it) } -> "Rock / Alternativo"
                listOf("elec", "edm", "techno", "house", "synth", "trance", "dj", "remix").any { hay.contains(it) } -> "Electrónica / DJ"
                listOf("rap", "hip hop", "hip-hop", "trap", "urbano", "freestyle").any { hay.contains(it) } -> "Hip-Hop / Urbano"
                listOf("salsa", "cumbia", "bachata", "reggaeton", "latin", "mariachi", "corrido").any { hay.contains(it) } -> "Latino"
                listOf("jazz", "blues", "acoustic", "acustico", "chill", "lofi", "soul").any { hay.contains(it) } -> "Acústico / Jazz"
                listOf("piano", "classic", "clasica", "symphony", "orchestra", "instrumental", "ost").any { hay.contains(it) } -> "Instrumental / Clásica"
                listOf("pop", "hit", "dance", "disco").any { hay.contains(it) } -> "Pop"
                formatLabel.equals("FLAC", ignoreCase = true) || formatLabel.equals("WAV", ignoreCase = true) -> "Hi-Res ($formatLabel)"
                else -> "Música Local"
            }
        }

        fun buildArtistSummaries(tracks: List<MonthlyTrackStat>): List<ArtistStatSummary> =
            tracks.groupBy { it.artistName }.map { (artist, items) ->
                ArtistStatSummary(artist, items.sumOf { it.totalSeconds }, items.sumOf { it.playCount }, items.size)
            }.sortedWith(compareByDescending<ArtistStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })

        fun buildAlbumSummaries(tracks: List<MonthlyTrackStat>): List<AlbumStatSummary> =
            tracks.groupBy { it.albumName to it.artistName }.map { (aa, items) ->
                AlbumStatSummary(
                    albumName = aa.first,
                    artistName = aa.second,
                    albumId = items.firstOrNull { it.albumId > 0L }?.albumId ?: 0L,
                    totalSeconds = items.sumOf { it.totalSeconds },
                    totalPlays = items.sumOf { it.playCount },
                    trackCount = items.size
                )
            }.sortedWith(compareByDescending<AlbumStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })
    }
}

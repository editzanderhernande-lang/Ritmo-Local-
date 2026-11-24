package com.example.data.stats

import com.example.model.AlbumStatSummary
import com.example.model.ArtistStatSummary
import com.example.model.MonthlyTrackStat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object StatsAnalyticsEngine {

    /**
     * Play-counting rule: a track earns a qualified play only once the listener has
     * listened for at least min(30s, 45% of track duration), preventing 1-2s accidental skips
     * from inflating counts.
     */
    fun requiredSecondsForQualifiedPlay(durationMs: Long): Long {
        if (durationMs <= 0L) return 15L
        val durationSec = (durationMs / 1000L).coerceAtLeast(1L)
        val fractional = (durationSec * 0.45).toLong().coerceAtLeast(8L)
        return minOf(30L, fractional, durationSec)
    }

    fun buildDateKey(timestampMs: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestampMs))

    fun buildMonthKey(timestampMs: Long): String =
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date(timestampMs))

    fun yearFromTimestamp(timestampMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return cal.get(Calendar.YEAR)
    }

    fun hourFromTimestamp(timestampMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun computeRealTimeSummary(
        events: List<ListeningEventEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): RealTimeStatsSummary {
        val todayKey = buildDateKey(nowMs)
        val todayEvents = events.filter { it.dateKey == todayKey }
        val todaySeconds = todayEvents.sumOf { it.listenedSeconds }
        val todayPlays = todayEvents.count { it.isCompletedPlay }.toLong()
        val todayUniqueArtists = todayEvents.map { it.artistName }.filter { it.isNotBlank() }.distinct().size
        val todayUniqueAlbums = todayEvents.map { it.albumName to it.artistName }.distinct().size

        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayFmt = SimpleDateFormat("EEE", Locale.forLanguageTag("es-ES"))
        val weeklyBars = mutableListOf<ChartBarPoint>()
        var weeklyTotal = 0L
        for (offset in 6 downTo 0) {
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dk = buildDateKey(dayCal.timeInMillis)
            val label = dayFmt.format(dayCal.time).replaceFirstChar { it.uppercase() }.take(3)
            val dayEvents = events.filter { it.dateKey == dk }
            val sec = dayEvents.sumOf { it.listenedSeconds }
            val plays = dayEvents.count { it.isCompletedPlay }.toLong()
            weeklyTotal += sec
            weeklyBars.add(ChartBarPoint(dk, label, sec, plays))
        }

        val monthBars = events.groupBy { it.monthKey }
            .entries
            .sortedBy { it.key }
            .takeLast(6)
            .map { (mk, items) ->
                ChartBarPoint(
                    key = mk,
                    label = mk.takeLast(2) + "/" + mk.take(4).takeLast(2),
                    seconds = items.sumOf { it.listenedSeconds },
                    plays = items.count { it.isCompletedPlay }.toLong()
                )
            }

        return RealTimeStatsSummary(
            todaySeconds = todaySeconds,
            todayPlays = todayPlays,
            todayUniqueArtists = todayUniqueArtists,
            todayUniqueAlbums = todayUniqueAlbums,
            weeklyDailyBars = weeklyBars,
            monthlyBars = monthBars,
            weeklyTotalSeconds = weeklyTotal,
            allTimeSeconds = events.sumOf { it.listenedSeconds },
            allTimePlays = events.count { it.isCompletedPlay }.toLong(),
            earliestEventMs = events.minOfOrNull { it.timestampMs }
        )
    }

    fun computeRecapReport(
        events: List<ListeningEventEntity>,
        monthKey: String,
        favoriteGenresByTrackKey: Map<String, String> = emptyMap()
    ): RitmoRecapReport {
        val allMonths = (listOf(monthKey) + events.map { it.monthKey }).distinct().sortedDescending()
        val monthEvents = events.filter { it.monthKey == monthKey }
        val earliestMs = events.minOfOrNull { it.timestampMs }

        val topSongs = aggregateTrackStats(monthEvents, monthKey)
        val topArtists = aggregateArtistStats(topSongs)
        val topAlbums = aggregateAlbumStats(topSongs)
        val topGenres = aggregateGenreStats(monthEvents, favoriteGenresByTrackKey)

        val dayBuckets = monthEvents.groupBy { it.dateKey }
            .mapValues { (_, list) -> list.sumOf { it.listenedSeconds } }
        val peakDay = dayBuckets.maxByOrNull { it.value }

        val hourBuckets = monthEvents.groupBy { it.hourOfDay }
            .mapValues { (_, list) -> list.sumOf { it.listenedSeconds } }
        val usualHour = hourBuckets.maxByOrNull { it.value }?.key

        val mostRepeated = topSongs.maxWithOrNull(
            compareBy<MonthlyTrackStat> { it.playCount }.thenBy { it.totalSeconds }
        )

        val priorArtists = events
            .filter { it.monthKey < monthKey }
            .map { it.artistName.lowercase() }
            .toSet()
        val discoveredThisMonth = monthEvents
            .map { it.artistName }
            .filter { it.isNotBlank() && !it.equals("Artista desconocido", ignoreCase = true) }
            .distinct()
            .filter { it.lowercase() !in priorArtists }

        val prevMonthKey = previousMonthKey(monthKey)
        val prevEvents = events.filter { it.monthKey == prevMonthKey }
        val totalSeconds = monthEvents.sumOf { it.listenedSeconds }
        val totalPlays = monthEvents.count { it.isCompletedPlay }.toLong()
        val comparison = buildPeriodComparison(
            currentLabel = monthKey,
            previousLabel = prevMonthKey,
            currentSeconds = totalSeconds,
            previousSeconds = prevEvents.sumOf { it.listenedSeconds },
            currentPlays = totalPlays,
            previousPlays = prevEvents.count { it.isCompletedPlay }.toLong(),
            hasPriorData = prevEvents.isNotEmpty()
        )

        val dailyBars = monthEvents.groupBy { it.dateKey }
            .entries
            .sortedBy { it.key }
            .map { (dk, list) ->
                ChartBarPoint(
                    key = dk,
                    label = dk.takeLast(2),
                    seconds = list.sumOf { it.listenedSeconds },
                    plays = list.count { it.isCompletedPlay }.toLong()
                )
            }

        return RitmoRecapReport(
            monthKey = monthKey,
            availableMonths = allMonths,
            totalSeconds = totalSeconds,
            totalPlays = totalPlays,
            uniqueSongsCount = topSongs.size,
            uniqueArtistsCount = topArtists.size,
            topSong = topSongs.firstOrNull(),
            topArtist = topArtists.firstOrNull(),
            topAlbum = topAlbums.firstOrNull(),
            mostRepeatedSong = mostRepeated,
            top10Songs = topSongs.take(10),
            top10Artists = topArtists.take(10),
            topAlbums = topAlbums.take(10),
            topGenres = topGenres.take(6),
            peakDayDateKey = peakDay?.key,
            peakDaySeconds = peakDay?.value ?: 0L,
            usualHourOfDay = usualHour,
            newlyDiscoveredArtists = discoveredThisMonth.take(12),
            comparisonWithPreviousMonth = comparison,
            dailyBarsInMonth = dailyBars,
            hasEnoughData = monthEvents.isNotEmpty() && (totalSeconds >= 10L || totalPlays >= 1L),
            earliestDataMs = earliestMs
        )
    }

    fun computeWrappedReport(
        events: List<ListeningEventEntity>,
        year: Int,
        favoriteGenresByTrackKey: Map<String, String> = emptyMap()
    ): RitmoWrappedReport {
        val availableYears = (listOf(year) + events.map { it.year }).distinct().sortedDescending()
        val yearEvents = events.filter { it.year == year }
        val earliestMs = events.minOfOrNull { it.timestampMs }

        val topSongs = aggregateTrackStats(yearEvents, year.toString())
        val topArtists = aggregateArtistStats(topSongs)
        val topAlbums = aggregateAlbumStats(topSongs)
        val mainGenres = aggregateGenreStats(yearEvents, favoriteGenresByTrackKey)

        val monthNames = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val monthlyEvolution12 = (1..12).map { m ->
            val mk = String.format(Locale.US, "%04d-%02d", year, m)
            val mEvents = yearEvents.filter { it.monthKey == mk }
            ChartBarPoint(
                key = mk,
                label = monthNames[m - 1],
                seconds = mEvents.sumOf { it.listenedSeconds },
                plays = mEvents.count { it.isCompletedPlay }.toLong()
            )
        }
        val peakMonth = monthlyEvolution12.maxByOrNull { it.seconds }?.takeIf { it.seconds > 0L }

        val priorArtists = events.filter { it.year < year }.map { it.artistName.lowercase() }.toSet()
        val discoveryOfYear = topArtists.firstOrNull {
            !it.artistName.equals("Artista desconocido", ignoreCase = true) &&
                it.artistName.lowercase() !in priorArtists
        }?.artistName ?: topArtists.firstOrNull()?.artistName

        val prevYearEvents = events.filter { it.year == year - 1 }
        val totalSeconds = yearEvents.sumOf { it.listenedSeconds }
        val totalPlays = yearEvents.count { it.isCompletedPlay }.toLong()
        val comparison = buildPeriodComparison(
            currentLabel = year.toString(),
            previousLabel = (year - 1).toString(),
            currentSeconds = totalSeconds,
            previousSeconds = prevYearEvents.sumOf { it.listenedSeconds },
            currentPlays = totalPlays,
            previousPlays = prevYearEvents.count { it.isCompletedPlay }.toLong(),
            hasPriorData = prevYearEvents.isNotEmpty()
        )

        return RitmoWrappedReport(
            year = year,
            availableYears = availableYears,
            totalSeconds = totalSeconds,
            totalPlays = totalPlays,
            uniqueSongsCount = topSongs.size,
            uniqueArtistsCount = topArtists.size,
            topArtist = topArtists.firstOrNull(),
            topSong = topSongs.firstOrNull(),
            topAlbum = topAlbums.firstOrNull(),
            top20Songs = topSongs.take(20),
            top10Artists = topArtists.take(10),
            mainGenres = mainGenres.take(6),
            peakMonthKey = peakMonth?.key,
            peakMonthSeconds = peakMonth?.seconds ?: 0L,
            monthlyEvolution12 = monthlyEvolution12,
            discoveryOfYearArtist = discoveryOfYear,
            comparisonWithPreviousYear = comparison,
            hasEnoughData = yearEvents.isNotEmpty() && (totalSeconds >= 10L || totalPlays >= 1L),
            earliestDataMs = earliestMs
        )
    }

    /**
     * Achievements focus on healthy discovery, musical curiosity, favorites, and steady engagement—never
     * incentivizing extreme listening marathons or unhealthy screen time.
     */
    fun computeAchievements(
        events: List<ListeningEventEntity>,
        trackStats: List<TrackStatsEntity>,
        favoriteCount: Int,
        customPlaylistCount: Int
    ): List<MusicalAchievement> {
        val uniqueArtists = trackStats.filter { it.qualifiedPlays > 0 || it.totalSeconds >= 20L }
            .map { it.artistName }.filter { it.isNotBlank() }.distinct().size
        val uniqueAlbums = trackStats.filter { it.qualifiedPlays > 0 || it.totalSeconds >= 20L }
            .map { it.albumName to it.artistName }.distinct().size
        val uniqueGenres = trackStats.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct().size
        val maxPlaysOneSong = trackStats.maxOfOrNull { it.qualifiedPlays }?.toInt() ?: 0
        val maxPlaysOneArtist = trackStats.groupBy { it.artistName }
            .maxOfOrNull { (_, items) -> items.sumOf { it.qualifiedPlays } }?.toInt() ?: 0
        val distinctDays = events.map { it.dateKey }.distinct().size

        fun build(
            id: String,
            title: String,
            desc: String,
            cat: AchievementCategory,
            current: Int,
            target: Int
        ): MusicalAchievement {
            val safeTarget = target.coerceAtLeast(1)
            val unlocked = current >= safeTarget
            return MusicalAchievement(
                id = id,
                title = title,
                description = desc,
                category = cat,
                currentValue = current.coerceAtMost(safeTarget),
                targetValue = safeTarget,
                isUnlocked = unlocked,
                progressFraction = (current.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
            )
        }

        return listOf(
            build("first_note", "Primera Nota", "Completa tu primera reproducción verificada en Ritmo Local.", AchievementCategory.CONSISTENCY, trackStats.sumOf { it.qualifiedPlays }.toInt(), 1),
            build("discoverer", "Descubridor", "Escucha música de al menos 10 artistas diferentes.", AchievementCategory.EXPLORATION, uniqueArtists, 10),
            build("collector", "Coleccionista", "Disfruta canciones de al menos 10 álbumes distintos.", AchievementCategory.COLLECTION, uniqueAlbums, 10),
            build("favorite_repeat", "Repetición Favorita", "Reproduce tu canción favorita al menos 5 veces.", AchievementCategory.FAVORITES, maxPlaysOneSong, 5),
            build("genre_explorer", "Explorador Musical", "Explora al menos 4 géneros musicales diferentes.", AchievementCategory.EXPLORATION, uniqueGenres, 4),
            build("consistency", "Constancia Musical", "Disfruta de tu música en al menos 7 días distintos.", AchievementCategory.CONSISTENCY, distinctDays, 7),
            build("artist_fan", "Fan de un Artista", "Acumula 15 reproducciones de un mismo artista.", AchievementCategory.FAVORITES, maxPlaysOneArtist, 15),
            build("curator", "Curador de Listas", "Crea al menos 2 playlists o guarda 5 favoritas.", AchievementCategory.COLLECTION, customPlaylistCount + (favoriteCount / 3), 2)
        )
    }

    fun computeHistoryReport(
        events: List<ListeningEventEntity>,
        trackStats: List<TrackStatsEntity>,
        selectedMonthKey: String,
        selectedYear: Int,
        nowMs: Long = System.currentTimeMillis()
    ): MusicalHistoryReport {
        val availableMonths = (listOf(selectedMonthKey) + events.map { it.monthKey }).distinct().sortedDescending()
        val availableYears = (listOf(selectedYear) + events.map { it.year }).distinct().sortedDescending()
        val earliestMs = events.minOfOrNull { it.timestampMs }

        val sortedMonthsAsc = events.map { it.monthKey }.distinct().sorted()
        val seenArtists = mutableSetOf<String>()
        val timelineAsc = sortedMonthsAsc.map { mk ->
            val mEvents = events.filter { it.monthKey == mk }
            val topSongs = aggregateTrackStats(mEvents, mk)
            val topArtists = aggregateArtistStats(topSongs)
            val topAlbums = aggregateAlbumStats(topSongs)
            val topGenre = aggregateGenreStats(mEvents).firstOrNull()?.genre
            val monthArtists = mEvents.map { it.artistName }.filter { it.isNotBlank() }.distinct()
            val newOnes = monthArtists.filter { it.lowercase() !in seenArtists }
            monthArtists.forEach { seenArtists.add(it.lowercase()) }
            HistoryPeriodSnapshot(
                monthKey = mk,
                totalSeconds = mEvents.sumOf { it.listenedSeconds },
                totalPlays = mEvents.count { it.isCompletedPlay }.toLong(),
                topArtistName = topArtists.firstOrNull()?.artistName,
                topSongTitle = topSongs.firstOrNull()?.trackTitle,
                topAlbumName = topAlbums.firstOrNull()?.albumName,
                topGenre = topGenre,
                newArtistsDiscovered = newOnes.take(6)
            )
        }

        val currentMonthEvents = events.filter { it.monthKey == selectedMonthKey }
        val prevMonthKey = previousMonthKey(selectedMonthKey)
        val prevMonthEvents = events.filter { it.monthKey == prevMonthKey }
        val monthComparison = buildPeriodComparison(
            currentLabel = selectedMonthKey,
            previousLabel = prevMonthKey,
            currentSeconds = currentMonthEvents.sumOf { it.listenedSeconds },
            previousSeconds = prevMonthEvents.sumOf { it.listenedSeconds },
            currentPlays = currentMonthEvents.count { it.isCompletedPlay }.toLong(),
            previousPlays = prevMonthEvents.count { it.isCompletedPlay }.toLong(),
            hasPriorData = prevMonthEvents.isNotEmpty()
        )

        val curYearEvents = events.filter { it.year == selectedYear }
        val prevYearEvents = events.filter { it.year == selectedYear - 1 }
        val yearComparison = buildPeriodComparison(
            currentLabel = selectedYear.toString(),
            previousLabel = (selectedYear - 1).toString(),
            currentSeconds = curYearEvents.sumOf { it.listenedSeconds },
            previousSeconds = prevYearEvents.sumOf { it.listenedSeconds },
            currentPlays = curYearEvents.count { it.isCompletedPlay }.toLong(),
            previousPlays = prevYearEvents.count { it.isCompletedPlay }.toLong(),
            hasPriorData = prevYearEvents.isNotEmpty()
        )

        val firstHalf = curYearEvents.filter { it.monthKey.takeLast(2).toIntOrNull() in 1..6 }
        val secondHalf = curYearEvents.filter { it.monthKey.takeLast(2).toIntOrNull() in 7..12 }
        val yearHalvesComparison = buildPeriodComparison(
            currentLabel = "Jul–Dic $selectedYear",
            previousLabel = "Ene–Jun $selectedYear",
            currentSeconds = secondHalf.sumOf { it.listenedSeconds },
            previousSeconds = firstHalf.sumOf { it.listenedSeconds },
            currentPlays = secondHalf.count { it.isCompletedPlay }.toLong(),
            previousPlays = firstHalf.count { it.isCompletedPlay }.toLong(),
            hasPriorData = firstHalf.isNotEmpty() && secondHalf.isNotEmpty()
        )

        val milestones = mutableListOf<MusicalMilestone>()
        val firstChronologicalMonth = timelineAsc.firstOrNull()
        if (firstChronologicalMonth != null && !firstChronologicalMonth.topArtistName.isNullOrBlank()) {
            milestones.add(
                MusicalMilestone(
                    id = "first_top_artist",
                    title = "Tu primer artista #1",
                    description = "${firstChronologicalMonth.topArtistName} lideró tu primer período registrado (${firstChronologicalMonth.monthKey}).",
                    periodLabel = firstChronologicalMonth.monthKey,
                    timestampMs = earliestMs ?: nowMs
                )
            )
        }
        val firstCompletedEvent = events.firstOrNull { it.isCompletedPlay }
        if (firstCompletedEvent != null) {
            milestones.add(
                MusicalMilestone(
                    id = "first_recorded_song",
                    title = "El inicio de tu archivo sonoro",
                    description = "Primera canción registrada: «${firstCompletedEvent.trackTitle}» de ${firstCompletedEvent.artistName}.",
                    periodLabel = firstCompletedEvent.dateKey,
                    timestampMs = firstCompletedEvent.timestampMs
                )
            )
        }
        val totalAllTimePlays = trackStats.sumOf { it.qualifiedPlays }
        if (totalAllTimePlays >= 25L) {
            milestones.add(
                MusicalMilestone(
                    id = "plays_25",
                    title = "25 reproducciones verificadas",
                    description = "Tu biblioteca personal ya acumula $totalAllTimePlays sesiones musicales reales.",
                    periodLabel = selectedMonthKey,
                    timestampMs = nowMs
                )
            )
        }

        val twentyOneDaysMs = 21L * 24L * 60L * 60L * 1000L
        val forgotten = trackStats
            .filter { it.qualifiedPlays >= 2L && (nowMs - it.lastPlayedAtMs) >= twentyOneDaysMs }
            .sortedByDescending { it.qualifiedPlays }
            .take(10)
            .map { stat ->
                ForgottenTrackEntry(
                    trackKey = stat.trackKey,
                    trackTitle = stat.trackTitle,
                    artistName = stat.artistName,
                    albumId = stat.albumId,
                    qualifiedPlays = stat.qualifiedPlays,
                    lastPlayedAtMs = stat.lastPlayedAtMs,
                    daysSinceLastPlay = ((nowMs - stat.lastPlayedAtMs) / (24L * 60L * 60L * 1000L)).toInt()
                )
            }

        val loyalArtists = events
            .filter { it.artistName.isNotBlank() && !it.artistName.equals("Artista desconocido", ignoreCase = true) }
            .groupBy { it.artistName }
            .map { (artist, items) ->
                LoyalArtistEntry(
                    artistName = artist,
                    activeMonthsCount = items.map { it.monthKey }.distinct().size,
                    totalPlays = items.count { it.isCompletedPlay }.toLong(),
                    totalSeconds = items.sumOf { it.listenedSeconds }
                )
            }
            .sortedWith(compareByDescending<LoyalArtistEntry> { it.activeMonthsCount }.thenByDescending { it.totalSeconds })
            .take(8)

        return MusicalHistoryReport(
            selectedMonthKey = selectedMonthKey,
            selectedYear = selectedYear,
            availableMonths = availableMonths,
            availableYears = availableYears,
            timelinePeriods = timelineAsc.reversed(),
            monthComparison = monthComparison,
            yearComparison = yearComparison,
            yearHalvesComparison = yearHalvesComparison,
            milestones = milestones,
            forgottenTracks = forgotten,
            loyalArtists = loyalArtists,
            earliestDataMs = earliestMs,
            hasEnoughHistory = events.isNotEmpty()
        )
    }

    fun exportAllStatsJson(events: List<ListeningEventEntity>, trackStats: List<TrackStatsEntity>): String {
        val root = JSONObject()
        root.put("format", "ritmo_local_stats_export_v1")
        root.put("exportedAtMs", System.currentTimeMillis())
        root.put("totalEvents", events.size)
        root.put("totalTracksTracked", trackStats.size)

        val tracksArr = JSONArray()
        trackStats.forEach { t ->
            tracksArr.put(
                JSONObject().apply {
                    put("trackKey", t.trackKey)
                    put("title", t.trackTitle)
                    put("artist", t.artistName)
                    put("album", t.albumName)
                    put("genre", t.genre)
                    put("qualifiedPlays", t.qualifiedPlays)
                    put("totalSeconds", t.totalSeconds)
                    put("firstPlayedAtMs", t.firstPlayedAtMs)
                    put("lastPlayedAtMs", t.lastPlayedAtMs)
                }
            )
        }
        root.put("trackStats", tracksArr)

        val eventsArr = JSONArray()
        events.takeLast(1000).forEach { e ->
            eventsArr.put(
                JSONObject().apply {
                    put("trackKey", e.trackKey)
                    put("dateKey", e.dateKey)
                    put("monthKey", e.monthKey)
                    put("seconds", e.listenedSeconds)
                    put("isCompletedPlay", e.isCompletedPlay)
                    put("timestampMs", e.timestampMs)
                }
            )
        }
        root.put("recentEvents", eventsArr)
        return root.toString(2)
    }

    private fun aggregateTrackStats(events: List<ListeningEventEntity>, periodKey: String): List<MonthlyTrackStat> {
        return events.groupBy { it.trackKey }
            .map { (tk, list) ->
                val sample = list.last()
                MonthlyTrackStat(
                    id = tk,
                    userId = "local",
                    monthKey = periodKey,
                    trackKey = tk,
                    trackTitle = sample.trackTitle,
                    artistName = sample.artistName,
                    albumName = sample.albumName,
                    albumId = list.firstOrNull { it.albumId > 0L }?.albumId ?: 0L,
                    playCount = list.count { it.isCompletedPlay }.toLong(),
                    totalSeconds = list.sumOf { it.listenedSeconds }
                )
            }
            .sortedWith(compareByDescending<MonthlyTrackStat> { it.totalSeconds }.thenByDescending { it.playCount })
    }

    private fun aggregateArtistStats(songs: List<MonthlyTrackStat>): List<ArtistStatSummary> {
        return songs.groupBy { it.artistName }
            .map { (artist, items) ->
                ArtistStatSummary(
                    artistName = artist,
                    totalSeconds = items.sumOf { it.totalSeconds },
                    totalPlays = items.sumOf { it.playCount },
                    trackCount = items.size
                )
            }
            .sortedWith(compareByDescending<ArtistStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })
    }

    private fun aggregateAlbumStats(songs: List<MonthlyTrackStat>): List<AlbumStatSummary> {
        return songs.groupBy { it.albumName to it.artistName }
            .map { (aa, items) ->
                AlbumStatSummary(
                    albumName = aa.first,
                    artistName = aa.second,
                    albumId = items.firstOrNull { it.albumId > 0L }?.albumId ?: 0L,
                    totalSeconds = items.sumOf { it.totalSeconds },
                    totalPlays = items.sumOf { it.playCount },
                    trackCount = items.size
                )
            }
            .sortedWith(compareByDescending<AlbumStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })
    }

    private fun aggregateGenreStats(
        events: List<ListeningEventEntity>,
        genreOverrides: Map<String, String> = emptyMap()
    ): List<GenreStatSummary> {
        val totalSec = events.sumOf { it.listenedSeconds }.coerceAtLeast(1L)
        return events.mapNotNull { ev ->
            val g = genreOverrides[ev.trackKey]?.takeIf { it.isNotBlank() } ?: ev.genre.takeIf { it.isNotBlank() }
            g?.let { it to ev }
        }
            .groupBy { it.first }
            .map { (genre, pairs) ->
                val sec = pairs.sumOf { it.second.listenedSeconds }
                GenreStatSummary(
                    genre = genre,
                    totalSeconds = sec,
                    totalPlays = pairs.count { it.second.isCompletedPlay }.toLong(),
                    percentage = (sec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.totalSeconds }
    }

    private fun buildPeriodComparison(
        currentLabel: String,
        previousLabel: String,
        currentSeconds: Long,
        previousSeconds: Long,
        currentPlays: Long,
        previousPlays: Long,
        hasPriorData: Boolean
    ): PeriodComparisonSummary {
        if (!hasPriorData) {
            return PeriodComparisonSummary(
                hasPriorPeriodData = false,
                currentLabel = currentLabel,
                previousLabel = previousLabel,
                currentSeconds = currentSeconds,
                previousSeconds = 0L,
                currentPlays = currentPlays,
                previousPlays = 0L,
                deltaPercentSeconds = 0,
                summaryMessage = "Aún no hay suficientes datos en $previousLabel para comparar."
            )
        }
        val deltaPct = if (previousSeconds > 0L) {
            (((currentSeconds - previousSeconds).toDouble() / previousSeconds.toDouble()) * 100.0).roundToInt()
        } else 0
        val message = when {
            deltaPct > 5 -> "Escuchaste un +$deltaPct% más de música respecto a $previousLabel."
            deltaPct < -5 -> "Tu escucha bajó un ${-deltaPct}% frente a $previousLabel."
            else -> "Mantuviste un ritmo similar respecto a $previousLabel."
        }
        return PeriodComparisonSummary(
            hasPriorPeriodData = true,
            currentLabel = currentLabel,
            previousLabel = previousLabel,
            currentSeconds = currentSeconds,
            previousSeconds = previousSeconds,
            currentPlays = currentPlays,
            previousPlays = previousPlays,
            deltaPercentSeconds = deltaPct,
            summaryMessage = message
        )
    }

    fun previousMonthKey(monthKey: String): String {
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return monthKey
        return if (month <= 1) {
            String.format(Locale.US, "%04d-12", year - 1)
        } else {
            String.format(Locale.US, "%04d-%02d", year, month - 1)
        }
    }
}

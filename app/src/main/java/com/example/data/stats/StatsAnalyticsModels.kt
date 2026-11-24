package com.example.data.stats

import com.example.model.AlbumStatSummary
import com.example.model.ArtistStatSummary
import com.example.model.MonthlyTrackStat

data class ChartBarPoint(
    val key: String,
    val label: String,
    val seconds: Long,
    val plays: Long
)

data class GenreStatSummary(
    val genre: String,
    val totalSeconds: Long,
    val totalPlays: Long,
    val percentage: Float
)

data class PeriodComparisonSummary(
    val hasPriorPeriodData: Boolean,
    val currentLabel: String,
    val previousLabel: String,
    val currentSeconds: Long,
    val previousSeconds: Long,
    val currentPlays: Long,
    val previousPlays: Long,
    val deltaPercentSeconds: Int,
    val summaryMessage: String
)

data class RealTimeStatsSummary(
    val todaySeconds: Long,
    val todayPlays: Long,
    val todayUniqueArtists: Int,
    val todayUniqueAlbums: Int,
    val weeklyDailyBars: List<ChartBarPoint>,
    val monthlyBars: List<ChartBarPoint>,
    val weeklyTotalSeconds: Long,
    val allTimeSeconds: Long,
    val allTimePlays: Long,
    val earliestEventMs: Long?
)

data class RitmoRecapReport(
    val monthKey: String,
    val availableMonths: List<String>,
    val totalSeconds: Long,
    val totalPlays: Long,
    val uniqueSongsCount: Int,
    val uniqueArtistsCount: Int,
    val topSong: MonthlyTrackStat?,
    val topArtist: ArtistStatSummary?,
    val topAlbum: AlbumStatSummary?,
    val mostRepeatedSong: MonthlyTrackStat?,
    val top10Songs: List<MonthlyTrackStat>,
    val top10Artists: List<ArtistStatSummary>,
    val topAlbums: List<AlbumStatSummary>,
    val topGenres: List<GenreStatSummary>,
    val peakDayDateKey: String?,
    val peakDaySeconds: Long,
    val usualHourOfDay: Int?,
    val newlyDiscoveredArtists: List<String>,
    val comparisonWithPreviousMonth: PeriodComparisonSummary,
    val dailyBarsInMonth: List<ChartBarPoint>,
    val hasEnoughData: Boolean,
    val earliestDataMs: Long?
)

data class RitmoWrappedReport(
    val year: Int,
    val availableYears: List<Int>,
    val totalSeconds: Long,
    val totalPlays: Long,
    val uniqueSongsCount: Int,
    val uniqueArtistsCount: Int,
    val topArtist: ArtistStatSummary?,
    val topSong: MonthlyTrackStat?,
    val topAlbum: AlbumStatSummary?,
    val top20Songs: List<MonthlyTrackStat>,
    val top10Artists: List<ArtistStatSummary>,
    val mainGenres: List<GenreStatSummary>,
    val peakMonthKey: String?,
    val peakMonthSeconds: Long,
    val monthlyEvolution12: List<ChartBarPoint>,
    val discoveryOfYearArtist: String?,
    val comparisonWithPreviousYear: PeriodComparisonSummary,
    val hasEnoughData: Boolean,
    val earliestDataMs: Long?
)

enum class AchievementCategory(val displayName: String) {
    EXPLORATION("Exploración"),
    COLLECTION("Colección"),
    CONSISTENCY("Constancia"),
    FAVORITES("Favoritos")
}

data class MusicalAchievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val currentValue: Int,
    val targetValue: Int,
    val isUnlocked: Boolean,
    val progressFraction: Float
)

data class MusicalMilestone(
    val id: String,
    val title: String,
    val description: String,
    val periodLabel: String,
    val timestampMs: Long
)

data class HistoryPeriodSnapshot(
    val monthKey: String,
    val totalSeconds: Long,
    val totalPlays: Long,
    val topArtistName: String?,
    val topSongTitle: String?,
    val topAlbumName: String?,
    val topGenre: String?,
    val newArtistsDiscovered: List<String>
)

data class ForgottenTrackEntry(
    val trackKey: String,
    val trackTitle: String,
    val artistName: String,
    val albumId: Long,
    val qualifiedPlays: Long,
    val lastPlayedAtMs: Long,
    val daysSinceLastPlay: Int
)

data class LoyalArtistEntry(
    val artistName: String,
    val activeMonthsCount: Int,
    val totalPlays: Long,
    val totalSeconds: Long
)

data class MusicalHistoryReport(
    val selectedMonthKey: String,
    val selectedYear: Int,
    val availableMonths: List<String>,
    val availableYears: List<Int>,
    val timelinePeriods: List<HistoryPeriodSnapshot>,
    val monthComparison: PeriodComparisonSummary,
    val yearComparison: PeriodComparisonSummary,
    val yearHalvesComparison: PeriodComparisonSummary,
    val milestones: List<MusicalMilestone>,
    val forgottenTracks: List<ForgottenTrackEntry>,
    val loyalArtists: List<LoyalArtistEntry>,
    val earliestDataMs: Long?,
    val hasEnoughHistory: Boolean
)

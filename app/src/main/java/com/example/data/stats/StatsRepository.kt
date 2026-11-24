package com.example.data.stats

import android.content.Context
import com.example.model.LocalTrack
import com.example.model.TimeOfDaySlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StatsRepository(context: Context) {

    private val dao = RitmoStatsDatabase.getInstance(context).statsDao()

    val allEvents: Flow<List<ListeningEventEntity>> = dao.observeAllEvents()
    val allTrackStats: Flow<List<TrackStatsEntity>> = dao.observeAllTrackStats()
    val metadataOverrides: Flow<Map<String, TrackMetadataOverrideEntity>> =
        dao.observeMetadataOverrides().map { list -> list.associateBy { it.trackKey } }
    val savedWrappedYears: Flow<List<SavedWrappedEntity>> = dao.observeSavedWrappedYears()

    suspend fun recordListening(
        track: LocalTrack,
        deltaSeconds: Long,
        qualifiedPlay: Boolean,
        slot: TimeOfDaySlot,
        timestampMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        dao.recordListeningActivity(
            trackKey = track.trackKey,
            trackTitle = track.title,
            artistName = track.artist,
            albumName = track.album,
            albumId = track.albumId,
            genre = track.genre,
            durationMs = track.durationMs,
            deltaSeconds = deltaSeconds,
            qualifiedPlay = qualifiedPlay,
            timestampMs = timestampMs,
            dateKey = StatsAnalyticsEngine.buildDateKey(timestampMs),
            monthKey = StatsAnalyticsEngine.buildMonthKey(timestampMs),
            year = StatsAnalyticsEngine.yearFromTimestamp(timestampMs),
            hourOfDay = StatsAnalyticsEngine.hourFromTimestamp(timestampMs),
            timeSlot = slot.wireValue
        )
    }

    suspend fun saveWrappedSnapshot(year: Int, summaryJson: String) = withContext(Dispatchers.IO) {
        dao.upsertSavedWrapped(
            SavedWrappedEntity(
                year = year,
                generatedAtMs = System.currentTimeMillis(),
                summaryJson = summaryJson
            )
        )
    }

    suspend fun saveMetadataOverride(
        trackKey: String,
        title: String,
        artist: String,
        album: String,
        genre: String,
        hiddenAsDuplicate: Boolean = false
    ) = withContext(Dispatchers.IO) {
        dao.upsertMetadataOverride(
            TrackMetadataOverrideEntity(
                trackKey = trackKey,
                title = title.trim().take(180),
                artist = artist.trim().take(180),
                album = album.trim().take(180),
                genre = genre.trim().take(60),
                hiddenAsDuplicate = hiddenAsDuplicate,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun setDuplicateHidden(track: LocalTrack, hidden: Boolean) = withContext(Dispatchers.IO) {
        val current = dao.getAllMetadataOverrides().firstOrNull { it.trackKey == track.trackKey }
        dao.upsertMetadataOverride(
            TrackMetadataOverrideEntity(
                trackKey = track.trackKey,
                title = current?.title ?: track.title,
                artist = current?.artist ?: track.artist,
                album = current?.album ?: track.album,
                genre = current?.genre ?: track.genre,
                hiddenAsDuplicate = hidden,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearMetadataOverride(trackKey: String) = withContext(Dispatchers.IO) {
        dao.deleteMetadataOverride(trackKey)
    }

    suspend fun getAllMetadataOverrides(): Map<String, TrackMetadataOverrideEntity> = withContext(Dispatchers.IO) {
        dao.getAllMetadataOverrides().associateBy { it.trackKey }
    }

    suspend fun exportStatsJson(): String = withContext(Dispatchers.IO) {
        val events = dao.getAllEventsAscending()
        val stats = dao.getAllTrackStats()
        StatsAnalyticsEngine.exportAllStatsJson(events, stats)
    }

    suspend fun clearAllListeningHistory() = withContext(Dispatchers.IO) {
        dao.clearAllListeningHistory()
    }
}

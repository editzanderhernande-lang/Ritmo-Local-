package com.example.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RitmoStatsDao {

    @Query("SELECT * FROM listening_events ORDER BY timestampMs DESC")
    fun observeAllEvents(): Flow<List<ListeningEventEntity>>

    @Query("SELECT * FROM listening_events ORDER BY timestampMs ASC")
    suspend fun getAllEventsAscending(): List<ListeningEventEntity>

    @Query("SELECT * FROM listening_events WHERE syncedToCloud = 0 ORDER BY timestampMs ASC LIMIT :limit")
    suspend fun getUnsyncedEvents(limit: Int = 100): List<ListeningEventEntity>

    @Query("UPDATE listening_events SET syncedToCloud = 1 WHERE id IN (:ids)")
    suspend fun markEventsSynced(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListeningEvent(event: ListeningEventEntity): Long

    @Query("SELECT * FROM track_stats ORDER BY lastPlayedAtMs DESC")
    fun observeAllTrackStats(): Flow<List<TrackStatsEntity>>

    @Query("SELECT * FROM track_stats")
    suspend fun getAllTrackStats(): List<TrackStatsEntity>

    @Query("SELECT * FROM track_stats WHERE trackKey = :trackKey LIMIT 1")
    suspend fun getTrackStat(trackKey: String): TrackStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrackStat(stat: TrackStatsEntity)

    @Transaction
    suspend fun recordListeningActivity(
        trackKey: String,
        trackTitle: String,
        artistName: String,
        albumName: String,
        albumId: Long,
        genre: String,
        durationMs: Long,
        deltaSeconds: Long,
        qualifiedPlay: Boolean,
        timestampMs: Long,
        dateKey: String,
        monthKey: String,
        year: Int,
        hourOfDay: Int,
        timeSlot: String
    ) {
        val safeDelta = deltaSeconds.coerceAtLeast(0L)
        if (safeDelta == 0L && !qualifiedPlay) return

        insertListeningEvent(
            ListeningEventEntity(
                trackKey = trackKey,
                trackTitle = trackTitle,
                artistName = artistName,
                albumName = albumName,
                albumId = albumId,
                genre = genre,
                durationMs = durationMs,
                listenedSeconds = safeDelta,
                isCompletedPlay = qualifiedPlay,
                timestampMs = timestampMs,
                dateKey = dateKey,
                monthKey = monthKey,
                year = year,
                hourOfDay = hourOfDay,
                timeSlot = timeSlot,
                syncedToCloud = false
            )
        )

        val existing = getTrackStat(trackKey)
        if (existing == null) {
            upsertTrackStat(
                TrackStatsEntity(
                    trackKey = trackKey,
                    trackTitle = trackTitle,
                    artistName = artistName,
                    albumName = albumName,
                    albumId = albumId,
                    genre = genre,
                    firstPlayedAtMs = timestampMs,
                    lastPlayedAtMs = timestampMs,
                    totalSeconds = safeDelta,
                    qualifiedPlays = if (qualifiedPlay) 1L else 0L
                )
            )
        } else {
            upsertTrackStat(
                existing.copy(
                    trackTitle = trackTitle,
                    artistName = artistName,
                    albumName = albumName,
                    albumId = if (albumId > 0L) albumId else existing.albumId,
                    genre = genre.ifBlank { existing.genre },
                    lastPlayedAtMs = maxOf(existing.lastPlayedAtMs, timestampMs),
                    totalSeconds = existing.totalSeconds + safeDelta,
                    qualifiedPlays = existing.qualifiedPlays + if (qualifiedPlay) 1L else 0L
                )
            )
        }
    }

    @Query("SELECT * FROM saved_wrapped_years ORDER BY year DESC")
    fun observeSavedWrappedYears(): Flow<List<SavedWrappedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedWrapped(entity: SavedWrappedEntity)

    @Query("SELECT * FROM track_metadata_overrides")
    fun observeMetadataOverrides(): Flow<List<TrackMetadataOverrideEntity>>

    @Query("SELECT * FROM track_metadata_overrides")
    suspend fun getAllMetadataOverrides(): List<TrackMetadataOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadataOverride(entity: TrackMetadataOverrideEntity)

    @Query("DELETE FROM track_metadata_overrides WHERE trackKey = :trackKey")
    suspend fun deleteMetadataOverride(trackKey: String)

    @Transaction
    suspend fun clearAllListeningHistory() {
        deleteAllEvents()
        deleteAllTrackStats()
        deleteAllSavedWrapped()
    }

    @Query("DELETE FROM listening_events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM track_stats")
    suspend fun deleteAllTrackStats()

    @Query("DELETE FROM saved_wrapped_years")
    suspend fun deleteAllSavedWrapped()
}

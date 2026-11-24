package com.example.data.stats

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "listening_events",
    indices = [
        Index(value = ["monthKey"]),
        Index(value = ["year"]),
        Index(value = ["dateKey"]),
        Index(value = ["trackKey"]),
        Index(value = ["syncedToCloud"])
    ]
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackKey: String,
    val trackTitle: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val genre: String = "",
    val durationMs: Long = 0L,
    val listenedSeconds: Long = 0L,
    val isCompletedPlay: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis(),
    val dateKey: String,   // YYYY-MM-DD
    val monthKey: String,  // YYYY-MM
    val year: Int,
    val hourOfDay: Int,    // 0..23
    val timeSlot: String,  // MORNING, AFTERNOON, NIGHT
    val syncedToCloud: Boolean = false
)

@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val trackKey: String,
    val trackTitle: String,
    val artistName: String,
    val albumName: String,
    val albumId: Long,
    val genre: String = "",
    val firstPlayedAtMs: Long,
    val lastPlayedAtMs: Long,
    val totalSeconds: Long,
    val qualifiedPlays: Long
)

@Entity(tableName = "saved_wrapped_years")
data class SavedWrappedEntity(
    @PrimaryKey val year: Int,
    val generatedAtMs: Long,
    val summaryJson: String
)

@Entity(tableName = "track_metadata_overrides")
data class TrackMetadataOverrideEntity(
    @PrimaryKey val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = "",
    val hiddenAsDuplicate: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)

package com.example.library

import com.example.data.stats.TrackStatsEntity
import com.example.model.LocalTrack

enum class LibrarySortOption(val id: String, val label: String) {
    TITLE_ASC("title_asc", "Título (A-Z)"),
    ARTIST_ASC("artist_asc", "Artista (A-Z)"),
    RECENTLY_ADDED("recent", "Más recientes"),
    DURATION_DESC("duration_desc", "Mayor duración"),
    MOST_PLAYED("most_played", "Más reproducidas")
}

data class LibraryFilterCriteria(
    val query: String = "",
    val selectedGenre: String = "",
    val selectedFormat: String = "",
    val onlyMissingArtwork: Boolean = false,
    val onlyUnplayed: Boolean = false,
    val minDurationSec: Int = 0,
    val sortOption: LibrarySortOption = LibrarySortOption.TITLE_ASC
)

data class DuplicateTrackGroup(
    val normalizedSignature: String,
    val displayTitle: String,
    val displayArtist: String,
    val tracks: List<LocalTrack>
)

data class GenreGroup(
    val genreName: String,
    val tracks: List<LocalTrack>
)

object LibraryOrganizerEngine {

    fun normalizeForMatch(value: String): String {
        val decomposed = java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
        return decomposed
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("\\(.*?\\)|\\[.*?]"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    /**
     * Detects duplicate tracks safely by normalized title + artist (and close duration).
     * Never deletes physical files; returns groups so the user can review and confirm hiding extras.
     */
    fun findDuplicateGroups(tracks: List<LocalTrack>): List<DuplicateTrackGroup> {
        val groupedBySignature = tracks.groupBy { track ->
            "${normalizeForMatch(track.title)}|${normalizeForMatch(track.artist)}"
        }
        val result = mutableListOf<DuplicateTrackGroup>()
        groupedBySignature.forEach { (sig, list) ->
            if (sig.isNotBlank() && !sig.startsWith("|") && list.size > 1) {
                val sample = list.first()
                result.add(
                    DuplicateTrackGroup(
                        normalizedSignature = sig,
                        displayTitle = sample.title,
                        displayArtist = sample.artist,
                        tracks = list.sortedByDescending { (it.albumArtUri != null).compareTo(false) }
                    )
                )
            }
        }
        return result.sortedBy { it.displayTitle.lowercase() }
    }

    fun findTracksMissingCover(tracks: List<LocalTrack>): List<LocalTrack> =
        tracks.filter { it.albumArtUri.isNullOrBlank() }

    fun buildGenreGroups(tracks: List<LocalTrack>): List<GenreGroup> {
        return tracks
            .groupBy { it.genre.trim().ifBlank { "Sin género asignado" } }
            .map { (genre, items) ->
                GenreGroup(genreName = genre, tracks = items.sortedBy { it.title.lowercase() })
            }
            .sortedByDescending { it.tracks.size }
    }

    fun availableGenres(tracks: List<LocalTrack>): List<String> =
        tracks.map { it.genre.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    fun availableFormats(tracks: List<LocalTrack>): List<String> =
        tracks.map { it.formatLabel.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    fun filterAndSort(
        tracks: List<LocalTrack>,
        criteria: LibraryFilterCriteria,
        trackStatsByKey: Map<String, TrackStatsEntity> = emptyMap()
    ): List<LocalTrack> {
        val q = criteria.query.trim().lowercase()
        val filtered = tracks.filter { track ->
            val matchesQuery = q.isBlank() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q) ||
                track.genre.lowercase().contains(q)

            val matchesGenre = criteria.selectedGenre.isBlank() ||
                track.genre.equals(criteria.selectedGenre, ignoreCase = true) ||
                (criteria.selectedGenre == "Sin género asignado" && track.genre.isBlank())

            val matchesFormat = criteria.selectedFormat.isBlank() ||
                track.formatLabel.equals(criteria.selectedFormat, ignoreCase = true)

            val matchesArtwork = !criteria.onlyMissingArtwork || track.albumArtUri.isNullOrBlank()
            val plays = trackStatsByKey[track.trackKey]?.qualifiedPlays ?: 0L
            val matchesUnplayed = !criteria.onlyUnplayed || plays == 0L
            val matchesMinDuration = track.durationMs >= criteria.minDurationSec * 1000L

            matchesQuery && matchesGenre && matchesFormat && matchesArtwork && matchesUnplayed && matchesMinDuration
        }

        return when (criteria.sortOption) {
            LibrarySortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.ARTIST_ASC -> filtered.sortedWith(
                compareBy<LocalTrack> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            LibrarySortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAdded }
            LibrarySortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            LibrarySortOption.MOST_PLAYED -> filtered.sortedByDescending {
                trackStatsByKey[it.trackKey]?.qualifiedPlays ?: 0L
            }
        }
    }
}

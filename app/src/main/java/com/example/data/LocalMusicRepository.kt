package com.example.data

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.stats.TrackMetadataOverrideEntity
import com.example.model.AlbumPlaylist
import com.example.model.ArtistPlaylist
import com.example.model.GeneratedPlaylist
import com.example.model.LocalTrack
import com.example.model.TimeOfDaySlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

private val Context.musicPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ritmolocal_prefs")

data class DownloadedTrackSidecar(
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val artworkUri: String?,
    val formatLabel: String,
    val sourceName: String = "",
    val licenseName: String = ""
)

class LocalMusicRepository(private val context: Context) {

    private val importedUrisKey = stringSetPreferencesKey("imported_audio_uris")

    suspend fun loadLocalTracks(
        hideShortOrWhatsApp: Boolean = false,
        coverOverrides: Map<String, String> = emptyMap(),
        metadataOverrides: Map<String, TrackMetadataOverrideEntity> = emptyMap()
    ): List<LocalTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalTrack>()
        val seenKeys = mutableSetOf<String>()
        val genreByAudioId = loadGenreMapByAudioId()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Pista sin título"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Artista desconocido" else rawArtist
                    val rawAlbum = cursor.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") "Álbum desconocido" else rawAlbum
                    val albumId = cursor.getLong(albumIdCol).coerceAtLeast(0L)
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(dateCol)
                    val mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val filePath = if (dataCol >= 0) cursor.getString(dataCol) else null

                    if (hideShortOrWhatsApp && isShortOrVoiceNoteAudio(durationMs, title, displayName, filePath, album)) {
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()
                    val systemArtUri = if (albumId > 0L) {
                        Uri.parse("content://media/external/audio/albumart/$albumId").toString()
                    } else null

                    val trackKey = buildTrackKey(title, artist, durationMs)
                    val override = metadataOverrides[trackKey]
                    if (override?.hiddenAsDuplicate == true) {
                        continue
                    }
                    val finalTitle = override?.title?.takeIf { it.isNotBlank() } ?: title
                    val finalArtist = override?.artist?.takeIf { it.isNotBlank() } ?: artist
                    val finalAlbum = override?.album?.takeIf { it.isNotBlank() } ?: album
                    val finalGenre = override?.genre?.takeIf { it.isNotBlank() }
                        ?: genreByAudioId[id].orEmpty()

                    val customArt = coverOverrides[trackKey]
                        ?: coverOverrides[CoverArtRepository.albumOverrideKey(finalAlbum, finalArtist)]
                    if (seenKeys.add(trackKey)) {
                        tracks.add(
                            LocalTrack(
                                id = id,
                                trackKey = trackKey,
                                title = finalTitle.take(180),
                                artist = finalArtist.take(180),
                                album = finalAlbum.take(180),
                                albumId = albumId,
                                durationMs = durationMs,
                                contentUri = contentUri,
                                albumArtUri = customArt ?: systemArtUri,
                                dateAdded = dateAdded,
                                formatLabel = resolveAudioFormatLabel(mimeType, displayName),
                                genre = finalGenre.take(60)
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            // Audio permission not granted yet; imported/shared tracks below still load
        } catch (_: Exception) {
            // Gracefully handle unavailable MediaStore
        }

        // Load authorized catalog tracks downloaded previously (preserved so existing user music never disappears)
        val downloadsDir = getDownloadsMusicDir(context)
        downloadsDir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".") && !file.name.endsWith(".part") && file.length() > 0L) {
                val sidecar = readDownloadedTrackSidecar(context, file.name)
                val track = extractDownloadedTrack(file, sidecar, coverOverrides)
                    ?.let { applyOverride(it, metadataOverrides) }
                if (track != null && seenKeys.add(track.trackKey)) {
                    tracks.add(track)
                }
            }
        }

        // Load SAF-imported audio files persisted in DataStore
        val prefs = context.musicPrefsDataStore.data.first()
        val savedUris = prefs[importedUrisKey].orEmpty()
        savedUris.forEach { uriStr ->
            val track = extractTrackFromUri(Uri.parse(uriStr), coverOverrides)
                ?.let { applyOverride(it, metadataOverrides) }
            if (track != null &&
                !(hideShortOrWhatsApp && isShortOrVoiceNoteAudio(track.durationMs, track.title, null, uriStr, track.album)) &&
                seenKeys.add(track.trackKey)
            ) {
                tracks.add(track)
            }
        }

        // Load audio files downloaded from Global Regional Chat or restored backups
        val sharedDir = getSharedDownloadsDir(context)
        sharedDir.listFiles()?.forEach { file ->
            if (file.isFile && file.length() > 0L) {
                val track = extractTrackFromUri(Uri.fromFile(file), coverOverrides)
                    ?.let { applyOverride(it, metadataOverrides) }
                if (track != null && seenKeys.add(track.trackKey)) {
                    tracks.add(track)
                }
            }
        }

        tracks.sortedBy { it.title.lowercase() }
    }

    private fun applyOverride(
        track: LocalTrack,
        overrides: Map<String, TrackMetadataOverrideEntity>
    ): LocalTrack? {
        val ov = overrides[track.trackKey] ?: return track
        if (ov.hiddenAsDuplicate) return null
        return track.copy(
            title = ov.title.takeIf { it.isNotBlank() } ?: track.title,
            artist = ov.artist.takeIf { it.isNotBlank() } ?: track.artist,
            album = ov.album.takeIf { it.isNotBlank() } ?: track.album,
            genre = ov.genre.takeIf { it.isNotBlank() } ?: track.genre
        )
    }

    private fun loadGenreMapByAudioId(): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.GENRE),
                    null,
                    null,
                    null
                )?.use { c ->
                    val idIdx = c.getColumnIndex(MediaStore.Audio.Media._ID)
                    val genreIdx = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    if (idIdx >= 0 && genreIdx >= 0) {
                        while (c.moveToNext()) {
                            val g = c.getString(genreIdx)?.trim()
                            if (!g.isNullOrBlank() && g != "<unknown>") {
                                map[c.getLong(idIdx)] = g
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    suspend fun saveImportedAudioUris(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext 0
        val validUris = mutableSetOf<String>()
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Temporary permission granted
            }
            validUris.add(uri.toString())
        }
        context.musicPrefsDataStore.edit { prefs ->
            val existing = prefs[importedUrisKey].orEmpty()
            prefs[importedUrisKey] = existing + validUris
        }
        validUris.size
    }

    private fun extractDownloadedTrack(
        file: File,
        sidecar: DownloadedTrackSidecar?,
        coverOverrides: Map<String, String>
    ): LocalTrack? {
        val uri = Uri.fromFile(file)
        val extracted = extractTrackFromUri(uri, coverOverrides)
        if (extracted != null && sidecar == null) return extracted

        val title = sidecar?.title?.takeIf { it.isNotBlank() }
            ?: extracted?.title
            ?: file.nameWithoutExtension
        val artist = sidecar?.artist?.takeIf { it.isNotBlank() }
            ?: extracted?.artist
            ?: "Artista autorizado"
        val album = sidecar?.album?.takeIf { it.isNotBlank() }
            ?: extracted?.album
            ?: "Descargas Ritmo Local"
        val durationMs = (extracted?.durationMs ?: 0L).takeIf { it > 0L }
            ?: sidecar?.durationMs?.coerceAtLeast(0L)
            ?: 0L
        val formatLabel = sidecar?.formatLabel?.takeIf { it.isNotBlank() }
            ?: extracted?.formatLabel
            ?: resolveAudioFormatLabel(null, file.name)
        val trackKey = buildTrackKey(title, artist, durationMs)
        val customArt = coverOverrides[trackKey]
            ?: coverOverrides[CoverArtRepository.albumOverrideKey(album, artist)]
            ?: sidecar?.artworkUri
            ?: extracted?.albumArtUri

        return LocalTrack(
            id = abs(uri.toString().hashCode().toLong()) + 2_000_000L,
            trackKey = trackKey,
            title = title.take(180),
            artist = artist.take(180),
            album = album.take(180),
            albumId = 0L,
            durationMs = durationMs,
            contentUri = uri.toString(),
            albumArtUri = customArt,
            dateAdded = (file.lastModified() / 1000L).coerceAtLeast(0L),
            formatLabel = formatLabel
        )
    }

    private fun extractTrackFromUri(uri: Uri, coverOverrides: Map<String, String>): LocalTrack? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val displayName = resolveDisplayName(uri) ?: uri.lastPathSegment
            val mimeType = context.contentResolver.getType(uri)
            val fileName = displayName?.substringBeforeLast('.') ?: "Audio local"
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fileName
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Artista local"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: "Archivo importado"
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() } ?: ""
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L

            val stableId = abs(uri.toString().hashCode().toLong()) + 1_000_000L
            val trackKey = buildTrackKey(title, artist, durationMs)
            val customArt = coverOverrides[trackKey]
                ?: coverOverrides[CoverArtRepository.albumOverrideKey(album, artist)]
            LocalTrack(
                id = stableId,
                trackKey = trackKey,
                title = title.take(180),
                artist = artist.take(180),
                album = album.take(180),
                albumId = 0L,
                durationMs = durationMs,
                contentUri = uri.toString(),
                albumArtUri = customArt,
                dateAdded = System.currentTimeMillis() / 1000L,
                formatLabel = resolveAudioFormatLabel(mimeType, displayName),
                genre = genre.take(60)
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return File(uri.path.orEmpty()).name
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAudioFormatLabel(mimeType: String?, fileName: String?): String {
        val ext = fileName?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.length in 2..5 }
        if (ext != null) return ext
        return when (mimeType?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "MP3"
            "audio/flac", "application/x-flac" -> "FLAC"
            "audio/wav", "audio/x-wav" -> "WAV"
            "audio/ogg", "audio/vorbis", "application/ogg" -> "OGG"
            "audio/opus" -> "OPUS"
            "audio/mp4", "audio/m4a", "audio/aac" -> "M4A"
            "audio/amr", "audio/amr-wb" -> "AMR"
            "audio/x-ms-wma" -> "WMA"
            "audio/midi", "audio/x-midi" -> "MIDI"
            else -> mimeType?.substringAfter('/')?.uppercase()?.take(5) ?: "AUDIO"
        }
    }

    fun generatePlaylistForSlot(
        allTracks: List<LocalTrack>,
        slot: TimeOfDaySlot,
        seed: Long = System.currentTimeMillis()
    ): GeneratedPlaylist {
        val random = Random(seed)
        if (allTracks.isEmpty()) {
            return GeneratedPlaylist(
                slot = slot,
                title = "Mix de ${slot.displayName}",
                subtitle = slot.description,
                tracks = emptyList(),
                generatedAtMs = System.currentTimeMillis()
            )
        }

        val scored = allTracks.map { track ->
            val durationMin = track.durationMs / 60000.0
            val slotWeight = when (slot) {
                TimeOfDaySlot.MORNING -> if (durationMin in 1.5..4.2) 1.4 else 1.0
                TimeOfDaySlot.AFTERNOON -> 1.2
                TimeOfDaySlot.NIGHT -> if (durationMin >= 3.5) 1.45 else 1.0
            }
            val score = random.nextDouble(0.25, 1.0) * slotWeight
            track to score
        }.sortedByDescending { it.second }.map { it.first }

        val selected = mutableListOf<LocalTrack>()
        val pool = scored.toMutableList()
        val maxTracks = minOf(allTracks.size, 25)
        while (selected.size < maxTracks && pool.isNotEmpty()) {
            val lastArtist = selected.lastOrNull()?.artist
            val pickIndex = pool.indexOfFirst { it.artist != lastArtist }.takeIf { it >= 0 } ?: 0
            selected.add(pool.removeAt(pickIndex))
        }

        val subtitle = when (slot) {
            TimeOfDaySlot.MORNING -> "${selected.size} canciones aleatorias para arrancar tu mañana"
            TimeOfDaySlot.AFTERNOON -> "${selected.size} canciones seleccionadas al azar para tu tarde"
            TimeOfDaySlot.NIGHT -> "${selected.size} canciones seleccionadas para tu sesión nocturna"
        }

        return GeneratedPlaylist(
            slot = slot,
            title = "Mix Aleatorio · ${slot.displayName}",
            subtitle = subtitle,
            tracks = selected,
            generatedAtMs = System.currentTimeMillis()
        )
    }

    fun buildAlbumPlaylists(tracks: List<LocalTrack>): List<AlbumPlaylist> {
        return tracks
            .groupBy { it.album to it.artist }
            .map { (albumArtist, albumTracks) ->
                AlbumPlaylist(
                    albumName = albumArtist.first,
                    artistName = albumArtist.second,
                    albumArtUri = albumTracks.firstNotNullOfOrNull { it.albumArtUri },
                    tracks = albumTracks.sortedBy { it.title.lowercase() }
                )
            }
            .sortedBy { it.albumName.lowercase() }
    }

    fun buildArtistPlaylists(tracks: List<LocalTrack>): List<ArtistPlaylist> {
        return tracks
            .groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                val sortedTracks = artistTracks.sortedBy { it.title.lowercase() }
                val artistAlbums = artistTracks
                    .groupBy { it.album }
                    .map { (albumName, albumTracks) ->
                        AlbumPlaylist(
                            albumName = albumName,
                            artistName = artistName,
                            albumArtUri = albumTracks.firstNotNullOfOrNull { it.albumArtUri },
                            tracks = albumTracks.sortedBy { it.title.lowercase() }
                        )
                    }
                    .sortedBy { it.albumName.lowercase() }
                ArtistPlaylist(
                    artistName = artistName,
                    albumCount = artistAlbums.size,
                    albums = artistAlbums,
                    tracks = sortedTracks
                )
            }
            .sortedByDescending { it.tracks.size }
    }

    private fun buildTrackKey(title: String, artist: String, durationMs: Long): String {
        val cleanTitle = title.trim().lowercase().take(80)
        val cleanArtist = artist.trim().lowercase().take(80)
        val durationBucket = (durationMs / 1000L)
        return "${cleanTitle}|${cleanArtist}|${durationBucket}".take(190)
    }

    companion object {
        private val VOICE_NOTE_REGEX = Regex("^(ptt|aud|wa)[-_ ]?\\d+.*", RegexOption.IGNORE_CASE)

        fun isShortOrVoiceNoteAudio(
            durationMs: Long,
            title: String,
            displayName: String? = null,
            filePath: String? = null,
            album: String? = null
        ): Boolean {
            if (durationMs in 1L..29_999L) return true
            val combined = listOfNotNull(title, displayName, filePath, album).joinToString(" ").lowercase()
            if (combined.contains("whatsapp") ||
                combined.contains("voice notes") ||
                combined.contains("nota de voz") ||
                combined.contains("telegram audio")
            ) {
                return true
            }
            return VOICE_NOTE_REGEX.matches(title.trim()) ||
                (displayName != null && VOICE_NOTE_REGEX.matches(displayName.substringBeforeLast('.').trim()))
        }

        fun getSharedDownloadsDir(context: Context): File {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            return File(base, "RitmoLocalShared").apply { mkdirs() }
        }

        fun getDownloadsMusicDir(context: Context): File {
            val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            return File(base, "RitmoLocalDownloads").apply { mkdirs() }
        }

        fun saveDownloadedTrackSidecar(context: Context, audioFileName: String, sidecar: DownloadedTrackSidecar) {
            runCatching {
                val metaDir = File(getDownloadsMusicDir(context), ".meta").apply { mkdirs() }
                val json = JSONObject().apply {
                    put("title", sidecar.title)
                    put("artist", sidecar.artist)
                    put("album", sidecar.album)
                    put("durationMs", sidecar.durationMs)
                    put("artworkUri", sidecar.artworkUri.orEmpty())
                    put("formatLabel", sidecar.formatLabel)
                    put("sourceName", sidecar.sourceName)
                    put("licenseName", sidecar.licenseName)
                }
                File(metaDir, "$audioFileName.json").writeText(json.toString())
            }
        }

        fun readDownloadedTrackSidecar(context: Context, audioFileName: String): DownloadedTrackSidecar? {
            return runCatching {
                val metaFile = File(File(getDownloadsMusicDir(context), ".meta"), "$audioFileName.json")
                if (!metaFile.exists()) return null
                val json = JSONObject(metaFile.readText())
                DownloadedTrackSidecar(
                    title = json.optString("title"),
                    artist = json.optString("artist"),
                    album = json.optString("album"),
                    durationMs = json.optLong("durationMs", 0L),
                    artworkUri = json.optString("artworkUri").takeIf { it.isNotBlank() },
                    formatLabel = json.optString("formatLabel"),
                    sourceName = json.optString("sourceName"),
                    licenseName = json.optString("licenseName")
                )
            }.getOrNull()
        }
    }
}

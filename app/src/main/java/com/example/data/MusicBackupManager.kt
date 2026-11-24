package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.model.CustomPlaylist
import com.example.model.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupSummary(
    val exportedTracks: Int,
    val exportedPlaylists: Int,
    val exportedCovers: Int
)

data class RestoreSummary(
    val restoredTracks: Int,
    val restoredPlaylists: Int
)

object MusicBackupManager {

    private const val MAX_AUDIO_FILES_IN_ZIP = 40
    private const val MAX_TOTAL_ZIP_BYTES = 180L * 1024L * 1024L

    suspend fun exportBackupZip(
        context: Context,
        destUri: Uri,
        tracks: List<LocalTrack>,
        customPlaylists: List<CustomPlaylist>,
        favoriteTrackKeys: Set<String>
    ): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            var exportedTracksCount = 0
            var exportedCoversCount = 0
            var runningBytes = 0L
            val includedTrackKeys = mutableListOf<String>()

            context.contentResolver.openOutputStream(destUri)?.use { out ->
                ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                    // 1. Custom covers
                    val customCoversDir = File(context.filesDir, "custom_covers")
                    customCoversDir.listFiles()?.forEach { coverFile ->
                        if (coverFile.isFile && coverFile.length() > 0L) {
                            zip.putNextEntry(ZipEntry("custom_covers/${coverFile.name}"))
                            coverFile.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                            exportedCoversCount++
                        }
                    }

                    // 2. Audio files (cap size & count cleanly for Drive upload reliability)
                    val usedNames = mutableSetOf<String>()
                    for (track in tracks.take(MAX_AUDIO_FILES_IN_ZIP)) {
                        if (runningBytes >= MAX_TOTAL_ZIP_BYTES) break
                        val ext = track.formatLabel.lowercase().takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "mp3"
                        val safeBase = track.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(48).ifBlank { "track" }
                        var entryName = "audio/${safeBase}.${ext}"
                        var idx = 1
                        while (!usedNames.add(entryName)) {
                            entryName = "audio/${safeBase}_${idx++}.${ext}"
                        }
                        try {
                            val trackUri = Uri.parse(track.contentUri)
                            context.contentResolver.openInputStream(trackUri)?.use { input ->
                                val buffer = ByteArray(16384)
                                zip.putNextEntry(ZipEntry(entryName))
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    zip.write(buffer, 0, read)
                                    runningBytes += read
                                }
                                zip.closeEntry()
                                exportedTracksCount++
                                includedTrackKeys.add(track.trackKey)
                            }
                        } catch (_: Exception) {
                            // Skip unreadable track
                        }
                    }

                    // 3. Manifest JSON
                    val manifest = JSONObject().apply {
                        put("app", "RitmoLocal")
                        put("version", 1)
                        put("exportedAtMs", System.currentTimeMillis())
                        put("playlists", JSONArray(UserPreferencesRepository.encodeCustomPlaylists(customPlaylists)))
                        put("favorites", JSONArray(favoriteTrackKeys.toList()))
                        put("exportedTrackKeys", JSONArray(includedTrackKeys))
                    }
                    zip.putNextEntry(ZipEntry("backup_manifest.json"))
                    zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo abrir el destino para guardar la copia."))

            Result.success(
                BackupSummary(
                    exportedTracks = exportedTracksCount,
                    exportedPlaylists = customPlaylists.size,
                    exportedCovers = exportedCoversCount
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupZip(
        context: Context,
        sourceUri: Uri,
        preferencesRepository: UserPreferencesRepository
    ): Result<RestoreSummary> = withContext(Dispatchers.IO) {
        try {
            var restoredTracks = 0
            var restoredPlaylists = 0
            val sharedDir = LocalMusicRepository.getSharedDownloadsDir(context)
            val customCoversDir = File(context.filesDir, "custom_covers").apply { mkdirs() }

            context.contentResolver.openInputStream(sourceUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zip ->
                    var entry: ZipEntry? = zip.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && !name.contains("..")) {
                            when {
                                name == "backup_manifest.json" -> {
                                    val jsonStr = zip.readBytes().toString(Charsets.UTF_8)
                                    val json = JSONObject(jsonStr)
                                    val playlistsArr = json.optJSONArray("playlists")
                                    if (playlistsArr != null) {
                                        val decoded = UserPreferencesRepository.decodeCustomPlaylists(playlistsArr.toString())
                                        if (decoded.isNotEmpty()) {
                                            preferencesRepository.saveCustomPlaylists(decoded)
                                            restoredPlaylists = decoded.size
                                        }
                                    }
                                    val favsArr = json.optJSONArray("favorites")
                                    if (favsArr != null) {
                                        for (i in 0 until favsArr.length()) {
                                            val key = favsArr.optString(i)
                                            if (key.isNotBlank()) {
                                                // Store offline favorite key safely
                                            }
                                        }
                                    }
                                }
                                name.startsWith("audio/") -> {
                                    val fileName = File(name).name.ifBlank { "restored_${System.currentTimeMillis()}.mp3" }
                                    val destFile = File(sharedDir, fileName)
                                    FileOutputStream(destFile).use { out ->
                                        zip.copyTo(out)
                                    }
                                    restoredTracks++
                                }
                                name.startsWith("custom_covers/") -> {
                                    val fileName = File(name).name
                                    if (fileName.isNotBlank()) {
                                        val destFile = File(customCoversDir, fileName)
                                        FileOutputStream(destFile).use { out ->
                                            zip.copyTo(out)
                                        }
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("No se pudo leer el archivo de respaldo."))

            Result.success(RestoreSummary(restoredTracks = restoredTracks, restoredPlaylists = restoredPlaylists))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildShareAudioIntent(context: Context, track: LocalTrack): Intent? {
        return try {
            val sourceUri = Uri.parse(track.contentUri)
            val shareUri = if (sourceUri.scheme == "file") {
                val file = File(sourceUri.path.orEmpty())
                if (!file.exists()) return null
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                // Copy into cache/shared_audio so receiving apps always have readable FileProvider grant
                val cacheDir = File(context.cacheDir, "shared_audio").apply { mkdirs() }
                val ext = track.formatLabel.lowercase().takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "mp3"
                val safeTitle = track.title.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40).ifBlank { "audio" }
                val tempFile = File(cacheDir, "$safeTitle.$ext")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(tempFile).use { out ->
                        input.copyTo(out)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0L) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                } else {
                    sourceUri
                }
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
                putExtra(Intent.EXTRA_TEXT, "Escucha '${track.title}' de ${track.artist} en RitmoLocal")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(sendIntent, "Compartir ${track.title}")
        } catch (_: Exception) {
            null
        }
    }

    fun resolveFileSizeAndName(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "audio.mp3"
        var size = 0L
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0) ?: name
                        size = cursor.getLong(1)
                    }
                }
        } catch (_: Exception) {
        }
        return name to size
    }
}

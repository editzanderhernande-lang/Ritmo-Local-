package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

class CoverArtRepository(private val context: Context) {

    private val customDir: File
        get() = File(context.filesDir, "custom_covers").apply { mkdirs() }

    private val cachedDir: File
        get() = File(context.filesDir, "cached_covers").apply { mkdirs() }

    private val wallpaperDir: File
        get() = File(context.filesDir, "wallpapers").apply { mkdirs() }

    private val avatarDir: File
        get() = File(context.filesDir, "avatars").apply { mkdirs() }

    suspend fun saveProfileAvatarFromUri(sourceUri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth.coerceAtLeast(1)
            val srcH = boundsOpts.outHeight.coerceAtLeast(1)
            var sampleSize = 1
            while ((srcW / sampleSize) > 256 && (srcH / sampleSize) > 256) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@withContext null

            val side = minOf(decoded.width, decoded.height).coerceAtLeast(1)
            val cropX = ((decoded.width - side) / 2).coerceAtLeast(0)
            val cropY = ((decoded.height - side) / 2).coerceAtLeast(0)
            val square = Bitmap.createBitmap(decoded, cropX, cropY, side, side)
            val scaled = if (side != 96) {
                Bitmap.createScaledBitmap(square, 96, 96, true)
            } else {
                square
            }

            var quality = 74
            var jpegBytes: ByteArray
            do {
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                jpegBytes = baos.toByteArray()
                quality -= 12
            } while (jpegBytes.size > 22_000 && quality >= 30)

            if (scaled !== square) scaled.recycle()
            if (square !== decoded) square.recycle()
            decoded.recycle()

            avatarDir.listFiles()?.forEach { it.delete() }
            val destFile = File(avatarDir, "user_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(destFile).use { it.write(jpegBytes) }

            val base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP).take(34_000)
            Uri.fromFile(destFile).toString() to base64
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearProfileAvatar() = withContext(Dispatchers.IO) {
        try {
            avatarDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    suspend fun saveWallpaperFromUri(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            wallpaperDir.listFiles()?.forEach { it.delete() }
            val destFile = File(wallpaperDir, "bg_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun clearWallpaper() = withContext(Dispatchers.IO) {
        try {
            wallpaperDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    suspend fun saveCustomArtworkFromUri(targetKey: String, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val safeName = "cov_${shaShort(targetKey)}.jpg"
            val destFile = File(customDir, safeName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAndCacheOnlineCover(
        targetKey: String,
        title: String,
        artist: String,
        album: String
    ): String? = withContext(Dispatchers.IO) {
        val safeName = "net_${shaShort(targetKey)}.jpg"
        val destFile = File(cachedDir, safeName)
        if (destFile.exists() && destFile.length() > 1024L) {
            return@withContext Uri.fromFile(destFile).toString()
        }

        val cleanArtist = artist.takeUnless {
            it.contains("desconocido", ignoreCase = true) || it.contains("local", ignoreCase = true)
        }.orEmpty()
        val cleanAlbum = album.takeUnless {
            it.contains("desconocido", ignoreCase = true) || it.contains("importado", ignoreCase = true)
        }.orEmpty()
        val queryParts = listOf(cleanArtist, cleanAlbum.ifBlank { title })
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        if (queryParts.isBlank()) return@withContext null

        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(queryParts, "UTF-8")
            val apiUrl = URL("https://itunes.apple.com/search?term=$encoded&media=music&limit=1")
            conn = (apiUrl.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val first = results.getJSONObject(0)
            val artUrlStr = first.optString("artworkUrl100")
                .takeIf { it.isNotBlank() }
                ?.replace("100x100bb", "600x600bb")
                ?: return@withContext null

            downloadImageToFile(artUrlStr, destFile)
            if (destFile.exists() && destFile.length() > 512L) {
                Uri.fromFile(destFile).toString()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadImageToFile(imageUrl: String, destFile: File) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(imageUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } finally {
            conn?.disconnect()
        }
    }

    private fun shaShort(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(20)
    }

    companion object {
        fun albumOverrideKey(albumName: String, artistName: String): String =
            "album|${albumName.trim().lowercase()}|${artistName.trim().lowercase()}"
    }
}

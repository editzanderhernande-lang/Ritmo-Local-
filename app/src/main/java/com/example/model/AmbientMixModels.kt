package com.example.model

import com.example.data.stats.TrackStatsEntity
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class AmbientMoodCategory(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val accentArgb: Long,
    val preferredKeywords: List<String>,
    val preferredDurationRangeMs: LongRange,
    val preferFavoritesBoost: Boolean = false
) {
    FAVORITES_SMART(
        id = "favorites_smart",
        displayName = "Favoritas",
        subtitle = "Tus canciones favoritas mezcladas con ritmo fluido",
        accentArgb = 0xFFEC4899,
        preferredKeywords = emptyList(),
        preferredDurationRangeMs = 0L..900_000L,
        preferFavoritesBoost = true
    ),
    MOST_PLAYED_SMART(
        id = "most_played_smart",
        displayName = "Más escuchado",
        subtitle = "Tus cortes con mayor número de sesiones reales",
        accentArgb = 0xFF9D6BFF,
        preferredKeywords = emptyList(),
        preferredDurationRangeMs = 0L..900_000L,
        preferFavoritesBoost = true
    ),
    RECENT_DISCOVERIES(
        id = "recent_discoveries",
        displayName = "Descubrimientos",
        subtitle = "Lo más reciente agregado o poco escuchado en tu biblioteca",
        accentArgb = 0xFF10B981,
        preferredKeywords = emptyList(),
        preferredDurationRangeMs = 0L..900_000L
    ),
    NOT_PLAYED_LATELY(
        id = "not_played_lately",
        displayName = "Hace tiempo no escuchas",
        subtitle = "Temas que amaste y llevan semanas sin sonar",
        accentArgb = 0xFFF59E0B,
        preferredKeywords = emptyList(),
        preferredDurationRangeMs = 0L..900_000L
    ),
    MORNING(
        id = "morning",
        displayName = "Mañana",
        subtitle = "Ritmo luminoso para empezar el día",
        accentArgb = 0xFF38BDF8,
        preferredKeywords = listOf("sun", "morning", "day", "light", "mañana", "sol", "amanecer", "alegría", "happy", "pop", "dance"),
        preferredDurationRangeMs = 130_000L..260_000L,
        preferFavoritesBoost = true
    ),
    NIGHT(
        id = "night",
        displayName = "Noche",
        subtitle = "Atmósfera nocturna y profunda",
        accentArgb = 0xFF818CF8,
        preferredKeywords = listOf("night", "noche", "luna", "moon", "midnight", "dark", "neon", "synth", "chill", "r&b", "soul"),
        preferredDurationRangeMs = 170_000L..380_000L
    ),
    RELAX(
        id = "relax",
        displayName = "Relajación",
        subtitle = "Temas suaves para bajar el pulso",
        accentArgb = 0xFF34D399,
        preferredKeywords = listOf("relax", "calm", "soft", "acoustic", "acústico", "piano", "jazz", "lofi", "lo-fi", "chill", "suave", "bossa"),
        preferredDurationRangeMs = 160_000L..420_000L
    ),
    FOCUS(
        id = "focus",
        displayName = "Concentración",
        subtitle = "Estudio y trabajo sin distracciones",
        accentArgb = 0xFF22D3EE,
        preferredKeywords = listOf("focus", "study", "instrumental", "ambient", "classical", "clásica", "piano", "synth", "deep", "score", "ost"),
        preferredDurationRangeMs = 140_000L..480_000L
    ),
    WORKOUT(
        id = "workout",
        displayName = "Entrenar",
        subtitle = "Energía alta para ritmo activo",
        accentArgb = 0xFFF43F5E,
        preferredKeywords = listOf("gym", "run", "power", "workout", "hip", "rap", "trap", "rock", "metal", "electro", "edm", "remix", "fuego", "party"),
        preferredDurationRangeMs = 120_000L..250_000L,
        preferFavoritesBoost = true
    ),
    SMART_RANDOM(
        id = "smart_random",
        displayName = "Aleatoria Inteligente",
        subtitle = "Variedad de artistas y géneros sin repeticiones seguidas",
        accentArgb = 0xFFA78BFA,
        preferredKeywords = emptyList(),
        preferredDurationRangeMs = 0L..900_000L
    );

    companion object {
        fun fromId(id: String?): AmbientMoodCategory =
            entries.firstOrNull { it.id == id } ?: MORNING

        fun fromTimeOfDay(slot: TimeOfDaySlot): AmbientMoodCategory = when (slot) {
            TimeOfDaySlot.MORNING -> MORNING
            TimeOfDaySlot.AFTERNOON -> FOCUS
            TimeOfDaySlot.NIGHT -> NIGHT
        }
    }
}

data class SavedAmbientMix(
    val id: String,
    val categoryId: String,
    val name: String,
    val trackKeys: List<String>,
    val isFavorite: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    companion object {
        fun encodeList(mixes: List<SavedAmbientMix>): String {
            val arr = JSONArray()
            mixes.forEach { mix ->
                arr.put(
                    JSONObject().apply {
                        put("id", mix.id)
                        put("categoryId", mix.categoryId)
                        put("name", mix.name)
                        put("isFavorite", mix.isFavorite)
                        put("updatedAtMs", mix.updatedAtMs)
                        put("trackKeys", JSONArray(mix.trackKeys))
                    }
                )
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<SavedAmbientMix> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val out = mutableListOf<SavedAmbientMix>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val keysJson = obj.optJSONArray("trackKeys")
                    val keys = mutableListOf<String>()
                    if (keysJson != null) {
                        for (j in 0 until keysJson.length()) {
                            val k = keysJson.optString(j)
                            if (k.isNotBlank()) keys.add(k)
                        }
                    }
                    out.add(
                        SavedAmbientMix(
                            id = id,
                            categoryId = AmbientMoodCategory.fromId(obj.optString("categoryId")).id,
                            name = name,
                            trackKeys = keys,
                            isFavorite = obj.optBoolean("isFavorite", false),
                            updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis())
                        )
                    )
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

data class GeneratedAmbientMix(
    val category: AmbientMoodCategory,
    val title: String,
    val subtitle: String,
    val tracks: List<LocalTrack>,
    val isSavedFavorite: Boolean = false
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs.coerceAtLeast(0L) }
}

object AmbientMixGenerator {

    fun generateMix(
        tracks: List<LocalTrack>,
        category: AmbientMoodCategory,
        favoriteTrackKeys: Set<String> = emptySet(),
        mostPlayedKeys: List<String> = emptyList(),
        trackStatsByKey: Map<String, TrackStatsEntity> = emptyMap(),
        seed: Long = System.nanoTime(),
        nowMs: Long = System.currentTimeMillis(),
        maxTracks: Int = 20
    ): GeneratedAmbientMix {
        if (tracks.isEmpty()) {
            return GeneratedAmbientMix(
                category = category,
                title = "Mezcla · ${category.displayName}",
                subtitle = category.subtitle,
                tracks = emptyList()
            )
        }

        val rng = Random(seed + category.id.hashCode() * 31L)
        val topPlayedRanks = mostPlayedKeys.withIndex().associate { it.value to it.index }

        val candidateTracks = when (category) {
            AmbientMoodCategory.FAVORITES_SMART -> {
                val favs = tracks.filter { it.trackKey in favoriteTrackKeys }
                if (favs.isNotEmpty()) favs else tracks
            }
            else -> tracks
        }

        val scored = candidateTracks.map { track ->
            val stat = trackStatsByKey[track.trackKey]
            val hay = "${track.title} ${track.artist} ${track.album} ${track.genre} ${track.formatLabel}".lowercase()
            var score = rng.nextDouble(0.0, 2.2)

            when (category) {
                AmbientMoodCategory.FAVORITES_SMART -> {
                    if (track.trackKey in favoriteTrackKeys) score += 10.0
                    score += ((stat?.qualifiedPlays ?: 0L).coerceAtMost(20L)) * 0.25
                }
                AmbientMoodCategory.MOST_PLAYED_SMART -> {
                    val plays = stat?.qualifiedPlays ?: 0L
                    val rankBonus = topPlayedRanks[track.trackKey]?.let { (25 - it).coerceAtLeast(0) * 0.8 } ?: 0.0
                    score += plays * 1.6 + rankBonus
                }
                AmbientMoodCategory.RECENT_DISCOVERIES -> {
                    val lowPlayBoost = if ((stat?.qualifiedPlays ?: 0L) <= 2L) 4.5 else 0.0
                    val dateBoost = (track.dateAdded / 1_000_000.0).coerceIn(0.0, 4.0)
                    score += lowPlayBoost + dateBoost
                }
                AmbientMoodCategory.NOT_PLAYED_LATELY -> {
                    val lastMs = stat?.lastPlayedAtMs
                    val plays = stat?.qualifiedPlays ?: 0L
                    score += when {
                        lastMs != null && plays > 0L -> {
                            val daysAgo = ((nowMs - lastMs) / (24L * 60L * 60L * 1000L)).coerceAtLeast(0L)
                            (daysAgo.coerceAtMost(90L) * 0.25) + (plays.coerceAtMost(15L) * 0.4)
                        }
                        lastMs == null -> 3.0 // never played yet
                        else -> 0.5
                    }
                }
                AmbientMoodCategory.SMART_RANDOM -> {
                    if (track.trackKey in favoriteTrackKeys) score += 1.4
                    if (stat == null || stat.qualifiedPlays == 0L) score += 1.2
                }
                else -> {
                    category.preferredKeywords.forEach { kw ->
                        if (hay.contains(kw)) score += 3.5
                    }
                    if (track.durationMs in category.preferredDurationRangeMs) {
                        score += 1.6
                    }
                    if (category.preferFavoritesBoost && track.trackKey in favoriteTrackKeys) {
                        score += 1.8
                    }
                    val playedRank = topPlayedRanks[track.trackKey]
                    if (playedRank != null) {
                        score += (2.0 - playedRank * 0.15).coerceAtLeast(0.3)
                    }
                    if (category == AmbientMoodCategory.RELAX &&
                        (hay.contains("flac") || hay.contains("acoustic") || track.durationMs >= 210_000L)
                    ) {
                        score += 1.1
                    }
                }
            }
            track to score
        }.sortedByDescending { it.second }

        // Keep varied artists in the mix (dispersion pass)
        val selected = mutableListOf<LocalTrack>()
        var lastArtist = ""
        val pool = scored.map { it.first }.toMutableList()
        while (pool.isNotEmpty() && selected.size < maxTracks) {
            val nextIdx = pool.indexOfFirst { !it.artist.equals(lastArtist, ignoreCase = true) }
            val chosen = if (nextIdx >= 0) pool.removeAt(nextIdx) else pool.removeAt(0)
            selected.add(chosen)
            lastArtist = chosen.artist
        }

        return GeneratedAmbientMix(
            category = category,
            title = "Mezcla · ${category.displayName}",
            subtitle = category.subtitle,
            tracks = selected
        )
    }

    fun generateAllCategories(
        tracks: List<LocalTrack>,
        favoriteTrackKeys: Set<String> = emptySet(),
        mostPlayedKeys: List<String> = emptyList(),
        trackStatsByKey: Map<String, TrackStatsEntity> = emptyMap(),
        seed: Long = System.nanoTime()
    ): Map<AmbientMoodCategory, GeneratedAmbientMix> =
        AmbientMoodCategory.entries.associateWith { cat ->
            generateMix(
                tracks = tracks,
                category = cat,
                favoriteTrackKeys = favoriteTrackKeys,
                mostPlayedKeys = mostPlayedKeys,
                trackStatsByKey = trackStatsByKey,
                seed = seed + cat.ordinal * 97L
            )
        }
}

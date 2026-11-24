package com.example.data

import android.content.Context
import android.util.Base64
import com.example.R
import com.example.model.AppSettingsPrefs
import com.example.model.ChatRegion
import com.example.model.FavoriteTrack
import com.example.model.LocalTrack
import com.example.model.MonthlyStat
import com.example.model.MonthlyTrackStat
import com.example.model.PlaybackProgress
import com.example.model.RegionalChatMessage
import com.example.model.TimeOfDaySlot
import com.example.model.stableTrackStatId
import com.example.notifications.ChatNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUid(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google para sincronizar en la nube.")
        check(uid == expectedUserId) { "La cuenta activa cambió; se cancela la operación." }
        return uid
    }

    fun observePlaybackProgress(userId: String): Flow<PlaybackProgress?> {
        val path = "users/$userId/playbackState/current"
        return db.collection("users").document(userId)
            .collection("playbackState").document("current")
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(
                        PlaybackProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, path)
                }
                throw error
            }
    }

    fun observeMonthlyStats(userId: String): Flow<List<MonthlyStat>> {
        val path = "users/$userId/monthlyStats"
        return db.collection("users").document(userId)
            .collection("monthlyStats")
            .orderBy("monthKey", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MonthlyStat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeMonthTrackStats(userId: String, monthKey: String): Flow<List<MonthlyTrackStat>> {
        val path = "users/$userId/monthlyStats/$monthKey/trackStats"
        return db.collection("users").document(userId)
            .collection("monthlyStats").document(monthKey)
            .collection("trackStats")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    MonthlyTrackStat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeFavorites(userId: String): Flow<List<FavoriteTrack>> {
        val path = "users/$userId/favorites"
        return db.collection("users").document(userId)
            .collection("favorites")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FavoriteTrack::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeRegionalChat(region: ChatRegion): Flow<List<RegionalChatMessage>> {
        val path = "regionalChats/${region.id}/messages"
        return db.collection("regionalChats").document(region.id)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(80)
            .snapshots()
            .map { snapshot ->
                val nowMs = System.currentTimeMillis()
                snapshot.toObjects(
                    RegionalChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).filterNot { msg ->
                    val expMs = msg.expiresAt?.toDate()?.time
                    if (expMs != null) {
                        expMs <= nowMs
                    } else {
                        val createdMs = msg.createdAt?.toDate()?.time
                        createdMs != null && (nowMs - createdMs) >= CHAT_TTL_MS
                    }
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser != null) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun sendRegionalChatMessage(
        region: ChatRegion,
        senderName: String,
        senderAvatarBase64: String = "",
        messageText: String,
        recommendedTrackTitle: String = "",
        recommendedArtist: String = "",
        audioFileName: String = "",
        audioMimeType: String = "",
        audioBytes: ByteArray = ByteArray(0)
    ): Result<Unit> {
        val path = "regionalChats/${region.id}/messages"
        return try {
            val user = auth.currentUser
                ?: throw IllegalStateException("Inicia sesión con Google para escribir en el chat global.")
            if (audioBytes.size > AudioClipTrimmer.MAX_RAW_CLIP_BYTES) {
                throw IllegalArgumentException(
                    "El audio supera ${AudioClipTrimmer.formatSizeReadable(AudioClipTrimmer.MAX_RAW_CLIP_BYTES.toLong())}."
                )
            }

            val docId = "msg_${System.currentTimeMillis()}_${user.uid.take(6)}"
            val docRef = db.collection("regionalChats").document(region.id)
                .collection("messages").document(docId)
            val expiresAt = Timestamp(Date(System.currentTimeMillis() + CHAT_TTL_MS))

            val encoded = if (audioBytes.isNotEmpty()) {
                withContext(Dispatchers.Default) { Base64.encodeToString(audioBytes, Base64.NO_WRAP) }
            } else ""

            var inlineBase64 = ""
            var chunkCount = 0

            if (encoded.isNotEmpty()) {
                if (encoded.length <= AudioClipTrimmer.MAX_INLINE_BASE64_CHARS) {
                    inlineBase64 = encoded
                } else {
                    val chunks = encoded.chunked(AudioClipTrimmer.CHUNK_BASE64_CHARS)
                    if (chunks.size > AudioClipTrimmer.MAX_AUDIO_CHUNKS) {
                        throw IllegalArgumentException("El audio es demasiado grande para compartir en el chat.")
                    }
                    chunkCount = chunks.size
                    chunks.forEachIndexed { idx, chunkStr ->
                        val chunkPayload = mapOf(
                            "senderId" to user.uid,
                            "regionId" to region.id,
                            "messageId" to docId,
                            "chunkIndex" to idx,
                            "dataBase64" to chunkStr,
                            "expiresAt" to expiresAt,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        docRef.collection("audioChunks").document("chunk_$idx").set(chunkPayload).await()
                    }
                }
            }

            val payload = mapOf(
                "senderId" to user.uid,
                "senderName" to senderName.trim().ifBlank { "Oyente" }.take(80),
                "senderAvatarBase64" to senderAvatarBase64.take(35_000),
                "regionId" to region.id,
                "messageText" to messageText.trim().take(500),
                "recommendedTrackTitle" to recommendedTrackTitle.trim().take(160),
                "recommendedArtist" to recommendedArtist.trim().take(160),
                "audioFileName" to audioFileName.trim().take(160),
                "audioMimeType" to audioMimeType.trim().take(80),
                "audioClipBase64" to inlineBase64.take(960_000),
                "audioChunkCount" to chunkCount,
                "audioSizeBytes" to audioBytes.size.toLong(),
                "expiresAt" to expiresAt,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun fetchAudioBytesForMessage(
        region: ChatRegion,
        message: RegionalChatMessage
    ): Result<ByteArray> {
        val path = "regionalChats/${region.id}/messages/${message.id}/audioChunks"
        return try {
            if (message.audioClipBase64.isNotBlank()) {
                val bytes = withContext(Dispatchers.Default) {
                    Base64.decode(message.audioClipBase64, Base64.DEFAULT)
                }
                return Result.success(bytes)
            }
            if (message.audioChunkCount <= 0) {
                return Result.failure(IllegalStateException("Este mensaje no tiene audio adjunto."))
            }
            auth.currentUser ?: throw IllegalStateException("Inicia sesión para descargar audio del chat.")
            val snapshot = db.collection("regionalChats").document(region.id)
                .collection("messages").document(message.id)
                .collection("audioChunks")
                .orderBy("chunkIndex", Query.Direction.ASCENDING)
                .get()
                .await()
            val joined = buildString {
                snapshot.documents.forEach { doc ->
                    append(doc.getString("dataBase64").orEmpty())
                }
            }
            if (joined.isBlank()) {
                return Result.failure(IllegalStateException("Los datos del audio ya expiraron o no están disponibles."))
            }
            val bytes = withContext(Dispatchers.Default) {
                Base64.decode(joined, Base64.DEFAULT)
            }
            Result.success(bytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun deleteRegionalChatMessage(region: ChatRegion, messageId: String): Result<Unit> {
        val path = "regionalChats/${region.id}/messages/$messageId"
        return try {
            auth.currentUser
                ?: throw IllegalStateException("Inicia sesión con Google para eliminar mensajes.")
            val msgRef = db.collection("regionalChats").document(region.id)
                .collection("messages").document(messageId)
            val chunks = runCatching { msgRef.collection("audioChunks").get().await() }.getOrNull()
            chunks?.documents?.forEach { chunkDoc ->
                runCatching { chunkDoc.reference.delete().await() }
            }
            msgRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun purgeExpiredChatMessages(region: ChatRegion): Result<Int> {
        val path = "regionalChats/${region.id}/messages"
        return try {
            if (auth.currentUser == null) return Result.success(0)
            val expiredSnap = db.collection("regionalChats").document(region.id)
                .collection("messages")
                .whereLessThanOrEqualTo("expiresAt", Timestamp.now())
                .limit(15)
                .get()
                .await()
            var removed = 0
            for (doc in expiredSnap.documents) {
                val chunks = runCatching { doc.reference.collection("audioChunks").get().await() }.getOrNull()
                chunks?.documents?.forEach { chunkDoc ->
                    runCatching { chunkDoc.reference.delete().await() }
                }
                val ok = runCatching { doc.reference.delete().await() }.isSuccess
                if (ok) removed++
            }
            Result.success(removed)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-blocking background maintenance
            Result.success(0)
        }
    }

    suspend fun toggleFavorite(
        userId: String,
        track: LocalTrack,
        isCurrentlyFavorite: Boolean
    ): Result<Boolean> {
        val favId = stableTrackStatId(track.trackKey.ifBlank { track.title })
        val docPath = "users/$userId/favorites/$favId"
        return try {
            val uid = requireAuthorizedUid(userId)
            val docRef = db.collection("users").document(uid)
                .collection("favorites").document(favId)

            if (isCurrentlyFavorite) {
                docRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "trackKey" to track.trackKey.ifBlank { "unknown_track" }.take(200),
                    "trackTitle" to track.title.ifBlank { "Pista sin título" }.take(200),
                    "artistName" to track.artist.ifBlank { "Artista desconocido" }.take(200),
                    "albumName" to track.album.ifBlank { "Álbum desconocido" }.take(200),
                    "trackUri" to track.contentUri.take(1000),
                    "albumId" to track.albumId.coerceAtLeast(0L),
                    "durationMs" to track.durationMs.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun savePlaybackProgress(
        userId: String,
        track: LocalTrack,
        positionMs: Long,
        totalPlaybackSeconds: Long,
        slot: TimeOfDaySlot
    ): Result<Unit> {
        val docPath = "users/$userId/playbackState/current"
        return try {
            val uid = requireAuthorizedUid(userId)
            val docRef = db.collection("users").document(uid)
                .collection("playbackState").document("current")

            val safeTrackKey = track.trackKey.ifBlank { "unknown_track" }.take(200)
            val safeTitle = track.title.ifBlank { "Pista sin título" }.take(200)
            val safeArtist = track.artist.ifBlank { "Artista desconocido" }.take(200)
            val safeAlbum = track.album.ifBlank { "Álbum desconocido" }.take(200)
            val safeUri = track.contentUri.take(1000)
            val safeAlbumId = track.albumId.coerceAtLeast(0L)
            val safePos = positionMs.coerceAtLeast(0L)
            val safeDuration = track.durationMs.coerceAtLeast(0L)
            val safeTotalSeconds = totalPlaybackSeconds.coerceAtLeast(0L)

            val updatePayload = mapOf(
                "trackKey" to safeTrackKey,
                "trackTitle" to safeTitle,
                "artistName" to safeArtist,
                "albumName" to safeAlbum,
                "trackUri" to safeUri,
                "albumId" to safeAlbumId,
                "positionMs" to safePos,
                "durationMs" to safeDuration,
                "totalPlaybackSeconds" to safeTotalSeconds,
                "timeOfDaySlot" to slot.wireValue,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (docExists(docRef)) {
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "trackKey" to safeTrackKey,
                    "trackTitle" to safeTitle,
                    "artistName" to safeArtist,
                    "albumName" to safeAlbum,
                    "trackUri" to safeUri,
                    "albumId" to safeAlbumId,
                    "positionMs" to safePos,
                    "durationMs" to safeDuration,
                    "totalPlaybackSeconds" to safeTotalSeconds,
                    "timeOfDaySlot" to slot.wireValue,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordListeningTime(
        userId: String,
        monthKey: String,
        track: LocalTrack,
        deltaSeconds: Long,
        incrementPlayCount: Boolean,
        slot: TimeOfDaySlot
    ): Result<Unit> {
        if (deltaSeconds <= 0L && !incrementPlayCount) return Result.success(Unit)
        val monthDocPath = "users/$userId/monthlyStats/$monthKey"
        return try {
            val uid = requireAuthorizedUid(userId)
            val safeDelta = deltaSeconds.coerceAtLeast(0L)
            val playIncrement = if (incrementPlayCount) 1L else 0L

            val monthRef = db.collection("users").document(uid)
                .collection("monthlyStats").document(monthKey)

            val morningDelta = if (slot == TimeOfDaySlot.MORNING) safeDelta else 0L
            val afternoonDelta = if (slot == TimeOfDaySlot.AFTERNOON) safeDelta else 0L
            val nightDelta = if (slot == TimeOfDaySlot.NIGHT) safeDelta else 0L

            if (docExists(monthRef)) {
                monthRef.update(
                    mapOf(
                        "totalSeconds" to FieldValue.increment(safeDelta),
                        "totalPlays" to FieldValue.increment(playIncrement),
                        "morningSeconds" to FieldValue.increment(morningDelta),
                        "afternoonSeconds" to FieldValue.increment(afternoonDelta),
                        "nightSeconds" to FieldValue.increment(nightDelta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                monthRef.set(
                    mapOf(
                        "userId" to uid,
                        "monthKey" to monthKey,
                        "totalSeconds" to safeDelta,
                        "totalPlays" to playIncrement,
                        "morningSeconds" to morningDelta,
                        "afternoonSeconds" to afternoonDelta,
                        "nightSeconds" to nightDelta,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val statId = stableTrackStatId(track.trackKey.ifBlank { track.title })
            val trackStatRef = monthRef.collection("trackStats").document(statId)
            val safeTrackKey = track.trackKey.ifBlank { "unknown_track" }.take(200)
            val safeTitle = track.title.ifBlank { "Pista sin título" }.take(200)
            val safeArtist = track.artist.ifBlank { "Artista desconocido" }.take(200)
            val safeAlbum = track.album.ifBlank { "Álbum desconocido" }.take(200)
            val safeAlbumId = track.albumId.coerceAtLeast(0L)

            if (docExists(trackStatRef)) {
                trackStatRef.update(
                    mapOf(
                        "trackTitle" to safeTitle,
                        "artistName" to safeArtist,
                        "albumName" to safeAlbum,
                        "albumId" to safeAlbumId,
                        "playCount" to FieldValue.increment(playIncrement),
                        "totalSeconds" to FieldValue.increment(safeDelta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                trackStatRef.set(
                    mapOf(
                        "userId" to uid,
                        "monthKey" to monthKey,
                        "trackKey" to safeTrackKey,
                        "trackTitle" to safeTitle,
                        "artistName" to safeArtist,
                        "albumName" to safeAlbum,
                        "albumId" to safeAlbumId,
                        "playCount" to playIncrement,
                        "totalSeconds" to safeDelta,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, monthDocPath)
            Result.failure(e)
        }
    }

    suspend fun upsertFcmToken(
        userId: String,
        token: String,
        settings: AppSettingsPrefs
    ): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.isBlank() || cleanToken.length > 512) {
            return Result.failure(IllegalArgumentException("Token FCM inválido."))
        }
        val tokenId = ChatNotificationHelper.stableFcmTokenId(cleanToken)
        val docPath = "users/$userId/fcmTokens/$tokenId"
        return try {
            val uid = requireAuthorizedUid(userId)
            val docRef = db.collection("users").document(uid)
                .collection("fcmTokens").document(tokenId)

            val updatePayload = mapOf(
                "token" to cleanToken,
                "notificationsEnabled" to settings.chatPushNotificationsEnabled,
                "onlyActiveRegion" to settings.chatPushOnlyActiveRegion,
                "regionId" to settings.preferredChatRegion.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (docExists(docRef)) {
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mapOf(
                    "userId" to uid,
                    "token" to cleanToken,
                    "notificationsEnabled" to settings.chatPushNotificationsEnabled,
                    "onlyActiveRegion" to settings.chatPushOnlyActiveRegion,
                    "regionId" to settings.preferredChatRegion.id,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeFcmToken(userId: String, token: String): Result<Unit> {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) return Result.success(Unit)
        val tokenId = ChatNotificationHelper.stableFcmTokenId(cleanToken)
        val docPath = "users/$userId/fcmTokens/$tokenId"
        return try {
            val uid = requireAuthorizedUid(userId)
            db.collection("users").document(uid)
                .collection("fcmTokens").document(tokenId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun docExists(docRef: DocumentReference): Boolean {
        val cached = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            null
        }
        if (cached != null && cached.exists()) return true
        return try {
            docRef.get(Source.DEFAULT).await().exists()
        } catch (_: Exception) {
            false
        }
    }

    private companion object {
        const val CHAT_TTL_MS = 78L * 3600L * 1000L
    }
}

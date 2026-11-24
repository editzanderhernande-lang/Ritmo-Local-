package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CloudSyncRepository
import com.example.data.UserPreferencesRepository
import com.example.model.AppSettingsPrefs
import com.example.model.ChatRegion
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

object ChatNotificationHelper {

    const val CHANNEL_ID = "ritmolocal_chat_messages"
    const val CHANNEL_NAME = "Mensajes del Chat Global"
    const val EXTRA_OPEN_CHAT_REGION = "open_chat_region"
    const val TOPIC_ALL_CHAT = "ritmo_chat_all"
    private const val TAG = "ChatNotificationHelper"

    fun topicForRegion(region: ChatRegion): String = "ritmo_chat_${region.id.lowercase()}"

    fun desiredTopicsForPrefs(
        enabled: Boolean,
        onlyActiveRegion: Boolean,
        activeRegion: ChatRegion
    ): Set<String> {
        if (!enabled) return emptySet()
        return if (onlyActiveRegion) {
            setOf(topicForRegion(activeRegion))
        } else {
            ChatRegion.entries.map(::topicForRegion).toSet() + TOPIC_ALL_CHAT
        }
    }

    fun shouldDisplayChatPushNotification(
        chatPushEnabled: Boolean,
        onlyActiveRegion: Boolean,
        preferredRegionId: String,
        incomingRegionId: String?,
        incomingSenderId: String?,
        currentUserId: String?
    ): Boolean {
        if (!chatPushEnabled) return false
        if (!incomingSenderId.isNullOrBlank() &&
            !currentUserId.isNullOrBlank() &&
            incomingSenderId == currentUserId
        ) {
            return false
        }
        if (onlyActiveRegion && !incomingRegionId.isNullOrBlank()) {
            return incomingRegionId.equals(preferredRegionId, ignoreCase = true)
        }
        return true
    }

    fun stableFcmTokenId(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "fcm_${hex.take(24)}"
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas push cuando llegan mensajes o canciones al Chat Global"
                    enableVibration(true)
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    fun showChatMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        regionId: String,
        messageId: String = "",
        recommendedTrackTitle: String = "",
        hasAudioAttachment: Boolean = false
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        ensureChannel(context)
        val region = ChatRegion.fromId(regionId)
        val cleanSender = senderName.trim().ifBlank { "Oyente" }
        val previewLine = buildString {
            append(messageText.trim().ifBlank { "Nuevo mensaje en el chat" })
            if (recommendedTrackTitle.isNotBlank()) {
                append("\n🎵 Recomienda: ").append(recommendedTrackTitle.trim())
            } else if (hasAudioAttachment) {
                append("\n🎧 Compartió un audio descargable")
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_CHAT_REGION, region.id)
        }
        val requestCode = 7200 + region.ordinal
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifId = if (messageId.isNotBlank()) {
            8100 + (messageId.hashCode() and 0x0FFF)
        } else {
            8100 + ((cleanSender + previewLine + System.currentTimeMillis()).hashCode() and 0x0FFF)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_ritmo)
            .setColor(ContextCompat.getColor(context, R.color.ritmo_notif_accent))
            .setContentTitle("Chat Global (${region.displayName}) · $cleanSender")
            .setContentText(previewLine)
            .setStyle(NotificationCompat.BigTextStyle().bigText(previewLine))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
        return true
    }

    suspend fun syncTopicSubscriptions(settings: AppSettingsPrefs) {
        val desired = desiredTopicsForPrefs(
            enabled = settings.chatPushNotificationsEnabled,
            onlyActiveRegion = settings.chatPushOnlyActiveRegion,
            activeRegion = settings.preferredChatRegion
        )
        val allTopics = ChatRegion.entries.map(::topicForRegion).toSet() + TOPIC_ALL_CHAT
        val messaging = runCatching { FirebaseMessaging.getInstance() }.getOrNull() ?: return

        for (topic in allTopics) {
            try {
                if (topic in desired) {
                    messaging.subscribeToTopic(topic).await()
                } else {
                    messaging.unsubscribeFromTopic(topic).await()
                }
            } catch (e: Exception) {
                Log.d(TAG, "Topic sync skipped ($topic): ${e.localizedMessage}")
            }
        }
    }

    suspend fun syncTokenAndSubscriptions(
        context: Context,
        cloudSyncRepository: CloudSyncRepository,
        preferencesRepository: UserPreferencesRepository,
        userId: String?,
        settings: AppSettingsPrefs
    ) {
        ensureChannel(context)
        syncTopicSubscriptions(settings)
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (!token.isNullOrBlank()) {
            preferencesRepository.setCachedFcmToken(token)
            if (userId != null) {
                cloudSyncRepository.upsertFcmToken(userId, token, settings)
            }
        }
    }
}

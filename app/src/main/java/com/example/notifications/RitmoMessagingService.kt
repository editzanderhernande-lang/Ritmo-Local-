package com.example.notifications

import android.util.Log
import com.example.data.CloudSyncRepository
import com.example.data.UserPreferencesRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RitmoMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        serviceScope.launch {
            val prefsRepo = UserPreferencesRepository(applicationContext)
            prefsRepo.setCachedFcmToken(token)
            val settings = prefsRepo.appSettingsPrefs.first()
            ChatNotificationHelper.syncTopicSubscriptions(settings)
            val currentUid = Firebase.auth.currentUser?.uid
            if (currentUid != null) {
                CloudSyncRepository(applicationContext).upsertFcmToken(currentUid, token, settings)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        serviceScope.launch {
            val prefsRepo = UserPreferencesRepository(applicationContext)
            val settings = prefsRepo.appSettingsPrefs.first()
            val data = message.data
            val incomingRegionId = data["regionId"].orEmpty()
            val incomingSenderId = data["senderId"].orEmpty()
            val currentUid = Firebase.auth.currentUser?.uid

            val allowed = ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = settings.chatPushNotificationsEnabled,
                onlyActiveRegion = settings.chatPushOnlyActiveRegion,
                preferredRegionId = settings.preferredChatRegion.id,
                incomingRegionId = incomingRegionId.ifBlank { null },
                incomingSenderId = incomingSenderId.ifBlank { null },
                currentUserId = currentUid
            )
            if (!allowed) {
                Log.d(TAG, "Suppressed chat push notification per user preferences")
                return@launch
            }

            val senderName = data["senderName"]
                ?: message.notification?.title
                ?: "Chat Global"
            val bodyText = data["messageText"]
                ?: message.notification?.body
                ?: "Nuevo mensaje en la comunidad musical"
            val messageId = data["messageId"].orEmpty()
            val recTitle = data["recommendedTrackTitle"].orEmpty()
            val hasAudio = data["hasAudio"] == "true"
            val regionForIntent = incomingRegionId.ifBlank { settings.preferredChatRegion.id }

            ChatNotificationHelper.showChatMessageNotification(
                context = applicationContext,
                senderName = senderName,
                messageText = bodyText,
                regionId = regionForIntent,
                messageId = messageId,
                recommendedTrackTitle = recTitle,
                hasAudioAttachment = hasAudio
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val TAG = "RitmoMessagingService"
    }
}

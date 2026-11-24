package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.AudioClipTrimmer
import com.example.model.ChatRegion
import com.example.model.LocalTrack
import com.example.model.RegionalChatMessage
import com.example.model.UiState
import com.example.model.UiStyleMode
import com.example.ui.components.AudioTrimmerDialog
import com.example.ui.components.UserProfileAvatar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.EmeraldSync
import com.example.ui.theme.LocalUiStyleMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ChatTabFilter(val label: String) {
    GENERAL("General"),
    RECOMMENDATIONS("Recomendaciones"),
    SHARE("Compartir")
}

@Composable
fun RegionalChatScreen(
    isSignedIn: Boolean,
    currentUserId: String?,
    selectedRegion: ChatRegion,
    messagesState: UiState<List<RegionalChatMessage>>,
    localTracks: List<LocalTrack>,
    preselectedTrackForChat: LocalTrack?,
    pendingAudioAttachmentName: String?,
    externalTrimmerUri: Uri? = null,
    isTrimmingAudio: Boolean = false,
    chatStatusMessage: String?,
    customUsername: String = "",
    defaultAccountName: String = "Oyente",
    userAvatarUri: String? = null,
    userAvatarBase64: String = "",
    userPhotoUrl: String? = null,
    chatPushEnabled: Boolean = true,
    onToggleChatPush: (Boolean) -> Unit = {},
    onUpdateCustomUsername: (String) -> Unit = {},
    onPickChatAvatar: () -> Unit = {},
    onClearChatAvatar: () -> Unit = {},
    onSelectRegion: (ChatRegion) -> Unit,
    onRequestSignIn: () -> Unit,
    onPickAudioAttachment: () -> Unit,
    onPickExternalAudioForTrim: () -> Unit = {},
    onTrimAndAttachAudio: (Uri, String, String, Long, Long, () -> Unit) -> Unit = { _, _, _, _, _, _ -> },
    onClearAudioAttachment: () -> Unit,
    onSendMessage: (text: String, recTitle: String, recArtist: String) -> Unit,
    onDownloadAttachedAudio: (RegionalChatMessage) -> Unit,
    onDeleteMessage: (RegionalChatMessage) -> Unit,
    onClearPreselectedTrack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val effectiveChatName = customUsername.trim().ifBlank { defaultAccountName }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var messageText by remember { mutableStateOf("") }
    var recTitle by remember(preselectedTrackForChat) {
        mutableStateOf(preselectedTrackForChat?.title.orEmpty())
    }
    var recArtist by remember(preselectedTrackForChat) {
        mutableStateOf(preselectedTrackForChat?.artist.orEmpty())
    }
    var activeTabFilter by remember { mutableStateOf(ChatTabFilter.GENERAL) }
    var showProfileBar by remember { mutableStateOf(!isNeon) }
    var showTrackPicker by remember { mutableStateOf(false) }
    var showTrimmerDialog by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        if (isNeon) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Chat Global 🌐",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "· ${selectedRegion.displayName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onToggleChatPush(!chatPushEnabled) },
                        modifier = Modifier.testTag("chat_push_bell_toggle")
                    ) {
                        Icon(
                            imageVector = if (chatPushEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                            contentDescription = if (chatPushEnabled) "Alertas push del chat activadas" else "Alertas push del chat silenciadas",
                            tint = if (chatPushEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onPickChatAvatar)
                            .testTag("pick_chat_avatar_button")
                    ) {
                        UserProfileAvatar(
                            avatarUri = userAvatarUri,
                            avatarBase64 = userAvatarBase64,
                            fallbackPhotoUrl = userPhotoUrl,
                            displayName = effectiveChatName,
                            size = 32.dp
                        )
                    }
                    IconButton(onClick = { showProfileBar = !showProfileBar }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Opciones de perfil del chat", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Top 3 mockup pill tabs: General | Recomendaciones | Compartir
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatTabFilter.entries.forEach { tab ->
                    val selected = tab == activeTabFilter
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { activeTabFilter = tab }
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact region strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ChatRegion.entries, key = { it.id }) { region ->
                    val selected = region == selectedRegion
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .border(
                                0.7.dp,
                                if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                RoundedCornerShape(50)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectRegion(region) }
                            .testTag("chat_region_${region.id}")
                    ) {
                        Text(
                            text = region.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.regionalChatTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { onToggleChatPush(!chatPushEnabled) },
                    modifier = Modifier.testTag("chat_push_bell_toggle")
                ) {
                    Icon(
                        imageVector = if (chatPushEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                        contentDescription = if (chatPushEnabled) "Alertas push del chat activadas" else "Alertas push del chat silenciadas",
                        tint = if (chatPushEnabled) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Comparte canciones de hasta 5 min (5.7 MB). El chat se borra tras 78 h, pero tus descargas se conservan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ChatRegion.entries, key = { it.id }) { region ->
                    val selected = region == selectedRegion
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectRegion(region) },
                        label = { Text(region.displayName) },
                        leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary.copy(alpha = 0.22f),
                            selectedLabelColor = primary,
                            selectedLeadingIconColor = primary
                        ),
                        modifier = Modifier.testTag("chat_region_${region.id}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isSignedIn) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, tint = primary, modifier = Modifier.size(40.dp))
                    Text(
                        text = "Chat Global por Región",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Estás en modo offline. Inicia sesión con Google para chatear con otros usuarios de ${selectedRegion.displayName}, pedir recomendaciones y descargar música compartida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onRequestSignIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("chat_sign_in_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        if (isSignedIn && (!isNeon || showProfileBar)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isNeon) {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onPickChatAvatar)
                                .testTag("pick_chat_avatar_button")
                        ) {
                            UserProfileAvatar(
                                avatarUri = userAvatarUri,
                                avatarBase64 = userAvatarBase64,
                                fallbackPhotoUrl = userPhotoUrl,
                                displayName = effectiveChatName,
                                size = 38.dp
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showUsernameDialog = true }
                            .testTag("chat_edit_username_button")
                    ) {
                        Text(
                            text = effectiveChatName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Toca aquí para cambiar alias · Auto-borrado 78h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onPickChatAvatar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.AddAPhoto, contentDescription = "Cambiar foto de perfil", tint = primary, modifier = Modifier.size(18.dp))
                    }
                    if (!userAvatarUri.isNullOrBlank() || userAvatarBase64.isNotBlank()) {
                        IconButton(
                            onClick = onClearChatAvatar,
                            modifier = Modifier.size(32.dp).testTag("clear_chat_avatar_button")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Quitar foto", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = { showUsernameDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Cambiar nombre", tint = primary, modifier = Modifier.size(16.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                        Text(
                            text = "78h",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        chatStatusMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSync.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldSync,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Messages Feed
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(color = primary, modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Text(
                        text = messagesState.message,
                        color = CoralPulse,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Success -> {
                    val allMessages = messagesState.data
                    val messages = remember(allMessages, activeTabFilter, isNeon) {
                        if (!isNeon) allMessages
                        else when (activeTabFilter) {
                            ChatTabFilter.GENERAL -> allMessages
                            ChatTabFilter.RECOMMENDATIONS -> allMessages.filter { it.recommendedTrackTitle.isNotBlank() }
                            ChatTabFilter.SHARE -> allMessages.filter { it.hasDownloadableAudio() }
                        }
                    }
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Forum, contentDescription = null, tint = primary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sé el primero en recomendar música en ${selectedRegion.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages, key = { it.id.ifBlank { "${it.senderId}_${it.messageText.hashCode()}" } }) { msg ->
                                val isMine = msg.senderId == currentUserId
                                val timeLabel = msg.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: ""
                                if (isNeon) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isMine) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                0.7.dp,
                                                if (isMine) primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(18.dp)
                                            )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            UserProfileAvatar(
                                                avatarUri = if (isMine) userAvatarUri else null,
                                                avatarBase64 = msg.senderAvatarBase64.ifBlank { if (isMine) userAvatarBase64 else "" },
                                                fallbackPhotoUrl = if (isMine) userPhotoUrl else null,
                                                displayName = msg.senderName,
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = buildAnnotatedString {
                                                            withStyle(
                                                                SpanStyle(
                                                                    color = if (isMine) primary else secondary,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            ) {
                                                                append("${msg.senderName}: ")
                                                            }
                                                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                                                                append(msg.messageText)
                                                            }
                                                        },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (timeLabel.isNotBlank()) {
                                                            Text(
                                                                text = timeLabel,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        if (isMine && msg.id.isNotBlank()) {
                                                            Icon(
                                                                Icons.Rounded.Delete,
                                                                contentDescription = "Eliminar mensaje",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier
                                                                    .size(15.dp)
                                                                    .clickable { onDeleteMessage(msg) }
                                                            )
                                                        }
                                                    }
                                                }
                                                if (msg.recommendedTrackTitle.isNotBlank()) {
                                                    Text(
                                                        text = "🎵 ${msg.recommendedTrackTitle}" + if (msg.recommendedArtist.isNotBlank()) " — ${msg.recommendedArtist}" else "",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (msg.hasDownloadableAudio()) {
                                                    val sizeLabel = if (msg.audioSizeBytes > 0L) " (${AudioClipTrimmer.formatSizeReadable(msg.audioSizeBytes)})" else ""
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { onDownloadAttachedAudio(msg) }
                                                            .padding(vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Download, contentDescription = null, tint = secondary, modifier = Modifier.size(15.dp))
                                                        Text(
                                                            text = "[Descargar ${msg.audioFileName.ifBlank { "audio" }}$sizeLabel]",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = secondary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isMine) primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(
                                                1.dp,
                                                if (isMine) primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    UserProfileAvatar(
                                                        avatarUri = if (isMine) userAvatarUri else null,
                                                        avatarBase64 = msg.senderAvatarBase64.ifBlank { if (isMine) userAvatarBase64 else "" },
                                                        fallbackPhotoUrl = if (isMine) userPhotoUrl else null,
                                                        displayName = msg.senderName,
                                                        size = 32.dp
                                                    )
                                                    Text(
                                                        text = "${msg.senderName} · ${msg.regionId}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (isMine) primary else secondary,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (isMine && msg.id.isNotBlank()) {
                                                    IconButton(
                                                        onClick = { onDeleteMessage(msg) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Delete,
                                                            contentDescription = "Eliminar mensaje",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = msg.messageText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (msg.recommendedTrackTitle.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = primary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = msg.recommendedTrackTitle,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            if (msg.recommendedArtist.isNotBlank()) {
                                                                Text(
                                                                    text = msg.recommendedArtist,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (msg.hasDownloadableAudio()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                val sizeLabel = if (msg.audioSizeBytes > 0L) {
                                                    " (${AudioClipTrimmer.formatSizeReadable(msg.audioSizeBytes)})"
                                                } else ""
                                                FilledTonalButton(
                                                    onClick = { onDownloadAttachedAudio(msg) },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "${strings.downloadAudio}: ${msg.audioFileName.ifBlank { "audio.m4a" }}$sizeLabel",
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Composer (clean pill bar in NEON_MODERN matching Mockup Panel 10)
        Surface(
            shape = RoundedCornerShape(if (isNeon) 26.dp else 18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (recTitle.isNotBlank() || pendingAudioAttachmentName != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (recTitle.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = primary.copy(alpha = 0.16f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "♫ $recTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Quitar recomendación",
                                        tint = primary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                recTitle = ""
                                                recArtist = ""
                                                onClearPreselectedTrack()
                                            }
                                    )
                                }
                            }
                        }
                        if (pendingAudioAttachmentName != null) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = secondary.copy(alpha = 0.16f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📎 $pendingAudioAttachmentName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = secondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Quitar archivo",
                                        tint = secondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable(onClick = onClearAudioAttachment)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isNeon) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTrackPicker = true },
                            enabled = localTracks.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Recomendar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = { showTrimmerDialog = true },
                            modifier = Modifier.weight(1f).testTag("chat_trim_audio_button")
                        ) {
                            Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Recortar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        OutlinedButton(
                            onClick = onPickAudioAttachment,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("≤5.7MB", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isNeon) {
                        IconButton(
                            onClick = { showTrackPicker = true },
                            enabled = localTracks.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = "Recomendar canción", tint = primary, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { showTrimmerDialog = true },
                            modifier = Modifier.size(36.dp).testTag("chat_trim_audio_button")
                        ) {
                            Icon(Icons.Rounded.ContentCut, contentDescription = "Recortar audio", tint = secondary, modifier = Modifier.size(19.dp))
                        }
                        IconButton(
                            onClick = onPickAudioAttachment,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = "Adjuntar archivo hasta 5.7 MB", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                        }
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = {
                            Text(
                                if (isNeon) "Escribe un mensaje…" else "Pide o recomienda música en ${selectedRegion.displayName}…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = if (isNeon) {
                            OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = primary.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        },
                        modifier = Modifier.weight(1f).testTag("chat_message_input")
                    )

                    FilledIconButton(
                        onClick = {
                            if (messageText.isNotBlank() || recTitle.isNotBlank() || pendingAudioAttachmentName != null) {
                                val safeMsg = messageText.trim().ifBlank { "Comparto una recomendación musical 🎵" }
                                onSendMessage(safeMsg, recTitle, recArtist)
                                messageText = ""
                                recTitle = ""
                                recArtist = ""
                                onClearPreselectedTrack()
                            }
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(46.dp).testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }

    if (showTrackPicker) {
        AlertDialog(
            onDismissRequest = { showTrackPicker = false },
            title = { Text("Recomendar canción de tu biblioteca") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(localTracks.take(40), key = { it.trackKey }) { track ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    recTitle = track.title
                                    recArtist = track.artist
                                    showTrackPicker = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrackPicker = false }) { Text("Cerrar") }
            }
        )
    }

    if (showTrimmerDialog) {
        AudioTrimmerDialog(
            localTracks = localTracks,
            externalSourceUri = externalTrimmerUri,
            isBusyTrimming = isTrimmingAudio,
            onPickExternalFile = onPickExternalAudioForTrim,
            onConfirmTrim = { uri, title, artist, startMs, endMs ->
                onTrimAndAttachAudio(uri, title, artist, startMs, endMs) {
                    if (recTitle.isBlank()) {
                        recTitle = title
                        recArtist = artist
                    }
                    showTrimmerDialog = false
                }
            },
            onDismiss = { showTrimmerDialog = false }
        )
    }

    if (showUsernameDialog) {
        var draftName by remember { mutableStateOf(customUsername.ifBlank { defaultAccountName }) }
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Nombre de usuario en el Chat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Este nombre aparecerá cuando envíes mensajes o compartas canciones en todas las salas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it.take(40) },
                        singleLine = true,
                        label = { Text("Tu alias") },
                        placeholder = { Text("Ej: DJ Zander") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_username_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCustomUsername(draftName)
                        showUsernameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.testTag("chat_username_save_button")
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

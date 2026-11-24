package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.i18n.extraStringsFor
import com.example.voice.VoiceAudioRoute
import com.example.voice.VoiceUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandSheet(
    voiceState: VoiceUiState,
    activeLanguage: AppLanguage,
    selectedVoiceLang: AppLanguage,
    handsFreeEnabled: Boolean = false,
    screenOffEnabled: Boolean = true,
    onToggleHandsFree: (Boolean) -> Unit = {},
    onToggleScreenOff: (Boolean) -> Unit = {},
    onSelectVoiceLanguage: (AppLanguage) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSubmitTextCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extra = extraStringsFor(activeLanguage)
    val primary = MaterialTheme.colorScheme.primary
    var manualCommand by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(primary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, tint = primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(extra.voiceControlTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (voiceState.isListening) extra.voiceListeningNow else extra.voiceTapToSpeak,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (voiceState.isListening) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isBluetooth = voiceState.audioRoute == VoiceAudioRoute.BLUETOOTH_HEADSET
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = primary.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_dsp_status_chip")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isBluetooth) Icons.Rounded.BluetoothAudio else Icons.Rounded.SurroundSound,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                    val routeSummary = if (isBluetooth) {
                        "Auriculares Bluetooth · Escucha sin pausar la música ni cortar A2DP"
                    } else {
                        val flags = listOfNotNull(
                            if (voiceState.aecActive) "AEC" else null,
                            if (voiceState.noiseSuppressionActive) "Reducción de ruido" else null
                        )
                        val dspText = if (flags.isNotEmpty()) flags.joinToString(" + ") else "Filtro anti-ruido"
                        "Altavoz del teléfono · Pausa temporal al escuchar ($dspText)"
                    }
                    Text(
                        text = routeSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(extra.handsFreeWakeTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = extra.handsFreeWakeSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = handsFreeEnabled,
                            onCheckedChange = onToggleHandsFree,
                            modifier = Modifier.testTag("voice_hands_free_switch")
                        )
                    }
                    if (handsFreeEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(extra.screenOffVoiceTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = extra.screenOffVoiceSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = screenOffEnabled,
                                onCheckedChange = onToggleScreenOff,
                                modifier = Modifier.testTag("voice_screen_off_switch")
                            )
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(AppLanguage.entries, key = { it.code }) { lang ->
                    FilterChip(
                        selected = lang == selectedVoiceLang,
                        onClick = { onSelectVoiceLanguage(lang) },
                        label = { Text("${lang.flagEmoji} ${lang.code.uppercase()}") }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FilledIconButton(
                    onClick = { if (voiceState.isListening) onStopListening() else onStartListening() },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (voiceState.isListening) MaterialTheme.colorScheme.secondary else primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("voice_sheet_mic_button")
                ) {
                    Icon(
                        imageVector = if (voiceState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = if (voiceState.isListening) "Detener voz" else "Hablar",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            val shownTranscript = voiceState.partialTranscript.ifBlank { voiceState.lastTranscript }
            if (shownTranscript.isNotBlank() || !voiceState.statusMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (shownTranscript.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "\"$shownTranscript\"",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        voiceState.statusMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text(
                text = extra.voiceHintExamples,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Quick voice phrases & offline text-command fallback
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val quickCommands = listOf(
                    "Ridmo siguiente canción",
                    "Ritmo anterior canción",
                    "Ritmo subir volumen",
                    "Ritmo bajar volumen",
                    "Ritmo ver letra",
                    "Ritmo pausar",
                    "Ritmo reproducir"
                )
                items(quickCommands) { cmd ->
                    FilterChip(
                        selected = false,
                        onClick = { onSubmitTextCommand(cmd) },
                        label = { Text(cmd) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualCommand,
                    onValueChange = { manualCommand = it },
                    placeholder = { Text("O escribe: \"Ritmo reproducir [canción] de [artista]\"") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("voice_manual_command_input")
                )
                IconButton(
                    onClick = {
                        if (manualCommand.isNotBlank()) {
                            onSubmitTextCommand(manualCommand.trim())
                            manualCommand = ""
                        }
                    },
                    modifier = Modifier.testTag("voice_manual_command_send")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Ejecutar comando", tint = primary)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

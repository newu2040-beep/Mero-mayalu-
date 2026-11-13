package com.example.feature.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.LocalMeroFontFamily
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.ChatMessage
import com.example.core.domain.Conversation
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.MediaHelper
import com.example.core.util.RecordedVoiceNote
import com.example.core.util.VoiceNotePlayer
import com.example.core.util.VoiceNoteRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    currentUid: String,
    language: AppLanguage,
    conversationFlow: Flow<Conversation?>,
    messagesFlow: Flow<List<ChatMessage>>,
    onBack: () -> Unit,
    onViewPartnerProfile: (String) -> Unit,
    onSendText: (Conversation, String) -> Unit,
    onSendImage: (Conversation, String) -> Unit,
    onSendVoice: (Conversation, RecordedVoiceNote) -> Unit,
    onToggleMute: (Conversation) -> Unit,
    onBlockPartner: (String) -> Unit,
    onReportPartner: (String) -> Unit,
    onShowError: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val conversation by conversationFlow.collectAsStateWithLifecycle(initialValue = null)
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var draft by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var liveAmp by remember { mutableIntStateOf(20) }
    var recordedDraft by remember { mutableStateOf<RecordedVoiceNote?>(null) }
    var playingMessageId by remember { mutableStateOf<String?>(null) }

    val recorder = remember { VoiceNoteRecorder(context) }
    val player = remember { VoiceNotePlayer(context) }

    DisposableEffect(Unit) {
        onDispose {
            recorder.cancel()
            player.stop()
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            liveAmp = recorder.pollAmplitude()
            delay(150L)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val conv = conversation
        if (uri != null && conv != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri, maxDimension = 560, quality = 65)
                if (compressed != null) {
                    onSendImage(conv, compressed)
                }
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (recorder.start()) isRecording = true
        } else {
            onShowError(s.micPermissionNeeded)
        }
    }

    val conv = conversation
    val isUserA = conv?.userAId == currentUid
    val partnerUid = if (isUserA) conv?.userBId.orEmpty() else conv?.userAId.orEmpty()
    val partnerName = if (isUserA) conv?.userBName.orEmpty() else conv?.userAName.orEmpty()
    val partnerPhoto = if (isUserA) conv?.userBPhoto.orEmpty() else conv?.userAPhoto.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Screen 4 Chat Header
        Surface(
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = MeroSpacing.sm)
            ) {
                MeroIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s.back,
                    onClick = onBack,
                    testTag = "chat_back_button"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(MeroRadius.small))
                        .clickable(enabled = partnerUid.isNotBlank()) { onViewPartnerProfile(partnerUid) }
                        .padding(vertical = MeroSpacing.xs)
                ) {
                    MeroAvatarImage(partnerPhoto, partnerName.ifBlank { "?" }, 40.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partnerName.ifBlank { "..." },
                            style = MeroType.Heading.copy(color = colors.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conv?.status == "MUTED") {
                            Text(
                                text = s.chatMutedBadge,
                                style = MeroType.Caption.copy(color = colors.textMuted)
                            )
                        }
                    }
                }
                Box {
                    MeroIconButton(
                        icon = Icons.Filled.MoreVert,
                        contentDescription = s.settings,
                        onClick = { menuExpanded = true },
                        testTag = "chat_menu_button"
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (partnerUid.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(s.viewProfile, style = MeroType.Body) },
                                onClick = { menuExpanded = false; onViewPartnerProfile(partnerUid) }
                            )
                        }
                        if (conv != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (conv.status == "MUTED") s.unmuteChat else s.muteChat,
                                        style = MeroType.Body
                                    )
                                },
                                onClick = { menuExpanded = false; onToggleMute(conv) }
                            )
                        }
                        if (partnerUid.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text(s.reportUser, style = MeroType.Body) },
                                onClick = { menuExpanded = false; onReportPartner(partnerUid) }
                            )
                            DropdownMenuItem(
                                text = { Text(s.blockUser, style = MeroType.Body.copy(color = colors.danger)) },
                                onClick = { menuExpanded = false; onBlockPartner(partnerUid) }
                            )
                        }
                    }
                }
            }
        }

        // Message stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MeroSpacing.lg)
                ) {
                    MeroEmptyState(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = partnerName.ifBlank { s.appName },
                        body = s.emptyMessages
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(MeroSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isMe = msg.senderUid == currentUid
                        MessageBubble(
                            message = msg,
                            isMe = isMe,
                            isPlaying = playingMessageId == msg.id,
                            onToggleVoicePlay = {
                                if (playingMessageId == msg.id) {
                                    player.stop()
                                    playingMessageId = null
                                } else if (msg.mediaData.isNotBlank()) {
                                    playingMessageId = msg.id
                                    player.play(msg.id, msg.mediaData) {
                                        playingMessageId = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Voice preview bar if recorded
        if (recordedDraft != null || isRecording) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                ) {
                    Text(
                        text = if (isRecording) {
                            "${s.recordingVoice} (${liveAmp}%)"
                        } else {
                            "${s.voicePreviewReady} (${recordedDraft?.durationSec ?: 1}s)"
                        },
                        style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f)
                    )
                    if (isRecording) {
                        MeroIconButton(
                            icon = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint = colors.danger,
                            onClick = {
                                isRecording = false
                                recordedDraft = recorder.stopAndEncode()
                            },
                            testTag = "voice_stop_button"
                        )
                    } else {
                        MeroIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = s.cancel,
                            onClick = { recordedDraft = null },
                            testTag = "voice_discard_button"
                        )
                        if (conv != null && recordedDraft != null) {
                            MeroIconButton(
                                icon = Icons.AutoMirrored.Filled.Send,
                                contentDescription = s.send,
                                tint = colors.primary,
                                onClick = {
                                    val note = recordedDraft
                                    recordedDraft = null
                                    if (note != null) onSendVoice(conv, note)
                                },
                                testTag = "voice_send_button"
                            )
                        }
                    }
                }
            }
        }

        // Screen 4 Floating Pill Composer
        Surface(
            shape = RoundedCornerShape(MeroRadius.pill),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = MeroSpacing.sm)
                ) {
                    val fontFamily = LocalMeroFontFamily.current
                    if (draft.isEmpty()) {
                        Text(
                            text = s.typeMessageHint,
                            style = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, color = colors.textMuted),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { if (it.length <= 2000) draft = it },
                        textStyle = TextStyle(fontFamily = fontFamily, fontSize = 16.sp, color = colors.text),
                        cursorBrush = SolidColor(colors.primary),
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("chat_message_input")
                    )
                }

                MeroIconButton(
                    icon = Icons.Filled.Image,
                    contentDescription = s.selectPhoto,
                    tint = colors.textMuted,
                    onClick = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    testTag = "chat_pick_image_button"
                )

                MeroIconButton(
                    icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = s.recordVoiceHint,
                    tint = if (isRecording) colors.danger else colors.textMuted,
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            recordedDraft = recorder.stopAndEncode()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                if (recorder.start()) isRecording = true
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    testTag = "chat_voice_button"
                )

                MeroIconButton(
                    icon = Icons.AutoMirrored.Filled.Send,
                    contentDescription = s.send,
                    tint = colors.onPrimary,
                    containerColor = if (draft.isNotBlank() && conv != null) colors.primary else colors.surface2,
                    enabled = draft.isNotBlank() && conv != null,
                    onClick = {
                        val c = conv ?: return@MeroIconButton
                        val msg = draft.trim()
                        if (msg.isNotEmpty()) {
                            draft = ""
                            onSendText(c, msg)
                        }
                    },
                    testTag = "chat_send_button"
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isPlaying: Boolean,
    onToggleVoicePlay: () -> Unit
) {
    val colors = LocalMeroColors.current
    val bubbleBg = if (isMe) colors.primary else colors.surface
    val bubbleFg = if (isMe) colors.onPrimary else colors.text
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate()
        if (date != null) SimpleDateFormat("h:mm a", Locale.getDefault()).format(date) else ""
    }

    Row(
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(MeroRadius.card),
            color = bubbleBg,
            border = if (isMe) null else BorderStroke(1.dp, colors.border),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                modifier = Modifier.padding(MeroSpacing.md)
            ) {
                when (message.type) {
                    "IMAGE" -> {
                        MeroPhotoBox(
                            photoData = message.mediaData,
                            contentDescription = "Shared photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(MeroRadius.small))
                        )
                    }
                    "VOICE" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MeroIconButton(
                                icon = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = bubbleBg,
                                containerColor = bubbleFg,
                                onClick = onToggleVoicePlay,
                                testTag = "voice_play_${message.id}"
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                val bars = if (message.waveform.isNotEmpty()) message.waveform.take(18) else List(14) { 45 }
                                bars.forEach { amp ->
                                    val barH = ((amp.coerceIn(10, 100) / 100f) * 24f).dp
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(barH)
                                            .clip(RoundedCornerShape(MeroRadius.pill))
                                            .background(bubbleFg.copy(alpha = 0.8f))
                                    )
                                }
                            }
                            Text(
                                text = "0:${message.audioDurationSec.toString().padStart(2, '0')}",
                                style = MeroType.Caption.copy(color = bubbleFg)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            style = MeroType.Body.copy(color = bubbleFg)
                        )
                    }
                }
                if (timeText.isNotBlank()) {
                    Text(
                        text = timeText,
                        style = MeroType.Caption.copy(color = bubbleFg.copy(alpha = 0.78f)),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

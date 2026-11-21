package com.example.feature.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Base64
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.audio.MeroSoundAndHapticHelper
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.LocalMeroFontFamily
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroEmptyState
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroPresenceDot
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.ChatMessage
import com.example.core.domain.Conversation
import com.example.core.domain.MeetupInvitation
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.LocationServiceHelper
import com.example.core.util.MediaHelper
import com.example.core.util.RecordedVoiceNote
import com.example.core.util.VideoUploadResult
import com.example.core.util.VoiceNotePlayer
import com.example.core.util.VoiceNoteRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class ExtractedChatLink(
    val raw: String,
    val normalizedUri: String,
    val displayLabel: String,
    val start: Int,
    val end: Int
)

object ChatLinkHelper {
    private val LINK_REGEX = Regex(
        "(?i)(https?://[^\\s<>\"']+|www\\.[^\\s<>\"']+|mailto:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|tel:\\+?[0-9\\-()]{7,18}|(?:[A-Za-z0-9-]+\\.)+(?:com|org|net|edu|gov|np|io|co|me|app|dev|in|uk|info|tv)(?:/[^\\s<>\"']*)?)"
    )

    fun extractLinks(text: String): List<ExtractedChatLink> {
        if (text.isBlank()) return emptyList()
        return LINK_REGEX.findAll(text).mapNotNull { match ->
            var raw = match.value
            var end = match.range.last + 1
            while (raw.isNotEmpty() && raw.last() in charArrayOf('.', ',', '!', '?', ';', ':', ')', ']', '}')) {
                raw = raw.dropLast(1)
                end -= 1
            }
            if (raw.length < 4) return@mapNotNull null
            val lower = raw.lowercase()
            val normalized = when {
                lower.startsWith("http://") || lower.startsWith("https://") -> raw
                lower.startsWith("mailto:") || lower.startsWith("tel:") -> raw
                else -> "https://$raw"
            }
            val label = when {
                lower.startsWith("mailto:") -> raw.removePrefix("mailto:")
                lower.startsWith("tel:") -> raw.removePrefix("tel:")
                else -> raw.removePrefix("https://").removePrefix("http://").removePrefix("www.").take(36)
            }
            ExtractedChatLink(
                raw = raw,
                normalizedUri = normalized,
                displayLabel = label,
                start = match.range.first,
                end = end
            )
        }.toList()
    }

    fun openLinkSafely(context: Context, uriString: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun quickRepliesFor(gender: String, isNepali: Boolean): List<String> {
        val isMale = gender.equals("Male", ignoreCase = true)
        val isFemale = gender.equals("Female", ignoreCase = true)
        return when {
            isMale && isNepali -> listOf(
                "नमस्ते! आज तपाईंको दिन कस्तो बित्दैछ? 😊",
                "तपाईंलाई कस्तो नेपाली खाना र चिया मन पर्छ? ☕",
                "कुनै मनपर्ने यात्रा वा सम्पदा स्थल बताउनुहोस् न 🏔️",
                "तपाईंको प्रोफाइल र रुचिहरू निकै प्रेरक लाग्यो ✨"
            )
            isMale -> listOf(
                "Namaste! How is your day going today? 😊",
                "What's your favorite local cafe or hangout spot? ☕",
                "Loved your interests — what hobbies keep you inspired? ✨",
                "Do you enjoy mountain views or cultural walks more? 🏔️"
            )
            isFemale && isNepali -> listOf(
                "नमस्ते! सुरुमा आत्मीय र सभ्य चिनजान गरौँ न 🌸",
                "तपाईंको खाली समयमा सबैभन्दा मनपर्ने काम के हो? ✨",
                "मलाई पनि शान्त वातावरण र चिया गफ निकै मन पर्छ ☕",
                "नेपालको कुन ठाउँ घुम्न तपाईंलाई विशेष मन पर्छ? 🏔️"
            )
            isFemale -> listOf(
                "Namaste! Let's start with getting to know each other's interests 🌸",
                "What kind of music, books, or weekend activities do you enjoy? ✨",
                "Always appreciate kind and respectful conversation ☕",
                "Which peaceful spot in Nepal feels closest to your heart? 🏔️"
            )
            isNepali -> listOf(
                "नमस्ते! आत्मीय चिनजान गरौँ न 😊",
                "तपाईंको रुचि र दिनचर्या कस्तो छ? ✨",
                "चिया गफ र संस्कृतिको बारेमा कुरा गरौँ न ☕"
            )
            else -> listOf(
                "Namaste! Nice connecting with you 😊",
                "What interests are you most passionate about? ✨",
                "Always happy to chat about Nepal, food & travel ☕"
            )
        }
    }
}

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
    onDeleteMessage: (Conversation, ChatMessage) -> Unit,
    onToggleMute: (Conversation) -> Unit,
    onBlockPartner: (String) -> Unit,
    onReportPartner: (String) -> Unit,
    onShowError: (String) -> Unit,
    meetupInvites: List<MeetupInvitation> = emptyList(),
    observePartnerProfile: ((String) -> Flow<PublicProfile?>)? = null,
    onStartAudioCall: (Conversation) -> Unit = {},
    onOpenTogetherMemories: (Conversation) -> Unit = {},
    onRespondMeetupInvite: (String, Boolean) -> Unit = { _, _ -> },
    myGender: String = "",
    hapticEnabled: Boolean = true,
    onSendVideo: (Conversation, String, Int) -> Unit = { _, _, _ -> }
) {
    BackHandler(onBack = onBack)

    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
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
    var deleteConfirmTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var pendingImagePreview by remember { mutableStateOf<String?>(null) }
    var isProcessingChatImage by remember { mutableStateOf(false) }
    var chatImageError by remember { mutableStateOf<String?>(null) }
    var lastFailedChatImageUri by remember { mutableStateOf<Uri?>(null) }

    var pendingVideoPreview by remember { mutableStateOf<VideoUploadResult.Success?>(null) }
    var isProcessingChatVideo by remember { mutableStateOf(false) }
    var chatVideoError by remember { mutableStateOf<String?>(null) }
    var lastFailedChatVideoUri by remember { mutableStateOf<Uri?>(null) }
    var activeVideoToPlay by remember { mutableStateOf<ChatMessage?>(null) }

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

    fun handleChatImageUri(uri: Uri) {
        scope.launch {
            isProcessingChatImage = true
            chatImageError = null
            lastFailedChatImageUri = null
            when (val res = MediaHelper.processImageUriWithResult(context, uri, hd = true)) {
                is com.example.core.util.MediaUploadResult.Success -> {
                    pendingImagePreview = res.dataUri
                }
                is com.example.core.util.MediaUploadResult.Error -> {
                    lastFailedChatImageUri = uri
                    chatImageError = if (language == AppLanguage.NEPALI) res.messageNe else res.messageEn
                }
            }
            isProcessingChatImage = false
        }
    }

    fun handleChatVideoUri(uri: Uri) {
        scope.launch {
            isProcessingChatVideo = true
            chatVideoError = null
            lastFailedChatVideoUri = null
            when (val res = MediaHelper.processVideoUriWithResult(context, uri)) {
                is VideoUploadResult.Success -> {
                    pendingVideoPreview = res
                }
                is VideoUploadResult.Error -> {
                    lastFailedChatVideoUri = uri
                    chatVideoError = if (language == AppLanguage.NEPALI) res.messageNe else res.messageEn
                }
            }
            isProcessingChatVideo = false
        }
    }

    val legacyImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) handleChatImageUri(uri)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handleChatImageUri(uri)
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handleChatVideoUri(uri)
    }

    val videoFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) handleChatVideoUri(uri)
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
    val callMicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            conv?.let(onStartAudioCall)
        } else {
            onShowError(s.micPermissionNeeded)
        }
    }

    val isUserA = conv?.userAId == currentUid
    val partnerUid = if (isUserA) conv?.userBId.orEmpty() else conv?.userAId.orEmpty()
    val partnerName = if (isUserA) conv?.userBName.orEmpty() else conv?.userAName.orEmpty()
    val partnerPhoto = if (isUserA) conv?.userBPhoto.orEmpty() else conv?.userAPhoto.orEmpty()

    val activeMeetupInvites = remember(meetupInvites, conv?.id) {
        if (conv == null) emptyList()
        else meetupInvites.filter { it.conversationId == conv.id }
    }

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
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
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
                    val partnerProfileState = remember(partnerUid, observePartnerProfile) {
                        if (partnerUid.isNotBlank() && observePartnerProfile != null) {
                            observePartnerProfile(partnerUid)
                        } else null
                    }?.collectAsStateWithLifecycle(initialValue = null)
                    val partnerProfile = partnerProfileState?.value
                    val isPartnerOnline = LocationServiceHelper.isProfileOnline(partnerProfile)
                    val presenceText = if (partnerProfile != null) {
                        LocationServiceHelper.formatPresenceLabel(partnerProfile, isNe)
                    } else ""

                    Box(contentAlignment = Alignment.BottomEnd) {
                        MeroAvatarImage(partnerPhoto, partnerName.ifBlank { "?" }, 40.dp)
                        if (partnerProfile != null) {
                            MeroPresenceDot(
                                isOnline = isPartnerOnline,
                                size = 12.dp,
                                borderColor = colors.surface,
                                modifier = Modifier.testTag("chat_header_presence_dot")
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partnerName.ifBlank { "..." },
                            style = MeroType.Heading.copy(color = colors.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusLine = when {
                            conv?.status == "MUTED" -> s.chatMutedBadge
                            presenceText.isNotBlank() -> presenceText
                            else -> ""
                        }
                        if (statusLine.isNotBlank()) {
                            Text(
                                text = statusLine,
                                style = MeroType.Caption.copy(
                                    color = if (conv?.status != "MUTED" && isPartnerOnline) colors.success else colors.textMuted,
                                    fontWeight = if (conv?.status != "MUTED" && isPartnerOnline) FontWeight.Medium else FontWeight.Normal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (conv != null) {
                    MeroIconButton(
                        icon = Icons.Filled.Collections,
                        contentDescription = if (isNe) "साझा सम्झनाहरू" else "Together Memories",
                        tint = colors.primary,
                        onClick = { onOpenTogetherMemories(conv) },
                        testTag = "chat_memories_button"
                    )
                    MeroIconButton(
                        icon = Icons.Filled.Call,
                        contentDescription = if (isNe) "अडियो कल" else "Audio Call",
                        tint = colors.primary,
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                onStartAudioCall(conv)
                            } else {
                                callMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        testTag = "chat_audio_call_button"
                    )
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
                                        text = if (isNe) "साझा सम्झनाहरू 🌸" else "Together Memories 🌸",
                                        style = MeroType.Body
                                    )
                                },
                                onClick = { menuExpanded = false; onOpenTogetherMemories(conv) }
                            )
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

        // Virtual Meetup Invitations Banner inside Chat
        activeMeetupInvites.take(1).forEach { invite ->
            Surface(
                shape = RoundedCornerShape(MeroRadius.small),
                color = colors.surface2,
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    Text(
                        text = "📅 ${invite.planTitle} (${invite.status})",
                        style = MeroType.Body.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${invite.proposedDateLabel} · ${invite.city} · NPR ${invite.estimatedBudgetNpr}",
                        style = MeroType.Caption.copy(color = colors.text)
                    )
                    if (invite.stopsPreview.isNotEmpty()) {
                        Text(
                            text = invite.stopsPreview.joinToString(" → "),
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                    if (invite.recipientUid == currentUid && invite.status == "PENDING") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MeroButton(
                                text = if (isNe) "स्वीकार्नुहोस्" else "Accept Meetup",
                                onClick = { onRespondMeetupInvite(invite.id, true) },
                                modifier = Modifier.weight(1f),
                                testTag = "accept_meetup_${invite.id}"
                            )
                            MeroSecondaryButton(
                                text = if (isNe) "अस्वीकार" else "Decline",
                                isDanger = true,
                                onClick = { onRespondMeetupInvite(invite.id, false) },
                                modifier = Modifier.weight(1f),
                                testTag = "decline_meetup_${invite.id}"
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
                            deleteLabel = s.deleteMessageLabel,
                            onToggleVoicePlay = {
                                MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                if (playingMessageId == msg.id) {
                                    player.stop()
                                    playingMessageId = null
                                } else if (msg.mediaData.isNotBlank()) {
                                    playingMessageId = msg.id
                                    player.play(msg.id, msg.mediaData) {
                                        playingMessageId = null
                                    }
                                }
                            },
                            onOpenVideo = {
                                MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                activeVideoToPlay = msg
                            },
                            onRequestDelete = if (isMe) {
                                { deleteConfirmTarget = msg }
                            } else null
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

        if (isProcessingChatImage || isProcessingChatVideo) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Text(
                    text = if (isProcessingChatVideo) {
                        if (isNe) "HD भिडियो तयार हुँदैछ..." else "Preparing HD video preview..."
                    } else {
                        if (isNe) "HD तस्बिर तयार हुँदैछ..." else "Preparing HD image preview..."
                    },
                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.padding(MeroSpacing.md)
                )
            }
        }

        if (chatImageError != null) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.danger.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    Text(
                        text = chatImageError.orEmpty(),
                        style = MeroType.Caption.copy(color = colors.danger),
                        modifier = Modifier.weight(1f)
                    )
                    if (lastFailedChatImageUri != null) {
                        MeroSecondaryButton(
                            text = s.retry,
                            onClick = {
                                val retryUri = lastFailedChatImageUri
                                if (retryUri != null) handleChatImageUri(retryUri)
                            },
                            testTag = "chat_retry_image_button"
                        )
                    }
                }
            }
        }

        if (chatVideoError != null) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.danger.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    Text(
                        text = chatVideoError.orEmpty(),
                        style = MeroType.Caption.copy(color = colors.danger),
                        modifier = Modifier.weight(1f)
                    )
                    if (lastFailedChatVideoUri != null) {
                        MeroSecondaryButton(
                            text = s.retry,
                            onClick = {
                                val retryUri = lastFailedChatVideoUri
                                if (retryUri != null) handleChatVideoUri(retryUri)
                            },
                            testTag = "chat_retry_video_button"
                        )
                    }
                }
            }
        }

        val previewImg = pendingImagePreview
        if (previewImg != null && conv != null) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    Text(
                        text = if (isNe) "✨ HD तस्बिर (१४४०px)" else "✨ HD Photo Ready (1440px)",
                        style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                    )
                    MeroPhotoBox(
                        photoData = previewImg,
                        contentDescription = "Preview image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(MeroRadius.small))
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MeroSecondaryButton(
                            text = s.cancel,
                            onClick = { pendingImagePreview = null },
                            modifier = Modifier.weight(1f),
                            testTag = "chat_discard_image_preview"
                        )
                        MeroButton(
                            text = s.send,
                            onClick = {
                                MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                pendingImagePreview = null
                                onSendImage(conv, previewImg)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "chat_confirm_send_image"
                        )
                    }
                }
            }
        }

        val previewVid = pendingVideoPreview
        if (previewVid != null && conv != null) {
            Surface(
                shape = RoundedCornerShape(MeroRadius.card),
                color = colors.surface2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.padding(MeroSpacing.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isNe) {
                                "🎬 HD भिडियो तयार (${previewVid.durationSec}s · ${previewVid.sizeBytes / 1024} KB)"
                            } else {
                                "🎬 HD Video Ready (${previewVid.durationSec}s · ${previewVid.sizeBytes / 1024} KB)"
                            },
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                        )
                    }
                    previewVid.thumbnailBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Video thumbnail preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(RoundedCornerShape(MeroRadius.small))
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MeroSecondaryButton(
                            text = s.cancel,
                            onClick = { pendingVideoPreview = null },
                            modifier = Modifier.weight(1f),
                            testTag = "chat_discard_video_preview"
                        )
                        MeroButton(
                            text = s.send,
                            onClick = {
                                MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                pendingVideoPreview = null
                                onSendVideo(conv, previewVid.dataUri, previewVid.durationSec)
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "chat_confirm_send_video"
                        )
                    }
                }
            }
        }

        // Gender-Aware Quick Replies & HD Media Strip
        val quickReplies = remember(myGender, isNe) {
            ChatLinkHelper.quickRepliesFor(myGender, isNe)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.xs)
        ) {
            MeroChip(
                label = "🎬 ${s.sendVideoLabel}",
                selected = false,
                leadingIcon = Icons.Filled.Videocam,
                onClick = {
                    MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                    runCatching {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }.onFailure {
                        videoFolderPicker.launch(MediaHelper.SUPPORTED_VIDEO_MIMES)
                    }
                },
                testTag = "chat_pick_video_chip"
            )
            quickReplies.forEachIndexed { idx, suggestion ->
                MeroChip(
                    label = suggestion,
                    selected = false,
                    onClick = {
                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                        draft = suggestion
                    },
                    testTag = "chat_quick_reply_$idx"
                )
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
                    contentDescription = s.sendHdPhotoLabel,
                    tint = colors.primary,
                    onClick = {
                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                        runCatching {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            legacyImagePicker.launch("image/*")
                        }
                    },
                    testTag = "chat_pick_image_button"
                )

                MeroIconButton(
                    icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = s.recordVoiceHint,
                    tint = if (isRecording) colors.danger else colors.textMuted,
                    onClick = {
                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
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
                            MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                            draft = ""
                            onSendText(c, msg)
                        }
                    },
                    testTag = "chat_send_button"
                )
            }
        }
    }

    val targetToDelete = deleteConfirmTarget
    if (targetToDelete != null && conv != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmTarget = null },
            containerColor = colors.surface,
            title = { Text(s.deleteMessageLabel, style = MeroType.Heading.copy(color = colors.danger)) },
            text = { Text(s.deleteMessageConfirm, style = MeroType.Body.copy(color = colors.text)) },
            confirmButton = {
                MeroButton(
                    text = s.confirm,
                    onClick = {
                        val target = targetToDelete
                        deleteConfirmTarget = null
                        MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled, strong = true)
                        onDeleteMessage(conv, target)
                    },
                    testTag = "confirm_delete_message"
                )
            },
            dismissButton = {
                MeroSecondaryButton(
                    text = s.cancel,
                    onClick = { deleteConfirmTarget = null },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    val vidMsg = activeVideoToPlay
    if (vidMsg != null && vidMsg.mediaData.isNotBlank()) {
        VideoPlaybackDialog(
            videoDataUri = vidMsg.mediaData,
            durationSec = vidMsg.audioDurationSec,
            closeLabel = s.close,
            onDismiss = { activeVideoToPlay = null }
        )
    }
}

@Composable
private fun VideoPlaybackDialog(
    videoDataUri: String,
    durationSec: Int,
    closeLabel: String,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val videoFile = remember(videoDataUri) {
        runCatching {
            val cleanB64 = videoDataUri.substringAfter("base64,", videoDataUri)
            val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
            val f = File(context.cacheDir, "play_vid_${System.currentTimeMillis()}.mp4")
            f.writeBytes(bytes)
            f
        }.getOrNull()
    }
    DisposableEffect(videoFile) {
        onDispose {
            runCatching { videoFile?.delete() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                text = "HD Video (${durationSec}s)",
                style = MeroType.Heading.copy(color = colors.text)
            )
        },
        text = {
            if (videoFile != null) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(videoFile.absolutePath)
                            val mc = MediaController(ctx)
                            mc.setAnchorView(this)
                            setMediaController(mc)
                            setOnPreparedListener { mp ->
                                mp.isLooping = false
                                start()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(MeroRadius.small))
                )
            } else {
                Text(
                    text = "Unable to decode video.",
                    style = MeroType.Body.copy(color = colors.danger)
                )
            }
        },
        confirmButton = {
            MeroButton(
                text = closeLabel,
                onClick = onDismiss,
                testTag = "chat_video_player_close"
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isPlaying: Boolean,
    deleteLabel: String,
    onToggleVoicePlay: () -> Unit,
    onOpenVideo: () -> Unit = {},
    onRequestDelete: (() -> Unit)?
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val bubbleBg = if (isMe) colors.primary else colors.surface
    val bubbleFg = if (isMe) colors.onPrimary else colors.text
    val linkColor = if (isMe) colors.onPrimary else colors.primary
    val timeText = remember(message.createdAt) {
        val date = message.createdAt?.toDate()
        if (date != null) SimpleDateFormat("h:mm a", Locale.getDefault()).format(date) else ""
    }
    val extractedLinks = remember(message.text) {
        ChatLinkHelper.extractLinks(message.text)
    }

    Row(
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(MeroRadius.card),
            color = bubbleBg,
            border = if (isMe) null else BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .widthIn(max = 288.dp)
                .clip(RoundedCornerShape(MeroRadius.card))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onRequestDelete?.invoke() }
                )
                .testTag("chat_bubble_${message.id}")
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
                                .height(190.dp)
                                .clip(RoundedCornerShape(MeroRadius.small))
                        )
                    }
                    "VIDEO" -> {
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.small),
                            color = bubbleFg.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(MeroRadius.small))
                                .clickable(onClick = onOpenVideo)
                                .testTag("chat_play_video_${message.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                modifier = Modifier.padding(MeroSpacing.md)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play video",
                                    tint = bubbleFg,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "HD Video",
                                        style = MeroType.Body.copy(color = bubbleFg, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "0:${message.audioDurationSec.coerceAtLeast(1).toString().padStart(2, '0')} · Tap to watch",
                                        style = MeroType.Caption.copy(color = bubbleFg.copy(alpha = 0.85f))
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = bubbleFg.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                        if (extractedLinks.isEmpty()) {
                            Text(
                                text = message.text,
                                style = MeroType.Body.copy(color = bubbleFg)
                            )
                        } else {
                            val annotated = remember(message.text, extractedLinks, linkColor) {
                                buildAnnotatedString {
                                    var cursor = 0
                                    extractedLinks.forEach { link ->
                                        if (link.start > cursor) {
                                            append(message.text.substring(cursor, link.start))
                                        }
                                        withLink(
                                            LinkAnnotation.Url(
                                                url = link.normalizedUri,
                                                styles = TextLinkStyles(
                                                    style = SpanStyle(
                                                        color = linkColor,
                                                        fontWeight = FontWeight.Medium,
                                                        textDecoration = TextDecoration.Underline
                                                    )
                                                ),
                                                linkInteractionListener = {
                                                    ChatLinkHelper.openLinkSafely(context, link.normalizedUri)
                                                }
                                            )
                                        ) {
                                            append(link.raw)
                                        }
                                        cursor = link.end
                                    }
                                    if (cursor < message.text.length) {
                                        append(message.text.substring(cursor))
                                    }
                                }
                            }
                            Text(
                                text = annotated,
                                style = MeroType.Body.copy(color = bubbleFg)
                            )
                            extractedLinks.take(2).forEach { link ->
                                Surface(
                                    shape = RoundedCornerShape(MeroRadius.small),
                                    color = bubbleFg.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MeroRadius.small))
                                        .clickable { ChatLinkHelper.openLinkSafely(context, link.normalizedUri) }
                                        .testTag("chat_link_preview_${message.id}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                                        modifier = Modifier.padding(horizontal = MeroSpacing.sm, vertical = MeroSpacing.xs)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Link,
                                            contentDescription = null,
                                            tint = bubbleFg,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = link.displayLabel,
                                            style = MeroType.Caption.copy(color = bubbleFg, fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = "Open link",
                                            tint = bubbleFg,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (timeText.isNotBlank()) {
                        Text(
                            text = timeText,
                            style = MeroType.Caption.copy(color = bubbleFg.copy(alpha = 0.78f))
                        )
                    }
                    if (onRequestDelete != null) {
                        Spacer(Modifier.width(MeroSpacing.xs))
                        IconButton(
                            onClick = onRequestDelete,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("chat_delete_msg_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = deleteLabel,
                                tint = bubbleFg.copy(alpha = 0.86f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.core.audio.MeroSoundAndHapticHelper
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroBannerImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroListItem
import com.example.core.designsystem.MeroLoadingState
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroPresenceBadge
import com.example.core.designsystem.MeroPresenceDot
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.designsystem.ProfileBannerPreset
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.MediaHelper
import com.example.core.util.MediaUploadResult
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    language: AppLanguage,
    publicProfile: PublicProfile?,
    interestsCount: Int,
    matchesCount: Int,
    chatsCount: Int,
    onUpdateBanner: (String) -> Unit,
    onUpdateProfilePhotos: (List<String>) -> Unit,
    onAddCustomInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit,
    onOpenSavedNotes: () -> Unit,
    onOpenTodos: () -> Unit = {},
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelpSafety: () -> Unit,
    onOpenAbout: () -> Unit,
    onShowError: (String) -> Unit,
    currentUid: String = "",
    showOnlineStatus: Boolean = true,
    onToggleOnlineStatus: (Boolean) -> Unit = {},
    hapticEnabled: Boolean = true,
    onShowToast: (String) -> Unit = {},
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val profile = publicProfile
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var showBannerSheet by remember { mutableStateOf(false) }
    var showPhotoManagerSheet by remember { mutableStateOf(false) }
    var customInterestInput by remember { mutableStateOf("") }
    var bannerCameraUri by remember { mutableStateOf<Uri?>(null) }
    var avatarCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Banner upload state (preview before saving + progress + retry)
    var isProcessingBanner by remember { mutableStateOf(false) }
    var pendingBannerPreview by remember { mutableStateOf<String?>(null) }
    var lastFailedBannerUri by remember { mutableStateOf<Uri?>(null) }
    var bannerUploadError by remember { mutableStateOf<String?>(null) }

    // Profile Photo Manager state (preview before saving + progress + retry)
    var isProcessingAvatar by remember { mutableStateOf(false) }
    var pendingAvatarPreview by remember { mutableStateOf<String?>(null) }
    var lastFailedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUploadError by remember { mutableStateOf<String?>(null) }

    fun handleBannerUri(uri: Uri) {
        scope.launch {
            isProcessingBanner = true
            bannerUploadError = null
            lastFailedBannerUri = null
            when (val res = MediaHelper.processImageUriWithResult(context, uri, hd = true)) {
                is MediaUploadResult.Success -> {
                    pendingBannerPreview = res.dataUri
                }
                is MediaUploadResult.Error -> {
                    lastFailedBannerUri = uri
                    bannerUploadError = if (isNe) res.messageNe else res.messageEn
                }
            }
            isProcessingBanner = false
        }
    }

    fun handleAvatarUri(uri: Uri) {
        scope.launch {
            isProcessingAvatar = true
            avatarUploadError = null
            lastFailedAvatarUri = null
            when (val res = MediaHelper.processImageUriWithResult(context, uri, hd = true)) {
                is MediaUploadResult.Success -> {
                    pendingAvatarPreview = res.dataUri
                }
                is MediaUploadResult.Error -> {
                    lastFailedAvatarUri = uri
                    avatarUploadError = if (isNe) res.messageNe else res.messageEn
                }
            }
            isProcessingAvatar = false
        }
    }

    val bannerLegacyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) handleBannerUri(uri)
    }

    val bannerPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handleBannerUri(uri)
    }

    val bannerFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) handleBannerUri(uri)
    }

    val bannerCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = bannerCameraUri
        if (success && uri != null) {
            handleBannerUri(uri)
        }
    }

    fun launchBannerCamera() {
        runCatching {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "banner_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            bannerCameraUri = uri
            bannerCameraLauncher.launch(uri)
        }.onFailure {
            bannerUploadError = if (isNe) {
                "क्यामेरा खोल्न सकिएन। कृपया ग्यालरीबाट छान्नुहोस्।"
            } else {
                "Could not open camera on this device. Please select a photo from gallery."
            }
        }
    }

    val bannerCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchBannerCamera()
        else bannerUploadError = if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is required."
    }

    // Profile Photo Manager launchers
    val avatarLegacyPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) handleAvatarUri(uri)
    }

    val avatarPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) handleAvatarUri(uri)
    }

    val avatarFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) handleAvatarUri(uri)
    }

    val avatarCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = avatarCameraUri
        if (success && uri != null) {
            handleAvatarUri(uri)
        }
    }

    fun launchAvatarCamera() {
        runCatching {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "avatar_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            avatarCameraUri = uri
            avatarCameraLauncher.launch(uri)
        }.onFailure {
            avatarUploadError = if (isNe) {
                "क्यामेरा खोल्न सकिएन। कृपया ग्यालरी वा फाइलबाट फोटो छान्नुहोस्।"
            } else {
                "Could not open camera on this device. Please select a photo from Gallery or Files."
            }
        }
    }

    val avatarCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchAvatarCamera()
        else avatarUploadError = if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is required."
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Screen 5 Customizable Cover Banner + Avatar Header
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                MeroBannerImage(
                    bannerData = profile?.bannerData,
                    contentDescription = profile?.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x441A0C13), colors.bg)
                            )
                        )
                )

                // Top-left Customize Banner pill & Top-right Settings button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MeroSpacing.lg)
                ) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = Color.Black.copy(alpha = 0.50f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(MeroRadius.pill))
                            .clickable {
                                bannerUploadError = null
                                pendingBannerPreview = null
                                showBannerSheet = true
                            }
                            .testTag("profile_customize_banner_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = s.customizeBanner,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = s.customizeBanner,
                                style = MeroType.Caption.copy(color = Color.White, fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    MeroIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = s.settings,
                        tint = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        onClick = onOpenSettings,
                        testTag = "profile_top_settings_button"
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 104.dp, start = MeroSpacing.lg, end = MeroSpacing.lg)
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                avatarUploadError = null
                                pendingAvatarPreview = null
                                showPhotoManagerSheet = true
                            }
                    ) {
                        MeroAvatarImage(
                            photoData = profile?.photoDataList?.firstOrNull(),
                            displayName = profile?.displayName ?: "?",
                            size = 112.dp
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .border(2.dp, colors.bg, CircleShape)
                                .clickable {
                                    avatarUploadError = null
                                    pendingAvatarPreview = null
                                    showPhotoManagerSheet = true
                                }
                                .testTag("profile_avatar_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = s.stepPhoto,
                                tint = colors.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                    ) {
                        Text(
                            text = profile?.displayName ?: "...",
                            style = MeroType.Title.copy(color = colors.text),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile?.ageVerified == true) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = s.verifiedBadge,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = profile?.publicLocationLabel ?: "Nepal",
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                    val effectiveOnline = showOnlineStatus && (profile?.onlineNow != false)
                    MeroPresenceBadge(
                        isOnline = effectiveOnline,
                        label = if (effectiveOnline) {
                            if (isNe) "अनलाइन · सक्रिय" else "Online Now"
                        } else {
                            if (isNe) "अफलाइन (गोप्य)" else "Offline (Hidden)"
                        },
                        modifier = Modifier.testTag("profile_own_presence_badge")
                    )
                    if (currentUid.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.pill),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(MeroRadius.pill))
                                .clickable {
                                    MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                                    clipboard.setText(AnnotatedString(currentUid))
                                    onShowToast(
                                        if (isNe) "तपाईंको प्रयोगकर्ता ID कपी गरियो!" else "Copied your User ID!"
                                    )
                                }
                                .testTag("profile_copy_uid_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                                modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                            ) {
                                Text(
                                    text = "${s.myUserIdLabel}: ${currentUid.take(12)}…",
                                    style = MeroType.Caption.copy(color = colors.textMuted, fontWeight = FontWeight.Medium)
                                )
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = s.myUserIdLabel,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real Stats Row (Interests / Matches / Chats)
        item {
            MeroCard(
                modifier = Modifier
                    .padding(horizontal = MeroSpacing.lg)
                    .widthIn(max = 560.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatCell(value = interestsCount, label = s.interestsStat, modifier = Modifier.weight(1f))
                    StatCell(value = matchesCount, label = s.matchesStat, modifier = Modifier.weight(1f))
                    StatCell(value = chatsCount, label = s.chatsStat, modifier = Modifier.weight(1f))
                }
            }
        }

        // My Cultural Badge & Live Custom Interests
        if (profile != null) {
            item {
                MeroCard(
                    modifier = Modifier
                        .padding(horizontal = MeroSpacing.lg)
                        .widthIn(max = 560.dp)
                ) {
                    if (profile.culturalBadge.isNotBlank()) {
                        MeroListItem(
                            title = s.myCulturalBadge,
                            subtitle = profile.culturalBadge,
                            leadingIcon = Icons.Filled.Favorite,
                            onClick = onEditProfile,
                            testTag = "profile_cultural_badge_row"
                        )
                    }
                    Text(
                        text = s.myInterests,
                        style = MeroType.Heading.copy(color = colors.text)
                    )
                    if (profile.interests.isEmpty()) {
                        Text(
                            text = if (isNe) "तल आफ्नो रुचि थप्नुहोस्।" else "Add a custom interest below.",
                            style = MeroType.Body.copy(color = colors.textMuted)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                        ) {
                            profile.interests.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(MeroRadius.pill),
                                    color = colors.surface2,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 40.dp)
                                        .clip(RoundedCornerShape(MeroRadius.pill))
                                        .clickable { onRemoveInterest(tag) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                                        modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = s.cancel,
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MeroInput(
                            label = s.customInterestLabel,
                            value = customInterestInput,
                            onValueChange = { if (it.length <= 32) customInterestInput = it },
                            placeholder = if (isNe) "जस्तै: सरोद, कविता, यात्रा" else "e.g. Sarangi, Poetry, Chiya",
                            modifier = Modifier.weight(1f),
                            testTag = "profile_custom_interest_input"
                        )
                        MeroSecondaryButton(
                            text = s.addCustomInterest,
                            leadingIcon = Icons.Filled.Add,
                            enabled = customInterestInput.trim().isNotBlank() && profile.interests.size < 16,
                            onClick = {
                                onAddCustomInterest(customInterestInput.trim())
                                customInterestInput = ""
                            },
                            testTag = "profile_add_custom_interest_button"
                        )
                    }
                }
            }
        }

        // Action Menu Card
        item {
            MeroCard(
                modifier = Modifier
                    .padding(horizontal = MeroSpacing.lg)
                    .widthIn(max = 560.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .padding(horizontal = MeroSpacing.xs)
                ) {
                    Text(
                        text = s.showOnlineStatusToggle,
                        style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = MeroSpacing.sm)
                    )
                    Switch(
                        checked = showOnlineStatus,
                        onCheckedChange = { enabled ->
                            MeroSoundAndHapticHelper.triggerHaptic(context, enabled = hapticEnabled)
                            onToggleOnlineStatus(enabled)
                        },
                        modifier = Modifier.testTag("profile_online_status_switch")
                    )
                }
                MeroListItem(
                    title = if (isNe) "प्रोफाइल फोटो थप्नुहोस् / बदल्नुहोस् / हटाउनुहोस्" else "Profile Photo (Add, Change, or Remove)",
                    subtitle = if (profile?.photoDataList.isNullOrEmpty()) {
                        if (isNe) "डिफल्ट नेपाली अवतार सक्रिय छ (ऐच्छिक)" else "Using default Nepali avatar (Optional)"
                    } else {
                        if (isNe) "${profile?.photoDataList?.size ?: 0}/4 फोटो राखिएको छ" else "${profile?.photoDataList?.size ?: 0}/4 photos saved"
                    },
                    leadingIcon = Icons.Filled.AddAPhoto,
                    onClick = {
                        avatarUploadError = null
                        pendingAvatarPreview = null
                        showPhotoManagerSheet = true
                    },
                    testTag = "profile_manage_photo_row"
                )
                MeroListItem(
                    title = s.mySavedNotesShortcut,
                    leadingIcon = Icons.Filled.BookmarkAdd,
                    onClick = onOpenSavedNotes,
                    testTag = "profile_saved_notes_row"
                )
                MeroListItem(
                    title = s.todosTab,
                    subtitle = if (isNe) "भेटघाट तयारी र व्यक्तिगत कार्यहरू" else "Meetup prep & personal checklist",
                    leadingIcon = Icons.Filled.CheckCircle,
                    onClick = onOpenTodos,
                    testTag = "profile_todos_row"
                )
                MeroListItem(
                    title = s.editProfile,
                    leadingIcon = Icons.Filled.Edit,
                    onClick = onEditProfile,
                    testTag = "profile_edit_row"
                )
                MeroListItem(
                    title = s.settings,
                    leadingIcon = Icons.Filled.Settings,
                    onClick = onOpenSettings,
                    testTag = "profile_settings_row"
                )
                MeroListItem(
                    title = s.helpSafety,
                    leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
                    onClick = onOpenHelpSafety,
                    testTag = "profile_help_row"
                )
                MeroListItem(
                    title = s.aboutApp,
                    leadingIcon = Icons.Filled.Info,
                    onClick = onOpenAbout,
                    testTag = "profile_about_row"
                )
            }
        }
    }

    // Profile Photo Manager Sheet (Add / Preview / Update / Remove anytime)
    if (showPhotoManagerSheet) {
        val currentPhotos = profile?.photoDataList.orEmpty()
        ModalBottomSheet(
            onDismissRequest = { showPhotoManagerSheet = false },
            containerColor = colors.surface
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
            ) {
                Text(
                    text = if (isNe) "प्रोफाइल फोटो व्यवस्थापन (ऐच्छिक)" else "Profile Photo Manager (Optional)",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                Text(
                    text = s.photoRequiredNote,
                    style = MeroType.Caption.copy(color = colors.textMuted)
                )

                if (isProcessingAvatar) {
                    MeroLoadingState(
                        message = if (isNe) "फोटो अनुकूलन र पूर्वावलोकन तयार हुँदैछ..." else "Optimizing image & preparing preview..."
                    )
                }

                if (avatarUploadError != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.small),
                        color = colors.danger.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.padding(MeroSpacing.md)
                        ) {
                            Text(
                                text = avatarUploadError.orEmpty(),
                                style = MeroType.Caption.copy(color = colors.danger, fontWeight = FontWeight.Medium)
                            )
                            if (lastFailedAvatarUri != null) {
                                MeroSecondaryButton(
                                    text = s.retry,
                                    onClick = {
                                        val retryUri = lastFailedAvatarUri
                                        if (retryUri != null) handleAvatarUri(retryUri)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "avatar_retry_upload_button"
                                )
                            }
                        }
                    }
                }

                val preview = pendingAvatarPreview
                if (preview != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.card),
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(MeroSpacing.md)
                        ) {
                            Text(
                                text = if (isNe) "नयाँ प्रोफाइल फोटो पूर्वावलोकन" else "New Profile Photo Preview",
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                            MeroAvatarImage(
                                photoData = preview,
                                displayName = profile?.displayName ?: "?",
                                size = 120.dp
                            )
                            MeroButton(
                                text = if (isNe) "मुख्य प्रोफाइल फोटोको रूपमा सुरक्षित गर्नुहोस्" else "Save as Primary Profile Photo",
                                leadingIcon = Icons.Filled.Check,
                                onClick = {
                                    val updated = (listOf(preview) + currentPhotos.filterNot { it == preview }).take(4)
                                    onUpdateProfilePhotos(updated)
                                    pendingAvatarPreview = null
                                    showPhotoManagerSheet = false
                                },
                                testTag = "avatar_confirm_save_button"
                            )
                            MeroSecondaryButton(
                                text = s.cancel,
                                onClick = { pendingAvatarPreview = null },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "avatar_discard_preview_button"
                            )
                        }
                    }
                }

                if (currentPhotos.isNotEmpty()) {
                    Text(
                        text = if (isNe) "हालका प्रोफाइल फोटोहरू (${currentPhotos.size}/4)" else "Current Profile Photos (${currentPhotos.size}/4)",
                        style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        currentPhotos.forEachIndexed { idx, photo ->
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(RoundedCornerShape(MeroRadius.card))
                            ) {
                                MeroPhotoBox(
                                    photoData = photo,
                                    contentDescription = "Profile photo ${idx + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(MeroSpacing.xs)
                                        .clip(RoundedCornerShape(MeroRadius.pill))
                                        .background(colors.bg.copy(alpha = 0.85f))
                                ) {
                                    MeroIconButton(
                                        icon = Icons.Filled.Delete,
                                        contentDescription = "Remove photo",
                                        tint = colors.danger,
                                        onClick = {
                                            val remaining = currentPhotos.filterIndexed { i, _ -> i != idx }
                                            onUpdateProfilePhotos(remaining)
                                        },
                                        testTag = "profile_remove_photo_$idx"
                                    )
                                }
                            }
                        }
                    }
                }

                MeroSecondaryButton(
                    text = s.selectPhoto,
                    leadingIcon = Icons.Filled.AddAPhoto,
                    enabled = !isProcessingAvatar,
                    onClick = {
                        runCatching {
                            avatarPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            avatarLegacyPicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_photo_pick_gallery"
                )
                MeroSecondaryButton(
                    text = s.browseFoldersPhoto,
                    leadingIcon = Icons.Filled.FolderOpen,
                    enabled = !isProcessingAvatar,
                    onClick = {
                        runCatching {
                            avatarFolderPicker.launch(MediaHelper.SUPPORTED_IMAGE_MIMES)
                        }.onFailure {
                            avatarLegacyPicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_photo_browse_files"
                )
                MeroSecondaryButton(
                    text = s.takePhoto,
                    leadingIcon = Icons.Filled.CameraAlt,
                    enabled = !isProcessingAvatar,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchAvatarCamera()
                        else avatarCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_photo_take_camera"
                )

                if (currentPhotos.isNotEmpty()) {
                    MeroSecondaryButton(
                        text = if (isNe) "प्रोफाइल फोटो हटाउनुहोस् (डिफल्ट अवतार राख्नुहोस्)" else "Remove Profile Picture (Use Default Avatar)",
                        leadingIcon = Icons.Filled.Delete,
                        isDanger = true,
                        onClick = {
                            onUpdateProfilePhotos(emptyList())
                            pendingAvatarPreview = null
                            showPhotoManagerSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "profile_remove_all_photos_button"
                    )
                }

                Spacer(Modifier.height(MeroSpacing.lg))
            }
        }
    }

    if (showBannerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBannerSheet = false },
            containerColor = colors.surface
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
            ) {
                Text(
                    text = s.customizeBanner,
                    style = MeroType.Heading.copy(color = colors.text)
                )
                MeroBannerImage(
                    bannerData = pendingBannerPreview ?: profile?.bannerData,
                    contentDescription = s.customizeBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(MeroRadius.card))
                )

                if (isProcessingBanner) {
                    MeroLoadingState(
                        message = if (isNe) "HD ब्यानर तयार हुँदैछ..." else "Preparing HD banner preview..."
                    )
                }

                if (bannerUploadError != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.small),
                        color = colors.danger.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.padding(MeroSpacing.md)
                        ) {
                            Text(
                                text = bannerUploadError.orEmpty(),
                                style = MeroType.Caption.copy(color = colors.danger, fontWeight = FontWeight.Medium)
                            )
                            if (lastFailedBannerUri != null) {
                                MeroSecondaryButton(
                                    text = s.retry,
                                    onClick = {
                                        val retryUri = lastFailedBannerUri
                                        if (retryUri != null) handleBannerUri(retryUri)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    testTag = "banner_retry_upload_button"
                                )
                            }
                        }
                    }
                }

                val bannerPreview = pendingBannerPreview
                if (bannerPreview != null) {
                    MeroButton(
                        text = if (isNe) "यो ब्यानर सुरक्षित गर्नुहोस्" else "Save Custom Banner",
                        leadingIcon = Icons.Filled.Check,
                        onClick = {
                            onUpdateBanner(bannerPreview)
                            pendingBannerPreview = null
                            showBannerSheet = false
                        },
                        testTag = "banner_confirm_save_button"
                    )
                    MeroSecondaryButton(
                        text = s.cancel,
                        onClick = { pendingBannerPreview = null },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "banner_discard_preview_button"
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                ) {
                    ProfileBannerPreset.entries.forEach { preset ->
                        MeroChip(
                            label = if (isNe) preset.labelNe else preset.labelEn,
                            selected = profile?.bannerData == preset.id && pendingBannerPreview == null,
                            onClick = {
                                pendingBannerPreview = null
                                onUpdateBanner(preset.id)
                                showBannerSheet = false
                            },
                            testTag = "banner_preset_${preset.id}"
                        )
                    }
                }
                MeroSecondaryButton(
                    text = s.selectPhoto,
                    leadingIcon = Icons.Filled.AddAPhoto,
                    enabled = !isProcessingBanner,
                    onClick = {
                        runCatching {
                            bannerPhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            bannerLegacyPicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "banner_pick_photo"
                )
                MeroSecondaryButton(
                    text = s.browseFoldersPhoto,
                    leadingIcon = Icons.Filled.FolderOpen,
                    enabled = !isProcessingBanner,
                    onClick = {
                        runCatching {
                            bannerFolderPicker.launch(MediaHelper.SUPPORTED_IMAGE_MIMES)
                        }.onFailure {
                            bannerLegacyPicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "banner_browse_folders"
                )
                MeroSecondaryButton(
                    text = s.takePhoto,
                    leadingIcon = Icons.Filled.CameraAlt,
                    enabled = !isProcessingBanner,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchBannerCamera()
                        else bannerCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "banner_take_photo"
                )
                Spacer(Modifier.height(MeroSpacing.lg))
            }
        }
    }
}

@Composable
private fun StatCell(value: Int, label: String, modifier: Modifier = Modifier) {
    val colors = LocalMeroColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
        modifier = modifier
    ) {
        Text(
            text = value.toString(),
            style = MeroType.Title.copy(color = colors.primary)
        )
        Text(
            text = label,
            style = MeroType.Caption.copy(color = colors.textMuted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

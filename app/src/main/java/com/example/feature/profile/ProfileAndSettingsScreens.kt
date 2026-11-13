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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroAvatarImage
import com.example.core.designsystem.MeroBannerImage
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroListItem
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.designsystem.ProfileBannerPreset
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.MediaHelper
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
    onAddCustomInterest: (String) -> Unit,
    onRemoveInterest: (String) -> Unit,
    onOpenSavedNotes: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelpSafety: () -> Unit,
    onOpenAbout: () -> Unit,
    onShowError: (String) -> Unit,
    bottomPadding: Dp
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val profile = publicProfile
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBannerSheet by remember { mutableStateOf(false) }
    var customInterestInput by remember { mutableStateOf("") }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val bannerPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) {
                    onUpdateBanner(compressed)
                    showBannerSheet = false
                }
            }
        }
    }

    val bannerFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) {
                    onUpdateBanner(compressed)
                    showBannerSheet = false
                }
            }
        }
    }

    val bannerCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null) {
                    onUpdateBanner(compressed)
                    showBannerSheet = false
                }
            }
        }
    }

    fun launchBannerCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "banner_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        bannerCameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchBannerCamera()
        else onShowError(if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is required.")
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
                            .clickable { showBannerSheet = true }
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
                    Box(contentAlignment = Alignment.BottomEnd) {
                        MeroAvatarImage(
                            photoData = profile?.photoDataList?.firstOrNull(),
                            displayName = profile?.displayName ?: "?",
                            size = 112.dp
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .border(2.dp, colors.bg, CircleShape)
                                .clickable(onClick = onEditProfile)
                                .testTag("profile_avatar_edit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = s.editProfile,
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
                MeroListItem(
                    title = s.mySavedNotesShortcut,
                    leadingIcon = Icons.Filled.BookmarkAdd,
                    onClick = onOpenSavedNotes,
                    testTag = "profile_saved_notes_row"
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
                    bannerData = profile?.bannerData,
                    contentDescription = s.customizeBanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(MeroRadius.card))
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                ) {
                    ProfileBannerPreset.entries.forEach { preset ->
                        MeroChip(
                            label = if (isNe) preset.labelNe else preset.labelEn,
                            selected = profile?.bannerData == preset.id,
                            onClick = {
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
                    onClick = {
                        bannerPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "banner_pick_photo"
                )
                MeroSecondaryButton(
                    text = s.browseFoldersPhoto,
                    leadingIcon = Icons.Filled.FolderOpen,
                    onClick = { bannerFolderPicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "banner_browse_folders"
                )
                MeroSecondaryButton(
                    text = s.takePhoto,
                    leadingIcon = Icons.Filled.CameraAlt,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchBannerCamera()
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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

package com.example.feature.culture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.PublicProfile
import com.example.core.domain.TogetherMemory
import com.example.core.localization.AppLanguage
import com.example.core.util.CompatibilityBreakdown
import com.example.core.util.LocationServiceHelper
import com.example.core.util.MediaHelper
import com.example.core.util.MediaUploadResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LocationPrivacySheet(
    myProfile: PublicProfile,
    language: AppLanguage,
    onSaveLocationAndPrivacy: (
        locationSharingEnabled: Boolean,
        coarseLat: Double,
        coarseLng: Double,
        province: String,
        district: String,
        publicLocationLabel: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNe = language == AppLanguage.NEPALI

    var sharingEnabled by remember(myProfile) { mutableStateOf(myProfile.locationSharingEnabled) }
    var selectedCity by remember(myProfile) {
        mutableStateOf(LocationServiceHelper.cityForDistrictOrName(myProfile.district))
    }
    var coarseLat by remember(myProfile) {
        mutableStateOf(if (myProfile.approxLat != 0.0) myProfile.approxLat else selectedCity.lat)
    }
    var coarseLng by remember(myProfile) {
        mutableStateOf(if (myProfile.approxLng != 0.0) myProfile.approxLng else selectedCity.lng)
    }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val locPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            scope.launch {
                val detected = LocationServiceHelper.fetchCoarseDeviceLocation(context)
                if (detected != null) {
                    sharingEnabled = true
                    coarseLat = detected.coarseLat
                    coarseLng = detected.coarseLng
                    selectedCity = detected.matchedCity
                    statusBanner = if (isNe) {
                        "📍 अनुमानित शहर पहिचान गरियो: ${detected.matchedCity.cityNe} (सटीक ठेगाना गोप्य राखिएको छ)"
                    } else {
                        "📍 Coarse location updated: ${detected.matchedCity.cityEn} (exact coordinates stay private)"
                    }
                }
            }
        } else {
            statusBanner = if (isNe) {
                "अनुमति अस्वीकार गरियो। तपाईंले तल म्यानुअल रूपमा आफ्नो शहर रोज्न सक्नुहुन्छ।"
            } else {
                "Location permission declined. You can manually select your city below."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
                .testTag("location_privacy_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNe) "स्थान र गोपनीयता सेटिङ" else "Real-Time Location & Privacy",
                        style = MeroType.Title.copy(color = colors.text)
                    )
                    Text(
                        text = if (isNe) {
                            "तपाईंको सटीक घर वा सडक ठेगाना कहिल्यै अरूलाई देखाइँदैन (अनुमानित कि.मि. मात्र)।"
                        } else {
                            "Your exact street coordinates are never exposed — only city & approximate km."
                        },
                        style = MeroType.Caption.copy(color = colors.textMuted)
                    )
                }
            }

            // Enable/Disable Approximate Distance Sharing Toggle
            MeroCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNe) "अनुमानित दूरी (~कि.मि.) देखाउनुहोस्" else "Share Approximate Distance (~km)",
                            style = MeroType.Heading.copy(color = colors.text)
                        )
                        Text(
                            text = if (isNe) {
                                "बन्द गर्दा तपाईंको शहर मात्र देखिनेछ र सटीक दूरी लुकाइनेछ।"
                            } else {
                                "Turn off anytime to hide your distance and show only your chosen city."
                            },
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                    Switch(
                        checked = sharingEnabled,
                        onCheckedChange = { enabled ->
                            sharingEnabled = enabled
                            if (enabled && !LocationServiceHelper.hasLocationPermission(context)) {
                                locPermLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary
                        ),
                        modifier = Modifier.testTag("location_sharing_switch")
                    )
                }

                MeroSecondaryButton(
                    text = if (isNe) "GPS बाट मेरो शहर पत्ता लगाउनुहोस्" else "Detect My City via GPS",
                    leadingIcon = Icons.Filled.MyLocation,
                    onClick = {
                        if (LocationServiceHelper.hasLocationPermission(context)) {
                            scope.launch {
                                val detected = LocationServiceHelper.fetchCoarseDeviceLocation(context)
                                if (detected != null) {
                                    sharingEnabled = true
                                    coarseLat = detected.coarseLat
                                    coarseLng = detected.coarseLng
                                    selectedCity = detected.matchedCity
                                    statusBanner = if (isNe) {
                                        "📍 ${detected.matchedCity.cityNe} अद्यावधिक गरियो।"
                                    } else {
                                        "📍 Updated to ${detected.matchedCity.cityEn}."
                                    }
                                }
                            }
                        } else {
                            locPermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "detect_gps_city_button"
                )

                if (!statusBanner.isNullOrBlank()) {
                    Text(
                        text = statusBanner.orEmpty(),
                        style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Manual City Selection (Fallback or Override)
            MeroCard {
                Text(
                    text = if (isNe) "म्यानुअल शहर छनोट (GPS बिना पनि काम गर्छ)" else "Manual City Selection (No GPS Required)",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    LocationServiceHelper.NEPALI_CITIES.forEach { city ->
                        MeroChip(
                            label = if (isNe) city.cityNe else city.cityEn,
                            selected = selectedCity.district == city.district && selectedCity.cityEn == city.cityEn,
                            onClick = {
                                selectedCity = city
                                coarseLat = city.lat
                                coarseLng = city.lng
                            }
                        )
                    }
                }
            }

            MeroButton(
                text = if (isNe) "स्थान र गोपनीयता सुरक्षित गर्नुहोस्" else "Save Location & Privacy",
                leadingIcon = Icons.Filled.Shield,
                onClick = {
                    onSaveLocationAndPrivacy(
                        sharingEnabled,
                        coarseLat,
                        coarseLng,
                        selectedCity.province,
                        selectedCity.district,
                        selectedCity.publicLabelEn
                    )
                    onDismiss()
                },
                testTag = "save_location_privacy_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VibeAndActivitySheet(
    myProfile: PublicProfile,
    language: AppLanguage,
    onSave: (vibeId: String, activities: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val isNe = language == AppLanguage.NEPALI

    var selectedVibe by remember(myProfile) {
        mutableStateOf(myProfile.currentVibeId.ifBlank { "chiya_guff" })
    }
    val selectedActivities = remember(myProfile) {
        mutableStateListOf<String>().apply {
            addAll(myProfile.favoriteActivities.ifEmpty { listOf("chiya_walk", "momo_tasting") })
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
                .testTag("vibe_and_activity_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mood,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (isNe) "नेपाली भाइब चेक र गतिविधि मेल" else "Nepali Vibe Check & Activity Match",
                    style = MeroType.Title.copy(color = colors.text)
                )
            }

            Text(
                text = if (isNe) "आज तपाईंको मुड कस्तो छ? (प्रोफाइल र होम फिडमा देखिनेछ)"
                else "Share your current Nepali-inspired mood and favorite real-world hangout activities.",
                style = MeroType.Caption.copy(color = colors.textMuted)
            )

            LocationServiceHelper.NEPALI_VIBE_CARDS.forEach { card ->
                val isSelected = selectedVibe == card.id
                Surface(
                    shape = RoundedCornerShape(MeroRadius.card),
                    color = if (isSelected) colors.primary.copy(alpha = 0.14f) else colors.surface2,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.primary else colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MeroRadius.card))
                        .clickable { selectedVibe = card.id }
                        .testTag("vibe_card_${card.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        Text(text = card.emoji, style = MeroType.Title)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isNe) card.titleNe else card.titleEn,
                                style = MeroType.Heading.copy(color = colors.text)
                            )
                            Text(
                                text = if (isNe) card.subtitleNe else card.subtitleEn,
                                style = MeroType.Caption.copy(color = colors.textMuted)
                            )
                        }
                    }
                }
            }

            MeroCard {
                Text(
                    text = if (isNe) "नजिकका गतिविधि मेल (Nearby Activity Match)" else "Nearby Activity Match Preferences",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    LocationServiceHelper.NEPALI_ACTIVITY_OPTIONS.forEach { act ->
                        val isPicked = act.id in selectedActivities
                        MeroChip(
                            label = "${act.emoji} ${if (isNe) act.labelNe else act.labelEn}",
                            selected = isPicked,
                            onClick = {
                                if (isPicked) {
                                    selectedActivities.remove(act.id)
                                } else if (selectedActivities.size < 6) {
                                    selectedActivities.add(act.id)
                                }
                            }
                        )
                    }
                }
            }

            MeroButton(
                text = if (isNe) "भाइब र गतिविधि अपडेट गर्नुहोस्" else "Save Vibe & Activities",
                leadingIcon = Icons.Filled.AutoAwesome,
                onClick = {
                    onSave(selectedVibe, selectedActivities.toList())
                    onDismiss()
                },
                testTag = "save_vibe_activity_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompatibilityBreakdownSheet(
    targetName: String,
    breakdown: CompatibilityBreakdown,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val isNe = language == AppLanguage.NEPALI

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
                .testTag("compatibility_breakdown_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNe) "मायालु अनुकूलता (Mayalu Compatibility)" else "Mayalu Compatibility",
                        style = MeroType.Title.copy(color = colors.text)
                    )
                    Text(
                        text = "$targetName · ${breakdown.totalPercent}%",
                        style = MeroType.Heading.copy(color = colors.primary)
                    )
                }
            }

            Text(
                text = if (isNe) breakdown.summaryNe else breakdown.summaryEn,
                style = MeroType.Body.copy(color = colors.text)
            )

            MeroCard {
                CompatibilityBarRow(
                    label = if (isNe) "साझा रुचि र सोख" else "Shared Interests & Hobbies",
                    score = breakdown.sharedInterestsScore
                )
                CompatibilityBarRow(
                    label = if (isNe) "सांस्कृतिक विचार र परम्परा" else "Cultural Prompts & Values",
                    score = breakdown.culturalPromptsScore
                )
                CompatibilityBarRow(
                    label = if (isNe) "शहर र भौगोलिक निकटता" else "City & Distance Proximity",
                    score = breakdown.locationProximityScore
                )
                CompatibilityBarRow(
                    label = if (isNe) "भेटघाट गतिविधि र भाइब" else "Hangout Activities & Vibe",
                    score = breakdown.sharedActivitiesScore
                )
            }

            MeroButton(
                text = if (isNe) "बन्द गर्नुहोस्" else "Close",
                onClick = onDismiss,
                testTag = "compatibility_close_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@Composable
private fun CompatibilityBarRow(label: String, score: Int) {
    val colors = LocalMeroColors.current
    Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = label, style = MeroType.Caption.copy(color = colors.text))
            Text(text = "$score%", style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(MeroRadius.pill))
                .background(colors.surface2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score / 100f).coerceIn(0.1f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(MeroRadius.pill))
                    .background(colors.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TogetherMemoriesSheet(
    partnerName: String,
    memories: List<TogetherMemory>,
    language: AppLanguage,
    isBusy: Boolean,
    onAddMemory: (title: String, noteText: String, imageData: String, milestoneTag: String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMeroColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNe = language == AppLanguage.NEPALI

    var title by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var milestoneTag by remember { mutableStateOf(if (isNe) "विशेष सम्झना" else "Special Moment") }
    var photoData by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val res = MediaHelper.processImageUriWithResult(context, uri, hd = false)
                if (res is MediaUploadResult.Success) {
                    photoData = res.dataUri
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
                .testTag("together_memories_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Collections,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNe) "साझा सम्झनाहरू (Together Memories)" else "Together Memories",
                        style = MeroType.Title.copy(color = colors.text)
                    )
                    Text(
                        text = if (isNe) "$partnerName सँगका विशेष पल, तस्बिर र माइलस्टोनहरू"
                        else "Shared milestones, notes, and photos with $partnerName",
                        style = MeroType.Caption.copy(color = colors.textMuted)
                    )
                }
            }

            // Create new memory card
            MeroCard {
                Text(
                    text = if (isNe) "नयाँ सम्झना थप्नुहोस्" else "Save a New Memory Together",
                    style = MeroType.Heading.copy(color = colors.text)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    val milestones = if (isNe) {
                        listOf("☕ पहिलो चिया भेट", "🏛️ सम्पदा पदयात्रा", "💬 मीठो संवाद", "🪔 पर्वको शुभकामना")
                    } else {
                        listOf("☕ First Chiya Date", "🏛️ Heritage Walk", "💬 Deep Conversation", "🪔 Festival Blessing")
                    }
                    milestones.forEach { tag ->
                        MeroChip(
                            label = tag,
                            selected = milestoneTag == tag,
                            onClick = { milestoneTag = tag }
                        )
                    }
                }
                MeroInput(
                    label = if (isNe) "शीर्षक" else "Memory Title",
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    placeholder = if (isNe) "जस्तै: पाटनमा साँझको मट्का चिया" else "e.g., Golden hour chiya at Patan",
                    testTag = "memory_title_input"
                )
                MeroInput(
                    label = if (isNe) "सम्झना नोट" else "Memory Reflection",
                    value = noteText,
                    onValueChange = { if (it.length <= 500) noteText = it },
                    placeholder = if (isNe) "यो पल किन विशेष रह्यो..." else "What made this moment special...",
                    singleLine = false,
                    maxLines = 3,
                    testTag = "memory_note_input"
                )
                if (photoData.isNotBlank()) {
                    MeroPhotoBox(
                        photoData = photoData,
                        contentDescription = "Memory photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(MeroRadius.small))
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MeroSecondaryButton(
                        text = if (isNe) "तस्बिर छान्नुहोस्" else "Add Photo",
                        leadingIcon = Icons.Filled.AddAPhoto,
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "memory_pick_photo_button"
                    )
                    MeroButton(
                        text = if (isNe) "सुरक्षित गर्नुहोस्" else "Save Memory",
                        enabled = title.trim().isNotEmpty() && !isBusy,
                        isLoading = isBusy,
                        onClick = {
                            onAddMemory(title.trim(), noteText.trim(), photoData, milestoneTag)
                            title = ""
                            noteText = ""
                            photoData = ""
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "memory_save_button"
                    )
                }
            }

            // Existing shared memories
            if (memories.isEmpty()) {
                MeroCard {
                    Text(
                        text = if (isNe) "अहिलेसम्म कुनै साझा सम्झना राखिएको छैन। पहिलो सम्झना माथि थप्नुहोस्!"
                        else "No shared memories saved yet. Add your first milestone above!",
                        style = MeroType.Body.copy(color = colors.textMuted)
                    )
                }
            } else {
                memories.forEach { mem ->
                    MeroCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(MeroRadius.pill),
                                color = colors.surface2
                            ) {
                                Text(
                                    text = mem.milestoneTag.ifBlank { "Memory" },
                                    style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                                )
                            }
                            MeroIconButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Delete memory",
                                tint = colors.textMuted,
                                onClick = { onDeleteMemory(mem.id) }
                            )
                        }
                        Text(
                            text = mem.title,
                            style = MeroType.Heading.copy(color = colors.text)
                        )
                        if (mem.noteText.isNotBlank()) {
                            Text(
                                text = mem.noteText,
                                style = MeroType.Body.copy(color = colors.text)
                            )
                        }
                        if (mem.imageData.isNotBlank()) {
                            MeroPhotoBox(
                                photoData = mem.imageData,
                                contentDescription = mem.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(MeroRadius.small))
                            )
                        }
                        Text(
                            text = if (isNe) "राख्ने: ${mem.authorName}" else "Added by ${mem.authorName}",
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                }
            }
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

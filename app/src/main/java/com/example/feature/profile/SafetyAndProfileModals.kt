package com.example.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroBannerImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroPresenceBadge
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

import com.example.core.util.LocationServiceHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailSheet(
    profile: PublicProfile,
    language: AppLanguage,
    canSendInterest: Boolean,
    onDismiss: () -> Unit,
    onInterested: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    myProfile: PublicProfile? = null,
    onSendCompliment: ((String, String) -> Unit)? = null,
    onOpenCompatibility: (() -> Unit)? = null
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    var showComplimentPicker by remember { mutableStateOf(false) }

    val distanceLabel = remember(myProfile, profile, isNe) {
        if (myProfile != null && myProfile.uid != profile.uid) {
            val km = LocationServiceHelper.estimateDistanceKm(myProfile, profile)
            LocationServiceHelper.formatDistanceBadge(km, isNe).takeIf { it.isNotBlank() }
        } else null
    }

    val compatibilityPct = remember(myProfile, profile) {
        if (myProfile != null && myProfile.uid != profile.uid) {
            LocationServiceHelper.computeCompatibility(myProfile, profile).totalPercent
        } else null
    }

    val vibeCard = remember(profile.currentVibeId) {
        profile.currentVibeId.takeIf { it.isNotBlank() }?.let { LocationServiceHelper.vibeById(it) }
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
        ) {
            if (profile.bannerData.isNotBlank()) {
                MeroBannerImage(
                    bannerData = profile.bannerData,
                    contentDescription = profile.displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(MeroRadius.card))
                )
            }

            MeroPhotoBox(
                photoData = profile.photoDataList.firstOrNull(),
                contentDescription = profile.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.95f)
                    .clip(RoundedCornerShape(MeroRadius.card))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${profile.displayName}, ${profile.age}",
                    style = MeroType.Title.copy(color = colors.text),
                    modifier = Modifier.weight(1f)
                )
                if (compatibilityPct != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(MeroRadius.pill))
                            .clickable(enabled = onOpenCompatibility != null) { onOpenCompatibility?.invoke() }
                            .testTag("detail_compatibility_pill")
                    ) {
                        Text(
                            text = if (isNe) "❤️ $compatibilityPct% मिल्ने" else "❤️ $compatibilityPct% Match",
                            style = MeroType.Caption.copy(color = colors.onPrimary, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = colors.primary)
                Text(
                    text = if (distanceLabel != null) {
                        "${profile.publicLocationLabel} · $distanceLabel"
                    } else {
                        profile.publicLocationLabel
                    },
                    style = MeroType.Body.copy(color = colors.textMuted),
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            MeroPresenceBadge(
                isOnline = LocationServiceHelper.isProfileOnline(profile),
                label = LocationServiceHelper.formatPresenceLabel(profile, isNe),
                modifier = Modifier.testTag("detail_presence_badge")
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                if (vibeCard != null) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${vibeCard.emoji} ${if (isNe) vibeCard.titleNe else vibeCard.titleEn}",
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                        )
                    }
                }
                if (profile.culturalBadge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.pill),
                        color = colors.surface2
                    ) {
                        Text(
                            text = profile.culturalBadge,
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                        )
                    }
                }
            }

            if (profile.bio.isNotBlank()) {
                Text(text = profile.bio, style = MeroType.Body.copy(color = colors.text))
            }
            if (profile.dailyQuestionAnswer.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        Text(
                            text = if (isNe) "मायालु दैनिक जवाफ 💬" else "Mayalu Daily Answer 💬",
                            style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = profile.dailyQuestionAnswer,
                            style = MeroType.Body.copy(color = colors.text)
                        )
                    }
                }
            }
            if (profile.lookingFor.isNotBlank()) {
                Text(
                    text = "${s.lookingForLabel}: ${profile.lookingFor}",
                    style = MeroType.Body.copy(color = colors.textMuted)
                )
            }
            if (profile.prompts.isNotEmpty()) {
                profile.prompts.forEach { entry ->
                    val parts = entry.split("::", limit = 2)
                    Surface(
                        shape = RoundedCornerShape(MeroRadius.small),
                        color = colors.surface2,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier.padding(MeroSpacing.md)
                        ) {
                            Text(
                                text = parts[0],
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                            if (parts.size > 1 && parts[1].isNotBlank()) {
                                Text(
                                    text = parts[1],
                                    style = MeroType.Body.copy(color = colors.text)
                                )
                            }
                        }
                    }
                }
            }
            if (profile.interests.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
                ) {
                    profile.interests.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(MeroRadius.pill),
                            color = colors.surface2
                        ) {
                            Text(
                                text = tag,
                                style = MeroType.Caption.copy(color = colors.text),
                                modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.xs)
                            )
                        }
                    }
                }
            }

            if (onSendCompliment != null && myProfile?.uid != profile.uid) {
                MeroSecondaryButton(
                    text = if (isNe) "🌸 गोप्य प्रशंसा पठाउनुहोस्" else "🌸 Send Secret Compliment",
                    onClick = { showComplimentPicker = !showComplimentPicker },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "detail_secret_compliment_button"
                )
                if (showComplimentPicker) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LocationServiceHelper.RESPECTFUL_COMPLIMENTS.forEachIndexed { idx, (compKey, pair) ->
                            val textToSend = if (isNe) pair.second else pair.first
                            MeroChip(
                                label = textToSend,
                                selected = false,
                                onClick = {
                                    showComplimentPicker = false
                                    onSendCompliment(compKey, textToSend)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "compliment_option_$idx"
                            )
                        }
                    }
                }
            }

            if (canSendInterest) {
                MeroButton(
                    text = s.interested,
                    leadingIcon = Icons.Filled.Favorite,
                    onClick = {
                        onDismiss()
                        onInterested()
                    },
                    testTag = "detail_interested_button"
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroSecondaryButton(
                    text = s.reportUser,
                    onClick = { onDismiss(); onReport() },
                    modifier = Modifier.weight(1f),
                    testTag = "detail_report_button"
                )
                MeroSecondaryButton(
                    text = s.blockUser,
                    isDanger = true,
                    onClick = { onDismiss(); onBlock() },
                    modifier = Modifier.weight(1f),
                    testTag = "detail_block_button"
                )
            }
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@Composable
fun ReportUserDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val categories = listOf(
        "HARASSMENT" to if (isNe) "दुर्व्यवहार" else "Harassment",
        "FAKE_PROFILE" to if (isNe) "नक्कली प्रोफाइल" else "Fake Profile",
        "INAPPROPRIATE_CONTENT" to if (isNe) "अनुचित सामग्री" else "Inappropriate Content",
        "UNDERAGE_CONCERN" to if (isNe) "उमेर सम्बन्धी शंका (१८-)" else "Underage Concern",
        "OTHER" to if (isNe) "अन्य" else "Other"
    )
    var selectedCategory by remember { mutableStateOf("HARASSMENT") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                Icon(imageVector = Icons.Filled.Shield, contentDescription = null, tint = colors.danger)
                Text(text = s.reportUser, style = MeroType.Heading.copy(color = colors.text))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                categories.forEach { (code, label) ->
                    MeroChip(
                        label = label,
                        selected = selectedCategory == code,
                        onClick = { selectedCategory = code },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MeroInput(
                    label = if (isNe) "विवरण (ऐच्छिक)" else "Details (Optional)",
                    value = details,
                    onValueChange = { if (it.length <= 400) details = it },
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            MeroButton(
                text = s.confirm,
                onClick = { onSubmit(selectedCategory, details) },
                testTag = "report_submit_button"
            )
        },
        dismissButton = {
            MeroSecondaryButton(
                text = s.cancel,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun HelpSafetyDialog(language: AppLanguage, onDismiss: () -> Unit) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(s.helpSafety, style = MeroType.Heading.copy(color = colors.text)) },
        text = {
            Text(
                text = if (isNe) {
                    "• दुवै पक्षको सहमति बिना सन्देश आदानप्रदान हुँदैन।\n• असहज लागेमा च्याट वा प्रोफाइल मेनुबाट तुरुन्त ब्लक र रिपोर्ट गर्नुहोस्।\n• पहिलो भेटघाट सधैं सार्वजनिक चिया पसल वा सम्पदा स्थलमा गर्नुहोस् र नजिकको साथीलाई जानकारी दिनुहोस्।"
                } else {
                    "• Private chat never opens without mutual approval.\n• Use Block or Report anytime from any profile or chat header.\n• Always choose public cafes or heritage landmarks for first meetups and keep personal financial details private."
                },
                style = MeroType.Body.copy(color = colors.textMuted)
            )
        },
        confirmButton = {
            MeroButton(text = s.close, onClick = onDismiss, testTag = "help_close_button")
        }
    )
}

@Composable
fun AboutAppDialog(language: AppLanguage, onDismiss: () -> Unit) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val context = LocalContext.current
    val creditUrl = "https://www.facebook.com/share/1EkV9EUs23/"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("${s.aboutApp} · v6.0 MAX", style = MeroType.Heading.copy(color = colors.text)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                Text(
                    text = "${s.appName} (v6.0 MAX) — ${s.tagline}",
                    style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold)
                )
                Text(text = s.privacyNoticeSummary, style = MeroType.Body.copy(color = colors.textMuted))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(MeroRadius.small))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(creditUrl))
                                )
                            }
                        }
                        .padding(vertical = MeroSpacing.sm)
                        .testTag("about_developer_credit_link")
                ) {
                    Text(
                        text = s.developerCredit,
                        style = MeroType.Caption.copy(
                            color = colors.primary,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = creditUrl,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            MeroButton(text = s.close, onClick = onDismiss, testTag = "about_close_button")
        }
    )
}

@Composable
fun SearchByIdDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> Result<PublicProfile>,
    onProfileFound: (PublicProfile) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val scope = rememberCoroutineScope()
    var uidInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSearching) onDismiss() },
        containerColor = colors.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = s.searchByIdTitle,
                    style = MeroType.Heading.copy(color = colors.text)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                MeroInput(
                    label = s.myUserIdLabel,
                    value = uidInput,
                    onValueChange = {
                        uidInput = it.trim().take(64)
                        errorMsg = null
                    },
                    placeholder = s.searchByIdPlaceholder,
                    singleLine = true,
                    testTag = "search_by_id_input"
                )
                if (isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = colors.primary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (language == AppLanguage.NEPALI) "प्रोफाइल खोजिँदैछ..." else "Searching member profile...",
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                }
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MeroType.Caption.copy(color = colors.danger),
                        modifier = Modifier.testTag("search_by_id_error")
                    )
                }
            }
        },
        confirmButton = {
            MeroButton(
                text = s.searchByIdTitle,
                enabled = uidInput.isNotBlank() && !isSearching,
                isLoading = isSearching,
                onClick = {
                    scope.launch {
                        isSearching = true
                        errorMsg = null
                        val res = onSearch(uidInput)
                        isSearching = false
                        res.onSuccess { profile ->
                            onDismiss()
                            onProfileFound(profile)
                        }.onFailure { err ->
                            errorMsg = err.localizedMessage
                        }
                    }
                },
                testTag = "search_by_id_submit_button"
            )
        },
        dismissButton = {
            MeroSecondaryButton(
                text = s.cancel,
                enabled = !isSearching,
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "search_by_id_cancel_button"
            )
        }
    )
}

@Composable
fun FirstDayCommunityDisclaimerDialog(
    language: AppLanguage,
    onAccept: () -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor = colors.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = s.disclaimerTitle,
                    style = MeroType.Heading.copy(color = colors.text)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = s.disclaimerBody,
                    style = MeroType.Body.copy(color = colors.textMuted)
                )
            }
        },
        confirmButton = {
            MeroButton(
                text = s.disclaimerAcceptButton,
                onClick = onAccept,
                testTag = "disclaimer_accept_button"
            )
        }
    )
}

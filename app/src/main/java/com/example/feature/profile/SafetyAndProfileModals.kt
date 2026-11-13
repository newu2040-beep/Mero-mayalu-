package com.example.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroBannerImage
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileDetailSheet(
    profile: PublicProfile,
    language: AppLanguage,
    canSendInterest: Boolean,
    onDismiss: () -> Unit,
    onInterested: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)

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

            Text(
                text = "${profile.displayName}, ${profile.age}",
                style = MeroType.Title.copy(color = colors.text)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null, tint = colors.primary)
                Text(
                    text = profile.publicLocationLabel,
                    style = MeroType.Body.copy(color = colors.textMuted)
                )
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

            if (profile.bio.isNotBlank()) {
                Text(text = profile.bio, style = MeroType.Body.copy(color = colors.text))
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text(s.aboutApp, style = MeroType.Heading.copy(color = colors.text)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)) {
                Text(text = "${s.appName} — ${s.tagline}", style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Bold))
                Text(text = s.privacyNoticeSummary, style = MeroType.Body.copy(color = colors.textMuted))
                Text(text = s.developerCredit, style = MeroType.Caption.copy(color = colors.primary))
            }
        },
        confirmButton = {
            MeroButton(text = s.close, onClick = onDismiss, testTag = "about_close_button")
        }
    )
}

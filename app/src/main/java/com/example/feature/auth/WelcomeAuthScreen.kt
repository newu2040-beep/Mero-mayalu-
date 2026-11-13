package com.example.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroCard
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroLotusEmblem
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeAuthScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onContinueWithGoogle: () -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    var showPolicySheet by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_welcome_himalayan_dusk),
            contentDescription = s.appName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Himalayan dusk scrim ensuring >= 4.5:1 readability across all widths
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x88231019),
                            Color(0x55231019),
                            Color(0xCC1A0C13),
                            Color(0xF51A0C13)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.lg)
        ) {
            // Top language toggle row
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                MeroChip(
                    label = if (language == AppLanguage.NEPALI) "नेपाली / EN" else "English / ने",
                    selected = false,
                    leadingIcon = Icons.Filled.Language,
                    onClick = {
                        val next = if (language == AppLanguage.NEPALI) AppLanguage.ENGLISH else AppLanguage.NEPALI
                        onLanguageChange(next)
                    },
                    testTag = "auth_language_toggle"
                )
            }

            Spacer(Modifier.height(MeroSpacing.xl))

            // Hero brand emblem
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                MeroLotusEmblem(size = 52.dp, tint = Color(0xFFFFD1DF))
            }

            Spacer(Modifier.height(MeroSpacing.md))

            Text(
                text = s.appName,
                style = MeroType.Title.copy(color = Color.White),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(MeroSpacing.xs))
            Text(
                text = s.tagline,
                style = MeroType.Heading.copy(color = Color(0xFFFFD6E4), fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(MeroSpacing.xxl))

            // Respectful mutual-consent callout + Google Sign-In
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.card),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Color(0xFFFFD6E4),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = s.privacyNoticeSummary,
                            style = MeroType.Caption.copy(color = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    MeroCard {
                        Text(
                            text = errorMessage,
                            style = MeroType.Caption.copy(color = colors.danger),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                MeroButton(
                    text = if (isLoading) s.signingIn else s.continueWithGoogle,
                    onClick = onContinueWithGoogle,
                    isLoading = isLoading,
                    leadingIcon = Icons.Filled.AccountCircle,
                    testTag = "google_sign_in_button"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showPolicySheet = "terms" },
                        modifier = Modifier.testTag("auth_terms_button")
                    ) {
                        Text(
                            text = s.termsLabel,
                            style = MeroType.Caption.copy(color = Color.White)
                        )
                    }
                    Text(
                        text = "•",
                        style = MeroType.Caption.copy(color = Color(0xFFFFD6E4))
                    )
                    TextButton(
                        onClick = { showPolicySheet = "privacy" },
                        modifier = Modifier.testTag("auth_privacy_button")
                    ) {
                        Text(
                            text = s.privacyLabel,
                            style = MeroType.Caption.copy(color = Color.White)
                        )
                    }
                }
            }
        }
    }

    if (showPolicySheet != null) {
        val isTerms = showPolicySheet == "terms"
        ModalBottomSheet(
            onDismissRequest = { showPolicySheet = null },
            containerColor = colors.surface
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.md)
            ) {
                Text(
                    text = if (isTerms) s.termsLabel else s.privacyLabel,
                    style = MeroType.Title.copy(color = colors.text)
                )
                Text(
                    text = if (isTerms) {
                        if (language == AppLanguage.NEPALI) {
                            "१. मेरो मायालु १८+ उमेरका वयस्कहरूका लागि मात्र हो।\n२. लिङ्ग चयन (पुरुष / महिला) खाता दर्ता पछि स्थिर रहन्छ र पारस्परिक सम्मानका साथ योग्य प्रोफाइल मात्र देखाइन्छ।\n३. एक पटकमा एक मात्र सक्रिय चासो अनुरोध पठाउन सकिन्छ। दुवै पक्षको सहमति बिना सन्देश खुल्दैन।\n४. उत्पीडन, नक्कली पहिचान वा असभ्य व्यवहार तुरुन्त प्रतिबन्धित हुन्छ।"
                        } else {
                            "1. Mero Mayalu is strictly for verified adults aged 18 and older.\n2. Gender selection (Male / Female) governs discovery eligibility.\n3. You may keep at most 1 pending outgoing interest request at a time; private chat unlocks only after mutual consent.\n4. Harassment, impersonation, or abuse results in immediate account action."
                        }
                    } else {
                        if (language == AppLanguage.NEPALI) {
                            "• तपाईंको जन्म मिति निजी खातामा मात्र सुरक्षित रहन्छ र सार्वजनिक प्रोफाइलमा कहिल्यै देखाइँदैन।\n• स्थान विवरण केवल प्रदेश/जिल्ला/शहर स्तरमा देखाइन्छ, घरको ठेगाना वा प्रत्यक्ष GPS कहिल्यै प्रकाशित हुँदैन।\n• सेटिङबाट जुनसुकै बेला डेटा JSON निर्यात वा खाता स्थायी रूपमा मेटाउन सकिन्छ।"
                        } else {
                            "• Your date of birth is kept strictly private and never published on discovery cards.\n• Location is shown only at the district/city level—never your exact coordinates or home address.\n• You can export your JSON data or permanently delete your account anytime in Settings."
                        }
                    },
                    style = MeroType.Body.copy(color = colors.textMuted)
                )
                Spacer(Modifier.height(MeroSpacing.sm))
                MeroButton(
                    text = s.close,
                    onClick = { showPolicySheet = null },
                    testTag = "policy_close_button"
                )
                Spacer(Modifier.height(MeroSpacing.lg))
            }
        }
    }
}

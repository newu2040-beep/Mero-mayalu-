package com.example.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.example.core.designsystem.MeroTopBar
import com.example.core.designsystem.MeroType
import com.example.core.domain.PrivateUser
import com.example.core.domain.PublicProfile
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.BikramSambatConverter
import com.example.core.util.MediaHelper
import com.example.core.util.NepaliCultureCatalog
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.Period
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    currentUid: String,
    language: AppLanguage,
    existingPrivate: PrivateUser?,
    existingPublic: PublicProfile?,
    isBusy: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onSave: (PrivateUser, PublicProfile, Boolean) -> Unit,
    onCancelEdit: (() -> Unit)? = null
) {
    val isEditMode = existingPrivate != null && existingPublic != null
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNe = language == AppLanguage.NEPALI

    var step by rememberSaveable { mutableIntStateOf(0) }
    var displayName by rememberSaveable { mutableStateOf(existingPublic?.displayName ?: existingPrivate?.displayName ?: "") }
    var dobText by rememberSaveable { mutableStateOf(existingPrivate?.dateOfBirthPrivate ?: "2000-05-14") }
    var dobModeBs by rememberSaveable { mutableStateOf(false) }
    var bsDobText by rememberSaveable { mutableStateOf("2057-02-01") }
    var gender by rememberSaveable { mutableStateOf(existingPrivate?.gender ?: "Female") }
    var photos by rememberSaveable { mutableStateOf(existingPublic?.photoDataList ?: emptyList()) }
    var bio by rememberSaveable { mutableStateOf(existingPublic?.bio ?: "") }
    var lookingFor by rememberSaveable { mutableStateOf(existingPublic?.lookingFor ?: "") }
    var selectedPrompts by rememberSaveable { mutableStateOf(existingPublic?.prompts ?: emptyList()) }
    var customPromptQuestion by rememberSaveable { mutableStateOf("") }
    var customPromptAnswer by rememberSaveable { mutableStateOf("") }
    var selectedInterests by rememberSaveable { mutableStateOf(existingPublic?.interests ?: emptyList()) }
    var customInterestInput by rememberSaveable { mutableStateOf("") }
    var culturalBadge by rememberSaveable { mutableStateOf(existingPublic?.culturalBadge ?: "") }
    var province by rememberSaveable { mutableStateOf(existingPublic?.province ?: "Bagmati") }
    var district by rememberSaveable { mutableStateOf(existingPublic?.district ?: "Kathmandu") }
    var municipality by rememberSaveable { mutableStateOf(existingPublic?.municipality ?: "") }
    var useBs by rememberSaveable { mutableStateOf(existingPrivate?.useBikramSambat ?: true) }
    var acceptedTerms by rememberSaveable { mutableStateOf(isEditMode) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    BackHandler(enabled = step > 0 || onCancelEdit != null) {
        if (step > 0) step -= 1 else onCancelEdit?.invoke()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null && photos.size < 4) {
                    photos = photos + compressed
                    validationError = null
                }
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null && photos.size < 4) {
                    photos = photos + compressed
                    validationError = null
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            scope.launch {
                val compressed = MediaHelper.compressUriToDataUri(context, uri)
                if (compressed != null && photos.size < 4) {
                    photos = photos + compressed
                    validationError = null
                }
            }
        }
    }

    fun launchCamera() {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else validationError = if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is required to capture a photo."
    }

    @SuppressLint("MissingPermission")
    fun resolveCoarseCity() {
        scope.launch {
            runCatching {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val loc = fused.lastLocation.await()
                if (loc != null) {
                    val city = withContext(Dispatchers.IO) {
                        @Suppress("DEPRECATION")
                        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                        } else {
                            Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                        }
                        addresses?.firstOrNull()?.locality ?: addresses?.firstOrNull()?.subAdminArea
                    }
                    if (!city.isNullOrBlank()) municipality = city
                }
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) resolveCoarseCity()
    }

    fun resolvedAdDobString(): String? {
        return if (dobModeBs) {
            BikramSambatConverter.parseBsToLocalDate(bsDobText)?.toString()
        } else {
            runCatching { LocalDate.parse(dobText.trim()).toString() }.getOrNull()
        }
    }

    fun computeAgeFromInput(): Int? {
        val adIso = resolvedAdDobString() ?: return null
        return runCatching {
            val parsed = LocalDate.parse(adIso)
            Period.between(parsed, LocalDate.now()).years
        }.getOrNull()
    }

    val stepTitles = listOf(
        s.stepIdentity,
        s.stepGender,
        s.stepPhoto,
        s.stepAbout,
        s.stepInterests,
        s.stepLocation,
        s.stepPreferences
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        MeroTopBar(
            title = if (isEditMode) s.editProfile else s.onboardingTitle,
            leadingAction = if (step > 0 || onCancelEdit != null) {
                {
                    MeroIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        onClick = { if (step > 0) step -= 1 else onCancelEdit?.invoke() },
                        testTag = "onboarding_back_button"
                    )
                }
            } else null
        )

        LinearProgressIndicator(
            progress = { (step + 1) / stepTitles.size.toFloat() },
            color = colors.primary,
            trackColor = colors.surface2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MeroSpacing.lg)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.lg),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(MeroSpacing.lg)
        ) {
            Text(
                text = "${step + 1}/${stepTitles.size} · ${stepTitles[step]}",
                style = MeroType.Heading.copy(color = colors.text)
            )

            when (step) {
                0 -> {
                    MeroCard {
                        MeroInput(
                            label = s.displayNameLabel,
                            value = displayName,
                            onValueChange = { displayName = it; validationError = null },
                            placeholder = if (isNe) "जस्तै: सिर्जना थापा" else "e.g. Srijana Thapa",
                            testTag = "input_display_name"
                        )
                        if (!isEditMode) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                MeroChip(
                                    label = s.dobModeAd,
                                    selected = !dobModeBs,
                                    onClick = {
                                        if (dobModeBs) {
                                            BikramSambatConverter.parseBsToLocalDate(bsDobText)?.let {
                                                dobText = it.toString()
                                            }
                                            dobModeBs = false
                                            validationError = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "dob_mode_ad_chip"
                                )
                                MeroChip(
                                    label = s.dobModeBs,
                                    selected = dobModeBs,
                                    onClick = {
                                        if (!dobModeBs) {
                                            runCatching { LocalDate.parse(dobText.trim()) }.getOrNull()?.let { ad ->
                                                BikramSambatConverter.fromLocalDate(ad)?.let { bs ->
                                                    bsDobText = bs.formatNumeric()
                                                }
                                            }
                                            dobModeBs = true
                                            validationError = null
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "dob_mode_bs_chip"
                                )
                            }

                            if (!dobModeBs) {
                                MeroInput(
                                    label = s.dobLabel,
                                    value = dobText,
                                    onValueChange = { dobText = it; validationError = null },
                                    placeholder = "2000-05-14",
                                    testTag = "input_dob"
                                )
                                val previewBs = runCatching { LocalDate.parse(dobText.trim()) }.getOrNull()
                                    ?.let { BikramSambatConverter.fromLocalDate(it)?.format(isNe) }
                                Text(
                                    text = if (previewBs != null) "${s.dobHint} · $previewBs" else s.dobHint,
                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                )
                            } else {
                                MeroInput(
                                    label = s.dobBsLabel,
                                    value = bsDobText,
                                    onValueChange = { bsDobText = it; validationError = null },
                                    placeholder = "2057-02-01",
                                    testTag = "input_dob_bs"
                                )
                                val convertedAd = BikramSambatConverter.parseBsToLocalDate(bsDobText)
                                Text(
                                    text = if (convertedAd != null) {
                                        val ageVal = Period.between(convertedAd, LocalDate.now()).years
                                        "${s.dobBsHint} · AD $convertedAd ($ageVal yrs)"
                                    } else {
                                        s.dobBsHint
                                    },
                                    style = MeroType.Caption.copy(color = colors.textMuted)
                                )
                            }
                        }
                    }
                }
                1 -> {
                    MeroCard {
                        Text(text = s.genderLabel, style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        Row(horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                            MeroChip(
                                label = s.genderFemale,
                                selected = gender == "Female",
                                onClick = { if (!isEditMode) gender = "Female" },
                                modifier = Modifier.weight(1f),
                                testTag = "gender_female_chip"
                            )
                            MeroChip(
                                label = s.genderMale,
                                selected = gender == "Male",
                                onClick = { if (!isEditMode) gender = "Male" },
                                modifier = Modifier.weight(1f),
                                testTag = "gender_male_chip"
                            )
                        }
                        Text(
                            text = s.genderEligibilityNote,
                            style = MeroType.Caption.copy(color = colors.textMuted)
                        )
                    }
                }
                2 -> {
                    MeroCard {
                        Text(text = s.photoRequiredNote, style = MeroType.Body.copy(color = colors.textMuted))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            photos.forEachIndexed { idx, photo ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(MeroRadius.card))
                                ) {
                                    MeroPhotoBox(
                                        photoData = photo,
                                        contentDescription = "Photo ${idx + 1}",
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
                                            contentDescription = "Remove",
                                            tint = colors.danger,
                                            onClick = { photos = photos.filterIndexed { i, _ -> i != idx } },
                                            testTag = "remove_photo_$idx"
                                        )
                                    }
                                }
                            }
                        }
                        MeroSecondaryButton(
                            text = s.selectPhoto,
                            leadingIcon = Icons.Filled.AddAPhoto,
                            enabled = photos.size < 4,
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "pick_photo_button"
                        )
                        MeroSecondaryButton(
                            text = s.browseFoldersPhoto,
                            leadingIcon = Icons.Filled.FolderOpen,
                            enabled = photos.size < 4,
                            onClick = { folderPickerLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "browse_folders_photo_button"
                        )
                        MeroSecondaryButton(
                            text = s.takePhoto,
                            leadingIcon = Icons.Filled.CameraAlt,
                            enabled = photos.size < 4,
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "take_photo_button"
                        )
                    }
                }
                3 -> {
                    MeroCard {
                        MeroInput(
                            label = s.bioLabel,
                            value = bio,
                            onValueChange = { if (it.length <= 240) bio = it },
                            singleLine = false,
                            maxLines = 4,
                            placeholder = if (isNe) "तपाईंको बारेमा केही मिठो कुरा..." else "Dream big, stay kind...",
                            testTag = "input_bio"
                        )
                        MeroInput(
                            label = s.lookingForLabel,
                            value = lookingFor,
                            onValueChange = { if (it.length <= 120) lookingFor = it },
                            placeholder = if (isNe) "सार्थक र सम्मानजनक मित्रता / सम्बन्ध" else "Meaningful, respectful connection",
                            testTag = "input_looking_for"
                        )
                        Text(text = s.promptsLabel, style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        NepaliCultureCatalog.prompts.forEach { (en, ne) ->
                            val label = if (isNe) ne else en
                            val alreadyAdded = selectedPrompts.any { it.startsWith("$label::") || it == label }
                            MeroChip(
                                label = label,
                                selected = customPromptQuestion == label || alreadyAdded,
                                leadingIcon = if (alreadyAdded) Icons.Filled.Check else null,
                                onClick = {
                                    customPromptQuestion = label
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        MeroInput(
                            label = s.customPromptQuestionLabel,
                            value = customPromptQuestion,
                            onValueChange = { if (it.length <= 100) customPromptQuestion = it },
                            placeholder = if (isNe) "जस्तै: चिया पसल कि पुस्तकालय?" else "e.g. Chiya date or sunrise hike?",
                            testTag = "input_custom_prompt_question"
                        )
                        MeroInput(
                            label = s.customPromptAnswerLabel,
                            value = customPromptAnswer,
                            onValueChange = { if (it.length <= 160) customPromptAnswer = it },
                            placeholder = if (isNe) "तपाईंको मनपर्ने उत्तर लेख्नुहोस्..." else "Write your own answer...",
                            testTag = "input_custom_prompt_answer"
                        )
                        MeroSecondaryButton(
                            text = s.addCustomPrompt,
                            leadingIcon = Icons.Filled.Add,
                            enabled = customPromptQuestion.trim().isNotBlank() && selectedPrompts.size < 6,
                            onClick = {
                                val q = customPromptQuestion.trim()
                                val a = customPromptAnswer.trim()
                                val entry = if (a.isNotBlank()) "$q::$a" else q
                                val filtered = selectedPrompts.filterNot { it.startsWith("$q::") || it == q }
                                selectedPrompts = (filtered + entry).take(6)
                                customPromptQuestion = ""
                                customPromptAnswer = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "add_custom_prompt_button"
                        )
                        if (selectedPrompts.isNotEmpty()) {
                            selectedPrompts.forEach { item ->
                                val parts = item.split("::", limit = 2)
                                Surface(
                                    shape = RoundedCornerShape(MeroRadius.small),
                                    color = colors.surface2,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = MeroSpacing.md, vertical = MeroSpacing.sm)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
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
                                        MeroIconButton(
                                            icon = Icons.Filled.Close,
                                            contentDescription = s.cancel,
                                            tint = colors.textMuted,
                                            onClick = { selectedPrompts = selectedPrompts - item }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    MeroCard {
                        Text(text = s.myInterests, style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                        ) {
                            val builtInLabels = NepaliCultureCatalog.interests.map { (en, ne) ->
                                (if (isNe) ne else en) to en
                            }
                            NepaliCultureCatalog.interests.forEach { (en, ne) ->
                                val tag = if (isNe) ne else en
                                val chosen = tag in selectedInterests || en in selectedInterests
                                MeroChip(
                                    label = tag,
                                    selected = chosen,
                                    onClick = {
                                        selectedInterests = if (chosen) {
                                            selectedInterests - tag - en
                                        } else {
                                            (selectedInterests + tag).take(16)
                                        }
                                    }
                                )
                            }
                            val builtInSet = builtInLabels.flatMap { listOf(it.first, it.second) }.toSet()
                            selectedInterests.filter { it !in builtInSet }.forEach { customTag ->
                                MeroChip(
                                    label = customTag,
                                    selected = true,
                                    leadingIcon = Icons.Filled.Close,
                                    onClick = { selectedInterests = selectedInterests - customTag }
                                )
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
                                placeholder = if (isNe) "जस्तै: सरोद, कविता, फोटोग्राफी" else "e.g. Sarangi, Poetry, Trekking",
                                modifier = Modifier.weight(1f),
                                testTag = "input_custom_interest"
                            )
                            MeroSecondaryButton(
                                text = s.addCustomInterest,
                                enabled = customInterestInput.trim().isNotBlank() && selectedInterests.size < 16,
                                onClick = {
                                    val clean = customInterestInput.trim()
                                    if (clean.isNotBlank() && selectedInterests.none { it.equals(clean, ignoreCase = true) }) {
                                        selectedInterests = (selectedInterests + clean).take(16)
                                    }
                                    customInterestInput = ""
                                },
                                testTag = "add_custom_interest_button"
                            )
                        }
                        Spacer(Modifier.height(MeroSpacing.sm))
                        Text(text = s.culturalBadgeLabel, style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        NepaliCultureCatalog.culturalBadges.forEach { (en, ne) ->
                            val badgeVal = if (en.isBlank()) "" else if (isNe) ne else en
                            MeroChip(
                                label = if (en.isBlank()) ne else if (isNe) ne else en,
                                selected = culturalBadge == badgeVal,
                                onClick = { culturalBadge = badgeVal },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                5 -> {
                    MeroCard {
                        Text(text = s.provinceLabel, style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                        ) {
                            NepaliCultureCatalog.provincesAndDistricts.keys.forEach { prov ->
                                MeroChip(
                                    label = prov,
                                    selected = province == prov,
                                    onClick = {
                                        province = prov
                                        district = NepaliCultureCatalog.provincesAndDistricts[prov]?.firstOrNull() ?: ""
                                    }
                                )
                            }
                        }
                        Text(text = s.districtLabel, style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
                        ) {
                            NepaliCultureCatalog.provincesAndDistricts[province].orEmpty().forEach { dist ->
                                MeroChip(
                                    label = dist,
                                    selected = district == dist,
                                    onClick = { district = dist }
                                )
                            }
                        }
                        MeroInput(
                            label = s.municipalityLabel,
                            value = municipality,
                            onValueChange = { municipality = it },
                            placeholder = if (isNe) "जस्तै: ललितपुर महानगर" else "e.g. Lalitpur",
                            testTag = "input_municipality"
                        )
                        MeroSecondaryButton(
                            text = s.detectCity,
                            leadingIcon = Icons.Filled.MyLocation,
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) resolveCoarseCity()
                                else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "detect_city_button"
                        )
                        Text(text = s.locationPrivacyNote, style = MeroType.Caption.copy(color = colors.textMuted))
                    }
                }
                6 -> {
                    MeroCard {
                        Text(text = s.languageLabel, style = MeroType.Body.copy(color = colors.text, fontWeight = FontWeight.Medium))
                        Row(horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                            AppLanguage.entries.forEach { lang ->
                                MeroChip(
                                    label = lang.label,
                                    selected = language == lang,
                                    onClick = { onLanguageChange(lang) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = s.bikramSambatToggle,
                                style = MeroType.Body.copy(color = colors.text),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(checked = useBs, onCheckedChange = { useBs = it })
                        }
                        if (!isEditMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(MeroRadius.small))
                                    .clickable { acceptedTerms = !acceptedTerms }
                                    .padding(vertical = MeroSpacing.xs)
                            ) {
                                Checkbox(
                                    checked = acceptedTerms,
                                    onCheckedChange = { acceptedTerms = it },
                                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                                )
                                Text(
                                    text = s.acceptTermsCheckbox,
                                    style = MeroType.Body.copy(color = colors.text),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError.orEmpty(),
                    style = MeroType.Caption.copy(color = colors.danger),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MeroButton(
                text = if (step == stepTitles.lastIndex) {
                    if (isEditMode) s.saveChanges else s.publishProfile
                } else s.next,
                isLoading = isBusy,
                onClick = {
                    validationError = null
                    when (step) {
                        0 -> {
                            if (displayName.trim().length < 2) {
                                validationError = if (isNe) "कम्तीमा २ अक्षरको नाम लेख्नुहोस्।" else "Please enter a name (2+ chars)."
                                return@MeroButton
                            }
                            if (!isEditMode) {
                                val age = computeAgeFromInput()
                                if (age == null || age < 18) {
                                    validationError = s.age18Error
                                    return@MeroButton
                                }
                            }
                            step += 1
                        }
                        2 -> {
                            if (photos.isEmpty()) {
                                validationError = s.photoRequiredNote
                                return@MeroButton
                            }
                            step += 1
                        }
                        stepTitles.lastIndex -> {
                            if (!isEditMode && !acceptedTerms) {
                                validationError = s.acceptTermsCheckbox
                                return@MeroButton
                            }
                            val resolvedDobIso = resolvedAdDobString() ?: dobText.trim()
                            val resolvedAge = if (isEditMode) existingPrivate?.age ?: 18 else computeAgeFromInput() ?: 18
                            val locLabel = if (municipality.isNotBlank() && !municipality.equals(district, true)) {
                                "$municipality, $district"
                            } else {
                                "$district, Nepal"
                            }
                            val priv = (existingPrivate ?: PrivateUser()).copy(
                                uid = currentUid,
                                displayName = displayName.trim(),
                                dateOfBirthPrivate = if (isEditMode) existingPrivate?.dateOfBirthPrivate.orEmpty() else resolvedDobIso,
                                age = resolvedAge,
                                ageVerified = resolvedAge >= 18,
                                gender = if (isEditMode) existingPrivate?.gender ?: gender else gender,
                                language = language.code,
                                useBikramSambat = useBs
                            )
                            val pub = (existingPublic ?: PublicProfile()).copy(
                                uid = currentUid,
                                displayName = displayName.trim(),
                                age = resolvedAge,
                                ageVerified = resolvedAge >= 18,
                                gender = priv.gender,
                                bio = bio.trim(),
                                lookingFor = lookingFor.trim(),
                                photoDataList = photos,
                                bannerData = existingPublic?.bannerData?.ifBlank { "preset:himalayan_dusk" } ?: "preset:himalayan_dusk",
                                interests = selectedInterests,
                                prompts = selectedPrompts,
                                culturalBadge = culturalBadge,
                                province = province,
                                district = district,
                                municipality = municipality.trim(),
                                publicLocationLabel = locLabel
                            )
                            onSave(priv, pub, !isEditMode)
                        }
                        else -> step += 1
                    }
                },
                testTag = "onboarding_next_button"
            )
        }
    }
}

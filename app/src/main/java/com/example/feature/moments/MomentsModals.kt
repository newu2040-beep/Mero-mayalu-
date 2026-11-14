package com.example.feature.moments

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.core.designsystem.LocalMeroColors
import com.example.core.designsystem.MeroButton
import com.example.core.designsystem.MeroChip
import com.example.core.designsystem.MeroIconButton
import com.example.core.designsystem.MeroInput
import com.example.core.designsystem.MeroPhotoBox
import com.example.core.designsystem.MeroRadius
import com.example.core.designsystem.MeroSecondaryButton
import com.example.core.designsystem.MeroSpacing
import com.example.core.designsystem.MeroType
import com.example.core.domain.CulturePost
import com.example.core.domain.SavedNote
import com.example.core.localization.AppLanguage
import com.example.core.localization.AppStrings
import com.example.core.util.MediaHelper
import com.example.core.util.NepaliCultureCatalog
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateMomentSheet(
    language: AppLanguage,
    initialThought: String = "",
    initialPollMode: Boolean = false,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onPublish: (
        thoughtText: String,
        imageData: String,
        culturalTag: String,
        postType: String,
        pollOptions: List<String>,
        scheduledAt: Timestamp?
    ) -> Unit,
    onShowError: (String) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPollMode by remember(initialPollMode) { mutableStateOf(initialPollMode) }
    var thoughtText by remember(initialThought) { mutableStateOf(initialThought) }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var selectedTag by remember {
        val first = NepaliCultureCatalog.thoughtTags.first()
        mutableStateOf(if (isNe) first.second else first.first)
    }
    var customTag by remember { mutableStateOf("") }
    var imageData by remember { mutableStateOf("") }
    var isProcessingImage by remember { mutableStateOf(false) }
    var imageUploadError by remember { mutableStateOf<String?>(null) }
    var lastFailedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hdImageEnabled by remember { mutableStateOf(true) }
    var isScheduled by remember { mutableStateOf(false) }
    var scheduledAtMillis by remember { mutableStateOf<Long?>(null) }
    var icebreakerIdx by remember { mutableIntStateOf(0) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun processImageUri(uri: Uri) {
        scope.launch {
            isProcessingImage = true
            imageUploadError = null
            lastFailedImageUri = null
            when (val res = MediaHelper.processImageUriWithResult(context, uri, hd = hdImageEnabled)) {
                is com.example.core.util.MediaUploadResult.Success -> {
                    imageData = res.dataUri
                }
                is com.example.core.util.MediaUploadResult.Error -> {
                    lastFailedImageUri = uri
                    imageUploadError = if (isNe) res.messageNe else res.messageEn
                }
            }
            isProcessingImage = false
        }
    }

    val legacyPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) processImageUri(uri)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) processImageUri(uri)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) processImageUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    fun launchCamera() {
        runCatching {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "moment_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }.onFailure {
            onShowError(
                if (isNe) "क्यामेरा खोल्न सकिएन। कृपया फेरि प्रयास गर्नुहोस्।"
                else "Unable to open device camera right now."
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else onShowError(if (isNe) "क्यामेरा अनुमति आवश्यक छ।" else "Camera permission is needed to take a photo.")
    }

    fun openDateTimePicker() {
        val base = Calendar.getInstance().apply {
            if (scheduledAtMillis != null && scheduledAtMillis!! > System.currentTimeMillis()) {
                timeInMillis = scheduledAtMillis!!
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
            }
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        if (picked.timeInMillis <= System.currentTimeMillis() + 60_000L) {
                            onShowError(
                                if (isNe) "कृपया भविष्यको मिति र समय छान्नुहोस्।"
                                else "Please pick a future date and time."
                            )
                        } else {
                            scheduledAtMillis = picked.timeInMillis
                            isScheduled = true
                        }
                    },
                    base.get(Calendar.HOUR_OF_DAY),
                    base.get(Calendar.MINUTE),
                    false
                ).show()
            },
            base.get(Calendar.YEAR),
            base.get(Calendar.MONTH),
            base.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1_000L
        }.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Text(
                text = s.shareThoughtCta,
                style = MeroType.Heading.copy(color = colors.text)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroChip(
                    label = if (isNe) "विचार / तस्बिर" else "Thought / Photo",
                    selected = !isPollMode,
                    onClick = { isPollMode = false },
                    modifier = Modifier.weight(1f),
                    testTag = "moment_mode_thought"
                )
                MeroChip(
                    label = if (isNe) "द्रुत मतदान" else "Quick Poll",
                    selected = isPollMode,
                    leadingIcon = Icons.Filled.Poll,
                    onClick = { isPollMode = true },
                    modifier = Modifier.weight(1f),
                    testTag = "moment_mode_poll"
                )
            }

            MeroInput(
                label = if (isPollMode) {
                    if (isNe) "तपाईंको प्रश्न (मतदान)" else "Your quick poll question"
                } else {
                    if (isNe) "तपाईंको विचार वा सन्देश" else "Your thought or cultural story"
                },
                value = thoughtText,
                onValueChange = { if (it.length <= 600) thoughtText = it },
                placeholder = if (isPollMode) {
                    if (isNe) "जस्तै: काठमाडौंको सबैभन्दा मनपर्ने चिया थलो कुन हो?" else "e.g., Best spot for a quiet chiya chat in Nepal?"
                } else {
                    s.thoughtPlaceholder
                },
                singleLine = false,
                maxLines = 5,
                testTag = "moment_thought_input"
            )

            if (isPollMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isNe) "मतदान विकल्पहरू (२–४)" else "Poll Options (2–4)",
                        style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
                    )
                    pollOptions.forEachIndexed { idx, optText ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MeroInput(
                                label = if (isNe) "विकल्प ${idx + 1}" else "Option ${idx + 1}",
                                value = optText,
                                onValueChange = { next ->
                                    if (next.length <= 60) {
                                        pollOptions = pollOptions.toMutableList().also { it[idx] = next }
                                    }
                                },
                                placeholder = if (isNe) "विकल्प लेख्नुहोस्" else "Enter choice...",
                                modifier = Modifier.weight(1f),
                                testTag = "moment_poll_option_$idx"
                            )
                            if (pollOptions.size > 2) {
                                MeroIconButton(
                                    icon = Icons.Filled.Close,
                                    contentDescription = s.cancel,
                                    tint = colors.danger,
                                    onClick = {
                                        pollOptions = pollOptions.toMutableList().also { it.removeAt(idx) }
                                    },
                                    testTag = "moment_poll_remove_$idx"
                                )
                            }
                        }
                    }
                    if (pollOptions.size < 4) {
                        MeroSecondaryButton(
                            text = if (isNe) "अर्को विकल्प थप्नुहोस्" else "Add Poll Option",
                            leadingIcon = Icons.Filled.Add,
                            onClick = { pollOptions = pollOptions + "" },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "moment_poll_add_option"
                        )
                    }
                }
            } else {
                MeroSecondaryButton(
                    text = s.icebreakerButton,
                    leadingIcon = Icons.Filled.AutoAwesome,
                    onClick = {
                        val item = NepaliCultureCatalog.icebreakers[icebreakerIdx % NepaliCultureCatalog.icebreakers.size]
                        icebreakerIdx += 1
                        thoughtText = if (isNe) item.second else item.first
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_icebreaker_button"
                )
            }

            Text(
                text = if (isNe) "सांस्कृतिक ट्याग" else "Cultural Tag",
                style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs)
            ) {
                NepaliCultureCatalog.thoughtTags.forEach { (en, ne) ->
                    val label = if (isNe) ne else en
                    MeroChip(
                        label = label,
                        selected = selectedTag == label && customTag.isBlank(),
                        onClick = {
                            selectedTag = label
                            customTag = ""
                        }
                    )
                }
            }
            MeroInput(
                label = if (isNe) "वा आफ्नै ट्याग लेख्नुहोस्" else "Or write a custom tag",
                value = customTag,
                onValueChange = { if (it.length <= 40) customTag = it },
                placeholder = if (isNe) "जस्तै: पोखरा साँझ" else "e.g., Kathmandu Chiya",
                testTag = "moment_custom_tag_input"
            )

            if (isProcessingImage) {
                com.example.core.designsystem.MeroLoadingState(
                    message = if (isNe) "तस्बिर लोड र अनुकूलन हुँदैछ..." else "Loading & optimizing image preview..."
                )
            }

            if (imageUploadError != null) {
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
                            text = imageUploadError.orEmpty(),
                            style = MeroType.Caption.copy(color = colors.danger, fontWeight = FontWeight.Medium)
                        )
                        if (lastFailedImageUri != null) {
                            MeroSecondaryButton(
                                text = s.retry,
                                onClick = {
                                    val retryUri = lastFailedImageUri
                                    if (retryUri != null) processImageUri(retryUri)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "moment_retry_image_button"
                            )
                        }
                    }
                }
            }

            if (imageData.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.25f)
                        .clip(RoundedCornerShape(MeroRadius.card))
                ) {
                    MeroPhotoBox(
                        photoData = imageData,
                        contentDescription = "Selected photo",
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(MeroSpacing.sm)
                            .clip(RoundedCornerShape(MeroRadius.pill))
                            .background(colors.bg.copy(alpha = 0.85f))
                    ) {
                        MeroIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = s.cancel,
                            tint = colors.danger,
                            onClick = { imageData = "" }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.HighQuality,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(
                        text = if (isNe) "HD उच्च गुणस्तर तस्बिर" else "HD Photo Quality",
                        style = MeroType.Body.copy(color = colors.text)
                    )
                }
                Switch(
                    checked = hdImageEnabled,
                    onCheckedChange = { hdImageEnabled = it }
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroSecondaryButton(
                    text = s.selectPhoto,
                    leadingIcon = Icons.Filled.AddAPhoto,
                    enabled = !isProcessingImage,
                    onClick = {
                        runCatching {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }.onFailure {
                            legacyPhotoLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_pick_photo"
                )
                MeroSecondaryButton(
                    text = s.browseFoldersPhoto,
                    leadingIcon = Icons.Filled.FolderOpen,
                    enabled = !isProcessingImage,
                    onClick = {
                        runCatching {
                            folderPickerLauncher.launch(MediaHelper.SUPPORTED_IMAGE_MIMES)
                        }.onFailure {
                            legacyPhotoLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_browse_folders"
                )
                MeroSecondaryButton(
                    text = s.takePhoto,
                    leadingIcon = Icons.Filled.CameraAlt,
                    enabled = !isProcessingImage,
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) launchCamera()
                        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "moment_take_photo"
                )
            }

            // Schedule Posting Section
            Text(
                text = if (isNe) "प्रकाशन समय" else "Publishing Schedule",
                style = MeroType.Caption.copy(color = colors.text, fontWeight = FontWeight.Medium)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                modifier = Modifier.fillMaxWidth()
            ) {
                MeroChip(
                    label = if (isNe) "अहिले पोस्ट" else "Post Now",
                    selected = !isScheduled,
                    onClick = {
                        isScheduled = false
                        scheduledAtMillis = null
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "moment_post_now_chip"
                )
                MeroChip(
                    label = if (isNe) "मिति तालिका" else "Schedule Date",
                    selected = isScheduled,
                    leadingIcon = Icons.Filled.Schedule,
                    onClick = {
                        isScheduled = true
                        if (scheduledAtMillis == null || scheduledAtMillis!! <= System.currentTimeMillis()) {
                            scheduledAtMillis = System.currentTimeMillis() + 3_600_000L
                        }
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "moment_schedule_chip"
                )
            }

            if (isScheduled) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        val formatted = remember(scheduledAtMillis) {
                            scheduledAtMillis?.let {
                                SimpleDateFormat("EEE, MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(it))
                            } ?: ""
                        }
                        if (formatted.isNotBlank()) {
                            Text(
                                text = if (isNe) "तालिका गरिएको: $formatted" else "Scheduled for: $formatted",
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MeroChip(
                                label = if (isNe) "+१ घण्टा" else "+1 Hour",
                                selected = false,
                                onClick = { scheduledAtMillis = System.currentTimeMillis() + 3_600_000L },
                                modifier = Modifier.weight(1f)
                            )
                            MeroChip(
                                label = if (isNe) "भोलि बिहान ९" else "Tomorrow 9 AM",
                                selected = false,
                                onClick = {
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                    }
                                    scheduledAtMillis = cal.timeInMillis
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        MeroSecondaryButton(
                            text = if (isNe) "आफ्नै मिति र समय छान्नुहोस्" else "Pick Custom Date & Time",
                            leadingIcon = Icons.Filled.CalendarMonth,
                            onClick = { openDateTimePicker() },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "moment_pick_schedule_datetime"
                        )
                    }
                }
            }

            val resolvedTag = customTag.trim().ifBlank { selectedTag }
            val cleanPollOptions = pollOptions.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val scheduleValid = !isScheduled || ((scheduledAtMillis ?: 0L) > System.currentTimeMillis())
            val canPublish = if (isPollMode) {
                thoughtText.trim().isNotBlank() && cleanPollOptions.size >= 2 && scheduleValid
            } else {
                (thoughtText.trim().isNotBlank() || imageData.isNotBlank()) && scheduleValid
            }
            val publishLabel = when {
                isScheduled && isPollMode -> if (isNe) "मतदान तालिका गर्नुहोस्" else "Schedule Quick Poll"
                isScheduled -> if (isNe) "पोस्ट तालिका गर्नुहोस्" else "Schedule Post"
                isPollMode -> if (isNe) "द्रुत मतदान प्रकाशित गर्नुहोस्" else "Publish Quick Poll"
                else -> if (isNe) "पोस्ट प्रकाशित गर्नुहोस्" else "Publish Cultural Moment"
            }

            MeroButton(
                text = publishLabel,
                enabled = canPublish && !isBusy,
                isLoading = isBusy,
                onClick = {
                    val ts = if (isScheduled && scheduledAtMillis != null) {
                        Timestamp(Date(scheduledAtMillis!!))
                    } else null
                    onPublish(
                        thoughtText.trim(),
                        imageData,
                        resolvedTag,
                        if (isPollMode) "POLL" else "THOUGHT",
                        if (isPollMode) cleanPollOptions else emptyList(),
                        ts
                    )
                },
                testTag = "moment_publish_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveNoteSheet(
    language: AppLanguage,
    sourcePost: CulturePost? = null,
    editingNote: SavedNote? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        noteText: String,
        sourcePostId: String,
        sourceAuthor: String,
        sourceExcerpt: String,
        imageData: String
    ) -> Unit
) {
    val colors = LocalMeroColors.current
    val s = AppStrings.forLanguage(language)
    val isNe = language == AppLanguage.NEPALI

    var title by remember(sourcePost, editingNote) {
        mutableStateOf(
            editingNote?.title
                ?: sourcePost?.culturalTag?.ifBlank { sourcePost.authorName }.orEmpty()
        )
    }
    var noteText by remember(sourcePost, editingNote) {
        mutableStateOf(
            editingNote?.noteText
                ?: if (sourcePost != null && sourcePost.thoughtText.isBlank()) ""
                else editingNote?.noteText.orEmpty()
        )
    }

    val authorName = editingNote?.sourceAuthorName ?: sourcePost?.authorName.orEmpty()
    val excerpt = editingNote?.sourceExcerpt ?: sourcePost?.thoughtText.orEmpty()
    val postId = editingNote?.sourcePostId ?: sourcePost?.id.orEmpty()
    val img = editingNote?.imageData ?: sourcePost?.imageData.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MeroSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MeroSpacing.lg, vertical = MeroSpacing.sm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MeroSpacing.sm)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    text = if (editingNote != null) {
                        if (isNe) "नोट सम्पादन गर्नुहोस्" else "Edit Saved Note"
                    } else if (sourcePost != null) {
                        s.savePostNote
                    } else {
                        s.addPersonalNote
                    },
                    style = MeroType.Heading.copy(color = colors.text)
                )
            }

            if (excerpt.isNotBlank() || authorName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(MeroRadius.small),
                    color = colors.surface2,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(MeroSpacing.xs),
                        modifier = Modifier.padding(MeroSpacing.md)
                    ) {
                        if (authorName.isNotBlank()) {
                            Text(
                                text = authorName,
                                style = MeroType.Caption.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                            )
                        }
                        if (excerpt.isNotBlank()) {
                            Text(
                                text = excerpt,
                                style = MeroType.Caption.copy(color = colors.textMuted),
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            MeroInput(
                label = if (isNe) "नोट शीर्षक" else "Note Title",
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                placeholder = if (isNe) "जस्तै: तिहारको सम्झना" else "e.g., Favorite Chiya Spot Note",
                testTag = "note_title_input"
            )

            MeroInput(
                label = if (isNe) "तपाईंको व्यक्तिगत नोट" else "Your Personal Note",
                value = noteText,
                onValueChange = { if (it.length <= 1000) noteText = it },
                placeholder = if (isNe) "आफूलाई मन परेको कुरा वा विचार लेख्नुहोस्..." else "Write your thoughts, memories, or cultural reflection...",
                singleLine = false,
                maxLines = 6,
                testTag = "note_body_input"
            )

            MeroButton(
                text = s.saveChanges,
                leadingIcon = Icons.Filled.BookmarkAdd,
                enabled = noteText.trim().isNotEmpty() && !isBusy,
                isLoading = isBusy,
                onClick = {
                    onSave(
                        editingNote?.id,
                        title.trim().ifBlank { if (isNe) "मेरो नोट" else "Cultural Note" },
                        noteText.trim(),
                        postId,
                        authorName,
                        excerpt,
                        img
                    )
                },
                testTag = "note_save_button"
            )
            Spacer(Modifier.height(MeroSpacing.lg))
        }
    }
}
